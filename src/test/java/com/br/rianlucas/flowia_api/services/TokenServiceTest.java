package com.br.rianlucas.flowia_api.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.br.rianlucas.flowia_api.domain.user.User;
import com.br.rianlucas.flowia_api.domain.user.UserRole;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setup() {
        tokenService = new TokenService();

        ReflectionTestUtils.setField(
                tokenService,
                "secret",
                "minha-chave-secreta-para-testes");
    }

    @Test
    void deveGerarToken() {

        User user = new User(
                "teste@email.com",
                "123456",
                "teste",
                "Usuário Teste",
                UserRole.USER);

        String token = tokenService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void deveValidarTokenGerado() {

        User user = new User(
                "teste@email.com",
                "123456",
                "teste",
                "Usuário Teste",
                UserRole.USER);

        String token = tokenService.generateToken(user);

        String email = tokenService.validateToken(token);

        assertEquals("teste@email.com", email);
    }

    @Test
    void deveRetornarVazioParaTokenInvalido() {

        String resultado = tokenService.validateToken("token-invalido");

        assertEquals("", resultado);
    }
}