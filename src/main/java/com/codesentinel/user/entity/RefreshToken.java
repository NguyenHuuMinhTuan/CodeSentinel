package com.codesentinel.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * Token refresh
     */
    @Column(
            nullable = false,
            unique = true,
            length = 500
    )
    private String token;


    /**
     * User sở hữu token
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    /**
     * Thời gian hết hạn
     */
    @Column(nullable = false)
    private Instant expiryDate;


    /**
     * Thời gian tạo
     */
    @Column(nullable = false)
    private Instant createdAt;


    /**
     * Trạng thái revoke
     */
    @Column(nullable = false)
    private boolean revoked = false;
}