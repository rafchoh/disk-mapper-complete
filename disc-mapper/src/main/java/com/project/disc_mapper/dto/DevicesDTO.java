package com.project.disc_mapper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DevicesDTO {

    private Long devId;
    private String name;
    private String forDevice;
    private String info;
    private String devAdded;
    private boolean isExternal;
    private boolean isPC;
}
