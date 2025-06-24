package com.project.disc_mapper.dto;

import com.project.disc_mapper.config.SearchType;
import jakarta.persistence.Transient;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class SearchRequestDTO {

    private Long userId;
    private String searchTerm;
    private SearchType searchType;
    private String pcModel;
    private Boolean isExternal;
    private Long currentDriveId;
    private Boolean isDirectory;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @Transient
    private boolean currentDrive;

    public SearchRequestDTO() {}
}
