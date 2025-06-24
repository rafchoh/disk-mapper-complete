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
@Table(name="pcs")
public class PCs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String pcName; /* one user cannot have more than one pc with the same name for the same pc model */
        private String pcModel;
        private String pcInfo;
    @Column(name = "pc_added", nullable = false)
        private LocalDateTime pcAdded;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
        private Users user;

    public PCs() { }
}

