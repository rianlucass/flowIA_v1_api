package com.br.rianlucas.flowia_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.rianlucas.flowia_api.domain.user.User;

public interface UserRepository extends JpaRepository<User, String> {
    User findByEmail(String email);
    User findByUsername(String username);
}
