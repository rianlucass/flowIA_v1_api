package com.br.rianlucas.flowia_api.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthController authController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("teste@email.com", "senha123", "testeuser", "Teste Usuario", UserRole.USER);
        user.setId("user-1");
    }

    @Test
    void deveRealizarLoginComSucesso() {
        LoginRequestDTO loginDTO = new LoginRequestDTO("teste@email.com", "senha123");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenService.generateToken(user)).thenReturn("jwt-token-mockado");

        ResponseEntity<AuthResponseDTO> response = authController.login(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token-mockado", response.getBody().token());
        assertEquals("user-1", response.getBody().userId());
        assertEquals("Teste Usuario", response.getBody().name());
        assertEquals("teste@email.com", response.getBody().email());
        assertEquals(UserRole.USER, response.getBody().role());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken(user);
    }

    @Test
    void deveRegistrarComSucesso() {
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("novouser", "Novo Usuario", "novo@email.com", "senha123");
        when(userRepository.findByEmail("novo@email.com")).thenReturn(null);
        when(userRepository.findByUsername("novouser")).thenReturn(null);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-senha");

        ResponseEntity<UserResponseDTO> response = authController.register(registerDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("novouser", response.getBody().username());
        assertEquals("Novo Usuario", response.getBody().name());
        assertEquals("novo@email.com", response.getBody().email());
        assertEquals(UserRole.USER, response.getBody().role());

        verify(userRepository).findByEmail("novo@email.com");
        verify(userRepository).findByUsername("novouser");
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaExiste() {
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("novouser", "Novo Usuario", "existe@email.com", "senha123");
        when(userRepository.findByEmail("existe@email.com")).thenReturn(user);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authController.register(registerDTO));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deveLancarExcecaoQuandoUsernameJaExiste() {
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("testeuser", "Novo Usuario", "novo@email.com", "senha123");
        when(userRepository.findByEmail("novo@email.com")).thenReturn(null);
        when(userRepository.findByUsername("testeuser")).thenReturn(user);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> authController.register(registerDTO));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deveCriarUsuarioComRolePadrao() {
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("novouser", "Novo Usuario", "novo@email.com", "senha123");
        when(userRepository.findByEmail("novo@email.com")).thenReturn(null);
        when(userRepository.findByUsername("novouser")).thenReturn(null);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-senha");

        ResponseEntity<UserResponseDTO> response = authController.register(registerDTO);

        assertEquals(UserRole.USER, response.getBody().role());
    }
}
