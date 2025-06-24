package com.project.disc_mapper.dto.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@Table(name="users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    @Column(nullable = false, unique = true)
        private String username;
    @Column(unique = true)
        private String email;
    @Column(nullable = false)
        private String password;
    @Column(nullable = false)
        private String fullName;
    @Column(nullable = false)
        private boolean recoveryMode = false;

    @Transient
        private String reTypePassword;
    @Transient
        private String newPassword;

    public Users() {

    }
}
