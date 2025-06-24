package com.project.disc_mapper.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.disc_mapper.dto.ClientDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClientService {

    @Autowired
    private ClientController clientController;

    public static class ClientState {
        private static volatile ClientState instance;
        private volatile boolean isBusy = false;

        private ClientState() {
        }

        public static ClientState getInstance() {
            if (instance == null) {
                synchronized (ClientState.class) {
                    if (instance == null) {
                        instance = new ClientState();
                    }
                }
            }
            return instance;
        }

        public synchronized boolean isBusy() {
            return isBusy;
        }

        public synchronized void setBusy(boolean busy) {
            this.isBusy = busy;
        }
    }

    public ClientDTO getClientInfo() {
        ObjectMapper mapper = new ObjectMapper();

        String jsonString = clientController.sendRequestToClient("clientData", null).toString();

        try {
            return mapper.readValue(jsonString, ClientDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse client data", e);
        }
    }

    public List<String> drivesInfo() {
        List<String> getRoots = getAllDrives();

        return getRoots.stream()
                .map(root -> root + " " + getDriveStorage(root))
                .collect(Collectors.toList());
    }

    public List<String> getAllDrives() {
        return (List<String>) clientController.sendRequestToClient("listDrives", null);
    }

    public String getDriveStorage(String driveLetter) {
        return clientController.sendRequestToClient("storage", driveLetter).toString();
    }

    public String getDriveFreeStorage(String driveLetter) {
        return clientController.sendRequestToClient("freeStorage", driveLetter).toString();
    }

    public Map<String, Object> mapDirectoryStructure(String directoryPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() { };
        String jsonString = clientController.sendRequestToMap("mapDirectory", directoryPath).toString();

        return mapper.readValue(jsonString, typeRef);
    }
}