package com.project.disc_mapper.dto;

import com.project.disc_mapper.dto.entity.DriveFiles;
import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class SearchResponseDTO {

    private Long userId;

    private String searchTerm;
    private String message;
    private String searchType;

    private boolean success;
    private boolean isFile;

    private List<DevicesDTO> devices;
    private List<DriveFiles> fileResults;

    private int totalResults;
}
