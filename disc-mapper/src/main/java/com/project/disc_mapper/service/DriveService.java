package com.project.disc_mapper.service;

import com.project.disc_mapper.dto.DevicesDTO;
import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.repo.DriveRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DriveService {

    @Autowired
    private DriveRepo driveRepo;
    @Autowired
    private UserService userService;


    public Drives getDriveById(Long id) {

        return driveRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Drive not found with id: " + id));
    }

    public List<Drives> getAllDrivesWhereExternalByUserId(boolean isExternal,
                                                          Long userId) {

        return driveRepo.findAllDrivesWhereExternalByUserId(isExternal, userId);
    }

    public List<Drives> getDrivesByPcId(Long pcId) {

        return driveRepo.findAllDrivesByPcId(false, pcId);
    }

    public String getJsonFromDbForDrive(Long driveId) {

        return driveRepo.getMapFromDbForDrive(driveId);
    }

    public boolean driveNameExistForCurrentUser(String pcName) {

        Long currentUsernameId = userService.getCurrentUserIdByUsername(userService.getAuthUsername());
        return driveRepo.existsByDriveNameForUser(pcName, currentUsernameId);
    }

    public boolean driveNameExistForOtherUsers(String pcName) {

        Long currentUsernameId = userService.getCurrentUserIdByUsername(userService.getAuthUsername());
        return driveRepo.existsByDriveNameForOtherUsers(pcName, currentUsernameId);
    }


    public DevicesDTO convertDriveToDeviceDTO(Drives drive) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return new DevicesDTO(
                drive.getId(),
                drive.getDriveName(),
                drive.getDrivePath(),
                drive.getStorageLeft() + " left of " + drive.getDriveSize(),
                drive.getLastMapped().toLocalDate() + "\n" + drive.getLastMapped().toLocalTime().format(timeFormatter),
                drive.getIsExternal(),
                false
        );
    }
}