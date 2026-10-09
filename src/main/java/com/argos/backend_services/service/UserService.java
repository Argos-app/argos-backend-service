package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateUserIn;
import com.argos.backend_services.dto.in.UpdateUserAccessIn;
import com.argos.backend_services.dto.in.UpdateUserStatusIn;
import com.argos.backend_services.dto.out.CreateUserOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserAccessOut;
import com.argos.backend_services.dto.out.UserCreationOptionsOut;
import com.argos.backend_services.dto.out.UserOptionOut;
import com.argos.backend_services.dto.out.AdminUserOptionOut;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.repository.PermissionRepository;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserPropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.utils.PageableUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserPropertyRepository userPropertyRepository;
    private final PermissionRepository permissionRepository;
    private final PropertyRepository propertyRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Transactional(Transactional.TxType.SUPPORTS)
    public UserCreationOptionsOut getCreationOptions() {
        List<UserOptionOut> properties = propertyRepository.findByActiveOrderByNameAsc(true).stream()
                .map(property -> new UserOptionOut(property.getId(), property.getName()))
                .toList();
        List<UserOptionOut> permissions = permissionRepository.findByActiveOrderByNameAsc(true).stream()
                .map(permission -> new UserOptionOut(permission.getId(), permission.getName()))
                .toList();
        return new UserCreationOptionsOut(properties, permissions);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<UserOptionOut> getActivePermissions() {
        return permissionRepository.findByActiveOrderByNameAsc(true).stream()
                .map(permission -> new UserOptionOut(permission.getId(), permission.getName()))
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PageOut<UserAccessOut> listUserAccesses(int page, int size) {
        Page<UserAccessOut> result = userRepository.findAllUserAccesses(
                PageableUtils.createPageRequest(page, size, Sort.unsorted()));
        return new PageOut<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional
    public void updateAccess(UUID userId, UpdateUserAccessIn data) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        Permission permission = permissionRepository.findById(data.permissionId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new NotFoundException("Permissão não encontrada ou inativa"));
        user.setPermission(permission);
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void updateStatus(UUID userId, UpdateUserStatusIn data) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        user.setActive(data.active());
        userRepository.saveAndFlush(user);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PageOut<AdminUserOptionOut> listAdministrators(int page, int size) {
        Page<AdminUserOptionOut> result = userRepository
                .findByActiveTrueAndPermission_ActiveTrueAndPermission_CodeOrderByNameAsc(
                        "ADMIN", PageableUtils.createPageRequest(page, size, Sort.unsorted()))
                .map(AdminUserOptionOut::from);
        return new PageOut<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional
    public void delete(String userId) {
        AppUser user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        UserProperty userProperty = user.getProperties().stream()
                .filter(UserProperty::getActive)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Usuário não está vinculado a nenhuma propriedade"));

        user.setActive(false);
        userProperty.setActive(false);
    }

    @Transactional
    public CreateUserOut create(CreateUserIn data) {
        String email = data.email().trim().toLowerCase(Locale.ROOT);
        String cpf = data.cpf().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new BadRequestException("CPF já cadastrado");
        }

        Permission permission = permissionRepository.findById(data.permissionId())
                .filter(Permission::getActive)
                .orElseThrow(() -> new NotFoundException("Permissão não encontrada ou inativa"));
        Property property = propertyRepository.findById(data.propertyId())
                .filter(Property::getActive)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada ou inativa"));

        String firebaseUid;
        try {
            UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(
                    new UserRecord.CreateRequest()
                            .setEmail(email)
                            .setPassword(data.password())
                            .setDisplayName(data.name().trim()));
            firebaseUid = firebaseUser.getUid();
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new BadRequestException("Email já cadastrado no Firebase");
            }
            throw new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "FIREBASE_USER_CREATION_FAILED", "Não foi possível criar a conta do usuário");
        }

        try {
            AppUser user = new AppUser();
            user.setPermission(permission);
            user.setName(data.name().trim());
            user.setCpf(cpf);
            user.setEmail(email);
            user.setFirebaseUid(firebaseUid);
            user.setActive(true);
            user.setFirstLogin(true);
            AppUser savedUser = userRepository.saveAndFlush(user);

            userPropertyRepository.saveAndFlush(UserProperty.builder()
                    .user(savedUser)
                    .property(property)
                    .linkDate(LocalDate.now())
                    .active(true)
                    .build());
            return new CreateUserOut(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
        } catch (RuntimeException exception) {
            try {
                FirebaseAuth.getInstance().deleteUser(firebaseUid);
            } catch (FirebaseAuthException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public void update(UUID userId, UpdateUserIn data) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        String previousEmail = user.getEmail();
        String email = data.email().trim().toLowerCase(Locale.ROOT);
        String name = data.name().trim();
        String cpf = data.cpf().trim();

        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new BadRequestException("Email já cadastrado");
        }
        if (userRepository.existsByCpfAndIdNot(cpf, userId)) {
            throw new BadRequestException("CPF já cadastrado");
        }

        Permission permission = permissionRepository.findById(data.permissionId())
                .filter(Permission::getActive)
                .orElseThrow(() -> new NotFoundException("Permissão não encontrada ou inativa"));
        Property targetProperty = propertyRepository.findById(data.propertyId())
                .filter(Property::getActive)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada ou inativa"));
        UserProperty currentLink = user.getProperties().stream()
                .filter(UserProperty::getActive)
                .filter(link -> link.getProperty().getId().equals(data.currentPropertyId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Vínculo do usuário não encontrado"));

        user.setName(name);
        user.setEmail(email);
        user.setCpf(cpf);
        user.setPermission(permission);
        userRepository.saveAndFlush(user);

        if (!Objects.equals(currentLink.getProperty().getId(), targetProperty.getId())) {
            currentLink.setActive(false);
            currentLink.setUnlinkDate(LocalDate.now());
            userPropertyRepository.save(currentLink);

            UserProperty existingTargetLink = user.getProperties().stream()
                    .filter(link -> link.getProperty().getId().equals(targetProperty.getId()))
                    .findFirst()
                    .orElse(null);
            if (existingTargetLink == null) {
                existingTargetLink = UserProperty.builder()
                        .user(user)
                        .property(targetProperty)
                        .linkDate(LocalDate.now())
                        .active(true)
                        .build();
            } else {
                existingTargetLink.setActive(true);
                existingTargetLink.setLinkDate(LocalDate.now());
                existingTargetLink.setUnlinkDate(null);
            }
            userPropertyRepository.saveAndFlush(existingTargetLink);
        }

        synchronizeFirebaseProfile(user, name, email, previousEmail);
    }

    private void synchronizeFirebaseProfile(AppUser user, String name, String email, String previousEmail) {
        try {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            UserRecord firebaseUser = user.getFirebaseUid() == null
                    ? firebaseAuth.getUserByEmail(previousEmail)
                    : firebaseAuth.getUser(user.getFirebaseUid());
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(firebaseUser.getUid())
                    .setDisplayName(name)
                    .setEmail(email));
            user.setFirebaseUid(firebaseUser.getUid());
        } catch (FirebaseAuthException exception) {
            LOGGER.error("Erro ao atualizar perfil do usuário no Firebase: {}", exception.getMessage(), exception);
            if (exception.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new BadRequestException("Email já cadastrado no Firebase");
            }
            throw new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "FIREBASE_USER_UPDATE_FAILED", "Não foi possível atualizar a conta do usuário");

        }
    }

    public PageOut<UserListItemOut> listUsers(int page, int size) {
        Page<UserListItemOut> result = userRepository.findAllLinked(
                PageableUtils.createPageRequest(page, size, Sort.unsorted()));

        return new PageOut<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }
}
