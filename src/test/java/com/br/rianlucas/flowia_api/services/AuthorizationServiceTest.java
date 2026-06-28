package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;
import com.br.rianlucas.flowia_api.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "teste@email.com",
                "123456",
                "teste",
                "Usuário Teste",
                UserRole.USER);
    }

    @Test
    void deveCarregarUsuarioPorEmail() {
        when(userRepository.findByEmail("teste@email.com")).thenReturn(user);

        UserDetails result = authorizationService.loadUserByUsername("teste@email.com");

        assertNotNull(result);
        assertEquals("teste@email.com", ((User) result).getEmail());
        assertEquals("teste", result.getUsername());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(userRepository.findByEmail("naoexiste@email.com")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> authorizationService.loadUserByUsername("naoexiste@email.com"));
    }
}
