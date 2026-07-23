package com.codesentinel.user.repository;

import com.codesentinel.user.entity.Provider;
import com.codesentinel.user.entity.User;
import com.codesentinel.user.entity.UserProvider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProviderRepository
        extends JpaRepository<UserProvider, Long> {

    List<UserProvider> findByUser(User user);

    Optional<UserProvider> findByUserAndProvider(
            User user,
            Provider provider
    );

    Optional<UserProvider> findByProviderAndProviderUserId(
            Provider provider,
            String providerUserId
    );
}