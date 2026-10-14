package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateUserIn;
import com.argos.backend_services.dto.in.UpdateCurrentUserIn;
import com.argos.backend_services.dto.in.UpdateUserAccessIn;
import com.argos.backend_services.dto.in.UpdateUserStatusIn;
import com.argos.backend_services.dto.out.CreateUserOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserPropertyOut;
import com.argos.backend_services.dto.out.UserProfileOut;
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
import com.argos.backend_services.exception.UnauthorizedException;
import com.argos.backend_services.repository.PermissionRepository;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserPropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.utils.PageableUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserPropertyRepository userPropertyRepository;
    private final PermissionRepository permissionRepository;
    private final PropertyRepository propertyRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Transactional(readOnly = true)
    public UserProfileOut getMyProfile(UUID userId) {
        AppUser user = userRepository.findActiveProfileById(userId)
                .orElseThrow(() -> new UnauthorizedException("Sessão não autorizada"));
        Permission permission = user.getPermission();
        return new UserProfileOut(user.getId(), user.getName(), user.getEmail(), user.getCpf(),
                user.getPhotoUrl(), permission.getName(), permission.getCode(),
                Boolean.TRUE.equals(user.getActive()), userPropertyRepository.findActiveProfileProperties(userId));
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public UserCreationOptionsOut getCreationOptions(UUID administratorId) {
        List<UserOptionOut> properties = propertyRepository.findByUser_IdAndActiveTrueOrderByNameAsc(administratorId).stream()
                .map(property -> new UserOptionOut(property.getId(), property.getName()))
                .toList();
        List<UserOptionOut> permissions = permissionRepository.findByActiveOrderByNameAsc(true).stream()
                .map(permission -> new UserOptionOut(permission.getId(), permission.getName()))
                .toList();
        return new UserCreationOptionsOut(properties, permissions);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<UserOptionOut> getActivePermissions() {
        return permissionRepository.findByActiveOrderByNameAsc(true).stream()
                .map(permission -> new UserOptionOut(permission.getId(), permission.getName()))
                .toList();
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public PageOut<UserAccessOut> listUserAccesses(int page, int size, UUID administratorId) {
        PageableUtils.validatePageAndSize(page, size);

        Page<UserAccessOut> result = userRepository.findUserAccessesByAdministrator(administratorId, PageRequest.of(page, size));
        return new PageOut<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional
    public void updateAccess(UUID userId, UpdateUserAccessIn data, UUID administratorId) {
        AppUser user = findExclusiveUser(userId, administratorId);
        Permission permission = permissionRepository.findById(data.permissionId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new NotFoundException("Permissão não encontrada ou inativa"));
        user.setPermission(permission);
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void updateStatus(UUID userId, UpdateUserStatusIn data, UUID administratorId) {
        AppUser user = findExclusiveUser(userId, administratorId);
        user.setActive(data.active());
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void updateCurrentUser(UUID userId, UpdateCurrentUserIn data) {
        if (data.isEmpty()) {
            throw new BadRequestException("Informe ao menos um campo para atualização");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        String previousEmail = user.getEmail();
        String name = data.name() == null ? user.getName() : data.name().trim();
        String email = data.email() == null ? user.getEmail() : data.email().trim().toLowerCase(Locale.ROOT);
        String photoUrl = data.photoUrl() == null ? user.getPhotoUrl() : data.photoUrl().trim();

        if (name.isBlank() || email.isBlank()) {
            throw new BadRequestException("Nome e email não podem ficar vazios");
        }
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new BadRequestException("Email já cadastrado");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPhotoUrl(photoUrl);
        userRepository.save(user);
        synchronizeFirebaseProfile(user, name, email, previousEmail, photoUrl);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public PageOut<AdminUserOptionOut> listAdministrators(int page, int size, UUID administratorId) {
        PageableUtils.validatePageAndSize(page, size);
        Page<AdminUserOptionOut> result = userRepository
                .findByIdAndActiveTrueAndPermission_ActiveTrueAndPermission_Code(
                        administratorId, "ADMIN", PageRequest.of(page, size))
                .map(AdminUserOptionOut::from);
        return new PageOut<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional
    public void delete(String userId, UUID administratorId) {
        AppUser user = userRepository.findManagedUserById(UUID.fromString(userId), administratorId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        user.getProperties().stream()
                .filter(link -> Boolean.TRUE.equals(link.getActive()))
                .filter(link -> Boolean.TRUE.equals(link.getProperty().getActive()))
                .filter(link -> link.getProperty().getUser().getId().equals(administratorId))
                .forEach(link -> {
                    link.setActive(false);
                    link.setUnlinkDate(LocalDate.now());
                });
    }

    @Transactional
    public CreateUserOut create(CreateUserIn data, UUID administratorId) {
        Property property = findManagedProperty(data.propertyId(), administratorId);
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
    public void update(UUID userId, UpdateUserIn data, UUID administratorId) {
        findManagedProperty(data.currentPropertyId(), administratorId);
        Property targetProperty = findManagedProperty(data.propertyId(), administratorId);
        AppUser user = findExclusiveUser(userId, administratorId);
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

        synchronizeFirebaseProfile(user, name, email, previousEmail, user.getPhotoUrl());
    }

    private void synchronizeFirebaseProfile(AppUser user, String name, String email, String previousEmail,
                                            String photoUrl) {
        try {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            UserRecord firebaseUser = user.getFirebaseUid() == null
                    ? firebaseAuth.getUserByEmail(previousEmail)
                    : firebaseAuth.getUser(user.getFirebaseUid());
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(firebaseUser.getUid())
                    .setDisplayName(name)
                    .setEmail(email)
                    .setPhotoUrl(photoUrl));
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

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public PageOut<UserListItemOut> listUsers(int page, int size, UUID administratorId) {
       PageableUtils.validatePageAndSize(page, size);
        Page<UserListItemOut> result = userRepository.findLinkedByAdministrator(administratorId, PageRequest.of(page, size));

        Map<UUID, List<UserPropertyOut>> propertiesByUser = result.isEmpty() ? Map.of()
                : userPropertyRepository.findActiveLinksByUsersAndAdministrator(
                        result.getContent().stream().map(UserListItemOut::userId).toList(), administratorId)
                .stream().collect(Collectors.groupingBy(link -> link.getUser().getId(),
                        Collectors.mapping(link -> new UserPropertyOut(link.getProperty().getId(),
                                link.getProperty().getName(),
                                link.getProperty().getCnpj() == null ? null : link.getProperty().getCnpj().trim(),
                                link.getLinkDate()), Collectors.toList())));
        List<UserListItemOut> items = result.getContent().stream()
                .map(user -> new UserListItemOut(user.userId(), user.photoUrl(), user.name(), user.cpf(),
                        user.email(), user.active(), user.permissionId(),
                        propertiesByUser.getOrDefault(user.userId(), List.of())))
                .toList();

        return new PageOut<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    private Property findManagedProperty(UUID propertyId, UUID administratorId) {
        return propertyRepository.findByIdAndUser_Id(propertyId, administratorId)
                .filter(property -> Boolean.TRUE.equals(property.getActive()))
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada ou inativa"));
    }

    private AppUser findExclusiveUser(UUID userId, UUID administratorId) {
        AppUser user = userRepository.findManagedUserById(userId, administratorId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        boolean shared = user.getProperties().stream()
                .anyMatch(link -> !link.getProperty().getUser().getId().equals(administratorId));
        if (shared || (!userId.equals(administratorId) && propertyRepository.existsByUser_Id(userId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A conta possui vínculos fora da sua responsabilidade e não pode ser alterada");
        }
        return user;
    }
}
