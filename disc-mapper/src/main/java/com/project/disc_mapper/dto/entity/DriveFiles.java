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
@Table(name = "drive_files")
public class DriveFiles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    @Column(nullable = false)
        private String fileName;
    @Column(columnDefinition = "CLOB", nullable = false)
        private String filePath;
    @Column(columnDefinition = "CLOB", nullable = false)
        private String parentPath;
    @Column(nullable = false)
        private boolean isDirectory;
        private LocalDateTime lastModified;

    @ManyToOne
    @JoinColumn(name = "drive_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
        private Drives drive;

    public DriveFiles() {}
}
