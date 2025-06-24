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
@Table(name="drives")
public class Drives {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    @Column(nullable = false)
        private String driveName;  /* one user cannot have more than one drive with the same name */
        private String drivePath;
    @Column(columnDefinition = "CLOB")
        private String driveMap;
    @Column(nullable = false)
        private String driveSize;
    @Column(nullable = false)
        private String storageLeft;
    @Column(name = "last_mapped", nullable = false)
        private LocalDateTime lastMapped;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
        private Users user;
    @ManyToOne
    @JoinColumn(name = "pc_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
        private PCs pcs;
    @Column(nullable = false)
        private boolean isExternal = false;

        public Drives() {}

    public boolean getIsExternal() {
        return isExternal;
    }
    public void setIsExternal(boolean external) {
        isExternal = external;
    }
}
