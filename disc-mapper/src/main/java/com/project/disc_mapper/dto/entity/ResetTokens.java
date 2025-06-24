package com.project.disc_mapper.dto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@Table(name="reset_tokens")
public class ResetTokens {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    @Column(nullable = false)
        private String token;
    @Column(name = "created_at",nullable = false)
        private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
        private Users user;

    
    private String username;

        public ResetTokens() {}
}
