package com.project.disc_mapper.config;

import com.project.disc_mapper.api.ClientService;
import com.project.disc_mapper.dto.ClientDTO;
import com.project.disc_mapper.dto.SearchResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServerCache {

    @Autowired
    private ClientService cs;


    private final Map<Long, SearchResponseDTO> cache = new ConcurrentHashMap<>();

    public void saveSearch(Long userId, SearchResponseDTO response) {
        cache.put(userId, response);
    }
    public SearchResponseDTO getSearch(Long userId) {
        return cache.get(userId);
    }
    public void clearSearch(Long userId) {
        cache.remove(userId);
    }





    private final Map<Long, ClientDTO> clientData = new ConcurrentHashMap<>();

    public void saveClientData(Long userId) {
        clientData.put(userId, cs.getClientInfo());
    }
    public ClientDTO getClientData(Long userId) {
        return clientData.get(userId);
    }
    public void clearClientData(Long userId) {
        clientData.remove(userId);
    }
}
