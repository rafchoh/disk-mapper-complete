package com.project.disc_mapper.service;

import com.project.disc_mapper.dto.DevicesDTO;
import com.project.disc_mapper.dto.entity.PCs;
import com.project.disc_mapper.repo.PCRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PCService {

    @Autowired
    private PCRepo pcRepo;

    @Autowired
    private UserService userService;

    public PCs getPCById(Long id) {

        return pcRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PC not found with id: " + id));
    }

    public List<PCs> getAllDevicesByUserId(Long userId) {
        return pcRepo.findAllPcByUserId(userId);
    }

    public boolean pcExistsForCurrentUser(String pcName,
                                          String pcModel) {

        Long currentUsernameId = userService.getCurrentUserIdByUsername(userService.getAuthUsername());
        return pcRepo.existsByPcNameAndPcModelAndUserId(pcName, pcModel, currentUsernameId);
    }

    public boolean pcNotExistForCurrentUser(String pcName,
                                            String pcModel) {

        Long currentUsernameId = userService.getCurrentUserIdByUsername(userService.getAuthUsername());
        return pcRepo.existsByPcNameAndPcModelAndOtherUserId(pcName, pcModel, currentUsernameId);
    }

    public Map<Long, String> myPCs(String username) {

        return pcRepo.findAllPcByUserId(userService.getCurrentUserIdByUsername(username)).stream()
                .collect(Collectors.toMap(
                        PCs::getId,
                        pc -> pc.getPcInfo() + " / " + pc.getPcName()
                ));
    }


    public DevicesDTO convertPCToDeviceDTO(PCs pc) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return new DevicesDTO(
                pc.getId(),
                pc.getPcName(),
                pc.getPcModel(),
                pc.getPcInfo(),
                pc.getPcAdded().toLocalDate() + "",
                false,
                true
        );
    }
}
