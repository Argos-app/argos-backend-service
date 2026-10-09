package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.PropertyIn;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.PropertyOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.entity.UserPropertyId;
import com.argos.backend_services.entity.enums.Permission;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserPropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UserPropertyRepository userPropertyRepository;

    @Transactional(Transactional.TxType.SUPPORTS)
    public PageOut<PropertyOut> listActive(int page, int size, UUID administratorId) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Parâmetros de paginação inválidos");
        }

        Page<PropertyOut> result = propertyRepository
                .findByUser_IdAndActiveTrue(administratorId,
                        PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id"))))
                .map(PropertyOut::from);

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

    public PropertyOut create(PropertyIn input, UUID administratorId) {
        validateResponsible(input.responsibleAdminId(), administratorId);
        AppUser responsible = findActiveAdministrator(administratorId);
        Property property = Property.builder()
                .user(responsible)
                .name(input.name().trim())
                .cnpj(input.cnpj())
                .address(input.address().trim())
                .city(input.city().trim())
                .state(input.state().trim().toUpperCase())
                .areaHectares(input.areaHectares())
                .phone(input.phone())
                .active(true)
                .build();

        Property saved = propertyRepository.saveAndFlush(property);
        activateLink(responsible, saved);
        return PropertyOut.from(saved);
    }

    public void update(UUID propertyId, PropertyIn input, UUID administratorId) {
        Property property = findProperty(propertyId, administratorId);
        validateResponsible(input.responsibleAdminId(), administratorId);
        AppUser responsible = findActiveAdministrator(administratorId);

        property.setName(input.name().trim());
        property.setCnpj(input.cnpj() == null ? null : input.cnpj().trim());
        property.setAddress(input.address().trim());
        property.setCity(input.city().trim());
        property.setState(input.state().trim().toUpperCase());
        property.setAreaHectares(input.areaHectares());
        property.setPhone(input.phone());

        activateLink(responsible, property);
    }

    public void deactivate(UUID propertyId, UUID administratorId) {
        Property property = findProperty(propertyId, administratorId);
        if (!property.getActive()) return;

        property.setActive(false);
        LocalDate unlinkDate = LocalDate.now();
        List<UserProperty> activeLinks = userPropertyRepository.findAllByProperty_IdAndActiveTrue(propertyId);
        activeLinks.forEach(link -> {
            link.setActive(false);
            link.setUnlinkDate(unlinkDate);
        });
    }

    private AppUser findActiveAdministrator(UUID adminId) {
        AppUser user = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Administrador responsável não encontrado"));
        if (!Boolean.TRUE.equals(user.getActive())
                || !Boolean.TRUE.equals(user.getPermission().getActive())
                || !Permission.ADMIN.getPermission().equals(user.getPermission().getCode())) {
            throw new BadRequestException("O responsável deve ser um administrador ativo");
        }
        return user;
    }

    private Property findProperty(UUID propertyId, UUID administratorId) {
        return propertyRepository.findByIdAndUser_Id(propertyId, administratorId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada"));
    }

    private void activateLink(AppUser user, Property property) {
        UserPropertyId id = new UserPropertyId(user.getId(), property.getId());
        UserProperty link = userPropertyRepository.findById(id).orElseGet(() -> UserProperty.builder()
                .user(user)
                .property(property)
                .linkDate(LocalDate.now())
                .active(true)
                .build());
        if (!Boolean.TRUE.equals(link.getActive())) {
            link.setActive(true);
            link.setLinkDate(LocalDate.now());
            link.setUnlinkDate(null);
        }
        userPropertyRepository.save(link);
    }

    private void validateResponsible(UUID responsibleId, UUID administratorId) {
        if (!administratorId.equals(responsibleId)) {
            throw new NotFoundException("Administrador responsável não encontrado");
        }
    }
}
