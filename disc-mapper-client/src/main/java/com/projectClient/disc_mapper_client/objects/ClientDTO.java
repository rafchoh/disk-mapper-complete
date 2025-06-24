package com.projectClient.disc_mapper_client.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClientDTO {
    private String pcName;
    private String pcModel;
    private List<DriveDataDTO> driveData;
}
