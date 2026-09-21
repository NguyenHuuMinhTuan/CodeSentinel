package com.codesentinel.auth.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    private String avatarUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Không đặt nullable=false: cột mới thêm vào bảng users đã có dữ liệu,
     * ràng buộc NOT NULL sẽ làm ddl-auto=update fail nếu chưa backfill dữ liệu cũ.
     * Giá trị mặc định cho user mới do @Builder.Default đảm nhiệm; user cũ (role=null)
     * được JwtAuthenticationFilter coi như USER.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
