package com.codesentinel.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_providers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private Provider provider;

    private String providerUserId;

    @Column(name = "password_hash")
    private String passwordHash;

    private String avatarUrl;

    private Boolean active;
}