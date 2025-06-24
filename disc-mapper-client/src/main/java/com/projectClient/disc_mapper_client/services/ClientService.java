package com.projectClient.disc_mapper_client.services;

import com.projectClient.disc_mapper_client.functionality.DeviceFuncs;
import com.projectClient.disc_mapper_client.functionality.DriveFuncs;
import com.projectClient.disc_mapper_client.objects.ClientDTO;
import com.projectClient.disc_mapper_client.objects.DriveDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientService {

    @Autowired
    private DriveFuncs driveAPI;

    @Autowired
    private DeviceFuncs deviceAPI;


    public ClientDTO clientInfo() {
        ClientDTO client = new ClientDTO();

        List<DriveDataDTO> ddd = driveAPI.getDrivesData();

        client.setPcName(deviceAPI.pullPcName());
        client.setPcModel(deviceAPI.pullPcModel());
        client.setDriveData(driveAPI.getDrivesData());

        return client;
    }
}
