package com.project.disc_mapper.service;

import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.repo.DrFilesRepo;
import com.project.disc_mapper.repo.DriveRepo;
import com.project.disc_mapper.repo.PCRepo;
import com.project.disc_mapper.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private DriveRepo driveRepo;
    @Autowired
    private PCRepo pcRepo;
    @Autowired
    private DrFilesRepo dfRepo;


    public Users findByUsername(String username) {

        return userRepo.findByUsername(username).orElse(null);
    }

    public String getAuthUsername() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public Long getCurrentUserIdByUsername(String username) {

        return userRepo.findByUsername(username)
                .map(Users::getId)
                .orElse(null);
    }

    public boolean isUserLoggedIn() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !getAuthUsername().equals("anonymousUser") && authentication.isAuthenticated();
    }

    public boolean isEmailPresent(String email) {

        return userRepo.existsByEmail(email);
    }

    public boolean emailExistForOtherUsers(String email) {

        Long currentUsernameId = getCurrentUserIdByUsername(getAuthUsername());
        return userRepo.existsByEmailForOtherUsers(email, currentUsernameId);
    }


    public boolean checkPcProperty(Long pcId) {

        Long currentUserId = getCurrentUserIdByUsername(getAuthUsername());
        return pcRepo.findAllPcByUserId(currentUserId, pcId);
    }

    public boolean checkDriveProperty(Long driveId) {

        Long currentUserId = getCurrentUserIdByUsername(getAuthUsername());
        return driveRepo.findAllDrivesForUserId(currentUserId, driveId);
    }

    public boolean checkFilesProperty(Long fileId) {

        Long currentUserId = getCurrentUserIdByUsername(getAuthUsername());
        return dfRepo.findAllFilesForUserId(currentUserId, fileId);
    }
}
