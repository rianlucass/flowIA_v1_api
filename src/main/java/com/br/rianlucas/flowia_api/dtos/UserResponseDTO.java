package com.br.rianlucas.flowia_api.dtos;

import com.br.rianlucas.flowia_api.domain.user.UserRole;

public record UserResponseDTO(
        String id,
        String username,
        String name,
        String email,
        UserRole role
) {}
