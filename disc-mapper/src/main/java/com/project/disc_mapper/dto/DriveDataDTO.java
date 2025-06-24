package com.project.disc_mapper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DriveDataDTO {
    private String driveRoot;
    private String totalStorage;
    private String availableStorage;
}
