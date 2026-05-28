package com.br.rianlucas.flowia_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.dtos.user.UserResponseDTO;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                new UserResponseDTO(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getRole())
        );
    }
}
