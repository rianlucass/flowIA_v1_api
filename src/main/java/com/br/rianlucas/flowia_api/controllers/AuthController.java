package com.br.rianlucas.flowia_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.dtos.user.AuthResponseDTO;
import com.br.rianlucas.flowia_api.dtos.user.LoginRequestDTO;
import com.br.rianlucas.flowia_api.dtos.user.RegisterRequestDTO;
import com.br.rianlucas.flowia_api.dtos.user.UserResponseDTO;
import com.br.rianlucas.flowia_api.infra.exceptions.EmailAlreadyExistsException;
import com.br.rianlucas.flowia_api.infra.exceptions.UsernameAlreadyExistsException;
import com.br.rianlucas.flowia_api.repositories.UserRepository;
import com.br.rianlucas.flowia_api.services.TokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var user = (User) auth.getPrincipal();
        var token = tokenService.generateToken(user);
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid RegisterRequestDTO data) {
        if (userRepository.findByEmail(data.email()) != null) {
            throw new EmailAlreadyExistsException(data.email());
        }
        if (userRepository.findByUsername(data.username()) != null) {
            throw new UsernameAlreadyExistsException(data.username());
        }

        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = new User(data.email(), encryptedPassword, data.username(), data.name(), UserRole.USER);
        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new UserResponseDTO(newUser.getId(), newUser.getUsername(), newUser.getName(), newUser.getEmail(), newUser.getRole())
        );
    }
}
