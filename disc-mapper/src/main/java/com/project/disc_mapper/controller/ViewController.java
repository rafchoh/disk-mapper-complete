package com.project.disc_mapper.controller;

import com.project.disc_mapper.api.ClientService;
import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.dto.DevicesDTO;
import com.project.disc_mapper.dto.entity.PCs;
import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.config.ServerCache;
import com.project.disc_mapper.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
@RequestMapping("/")
public class ViewController {

    @Autowired
    private UserService userService;

    @Autowired
    private DriveService driveService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DrFilesService drFilesService;

    @Autowired
    private PCService pcService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ServerCache serverCache;


    @GetMapping("")
    public String getExistingDevices(Model model) {

        Users user = userService.findByUsername(userService.getAuthUsername());

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));

        model.addAttribute("page_name", "Devices");

        List<DevicesDTO> mergedList = devicesList(user.getId());

        model.addAttribute("cards", mergedList);

        return "home";
    }


    @GetMapping(value = "/drive/create/{driveId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDriveProgress(@PathVariable Long driveId) {
        return drFilesService.createProgressEmitter(driveId);
    }

    @PostMapping("/drive/create/{driveId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelDriveImport(@PathVariable Long driveId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (drFilesService.cancelImport(driveId)) {
                response.put("success", true);
                response.put("message", "Import cancelled successfully");
                response.put("driveId", driveId);

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Import not found or already completed");
                response.put("driveId", driveId);

                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error cancelling import: " + e.getMessage());
            response.put("driveId", driveId);

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/device/checkPcName")
    @ResponseBody
    public Map<String, Boolean> checkPcName(@RequestParam("name") String name) {
        Users user = userService.findByUsername(userService.getAuthUsername());

        boolean exists = pcService.pcExistsForCurrentUser(name, serverCache.getClientData(user.getId()).getPcModel());
        return Collections.singletonMap("exists", exists);
    }

    @GetMapping("/drive/checkDriveName")
    @ResponseBody
    public Map<String, Boolean> checkDriveName(@RequestParam("name") String name) {
        boolean exists = driveService.driveNameExistForCurrentUser(name);
        return Collections.singletonMap("exists", exists);
    }


    private List<DevicesDTO> devicesList(Long userId) {

        List<PCs> pcs = pcService.getAllDevicesByUserId(userId);
        List<Drives> drives = driveService.getAllDrivesWhereExternalByUserId(true, userId);

        return Stream.concat(
                pcs.stream().map(pcService::convertPCToDeviceDTO),
                drives.stream().map(driveService::convertDriveToDeviceDTO)
        ).toList();
    }
}
