package com.codesentinel.auth.infrastructure;

import com.codesentinel.auth.domain.Provider;
import com.codesentinel.auth.domain.User;
import com.codesentinel.auth.domain.UserProvider;

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
