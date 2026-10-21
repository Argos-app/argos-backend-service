package com.argos.backend_services.dto.out;

import java.util.List;

public record UserCreationOptionsOut(List<UserOptionOut> properties, List<UserOptionOut> permissions) {}
