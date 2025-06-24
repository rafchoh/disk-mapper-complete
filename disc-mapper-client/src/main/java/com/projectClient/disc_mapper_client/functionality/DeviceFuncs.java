package com.projectClient.disc_mapper_client.functionality;

import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.ComputerSystem;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class DeviceFuncs {

    public String pullPcName() {

        String out = "";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String pcName = localHost.getHostName();

            out = pcName;
        } catch (UnknownHostException e) {
            out = "Unable to determine PC name: " + e.getMessage();
        }

        return out;
    }

    public String pullPcModel() {

        SystemInfo systemInfo = new SystemInfo();
        ComputerSystem computerSystem = systemInfo.getHardware().getComputerSystem();
        String manufacturer = computerSystem.getManufacturer();
        String pcModel = computerSystem.getModel();

        return manufacturer + " " + pcModel;
    }
}