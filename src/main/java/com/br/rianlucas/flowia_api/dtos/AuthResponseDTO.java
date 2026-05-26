package com.br.rianlucas.flowia_api.dtos;

import com.br.rianlucas.flowia_api.domain.user.UserRole;

public record AuthResponseDTO(
        String token,
        String name,
        String email,
        UserRole role
) {}
