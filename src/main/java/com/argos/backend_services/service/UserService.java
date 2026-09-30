package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    public static final int PAGE_SIZE = 6;

    private final UserRepository userRepository;

    public PageOut<UserListItemOut> listUsers(int page) {
        if (page < 0) {
            throw new BadRequestException("Página não pode ser negativa");
        }

        Page<UserListItemOut> result = userRepository.findAllLinked(PageRequest.of(page, PAGE_SIZE));

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
