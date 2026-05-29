package com.br.rianlucas.flowia_api.dtos.user;

import com.br.rianlucas.flowia_api.domain.user.UserRole;

public record AuthResponseDTO(
        String token,
        String userId,
        String name,
        String email,
        UserRole role
) {}
