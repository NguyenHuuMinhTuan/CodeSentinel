package com.codesentinel.user.repository;

import com.codesentinel.user.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderRepository
        extends JpaRepository<Provider, Long> {

    Optional<Provider> findByCode(String code);

    boolean existsByCode(String code);
}