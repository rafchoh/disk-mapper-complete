package com.project.disc_mapper.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.disc_mapper.api.ClientService;
import com.project.disc_mapper.dto.ClientDTO;
import com.project.disc_mapper.dto.DriveDataDTO;
import com.project.disc_mapper.dto.entity.DriveFiles;
import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.repo.DrFilesRepo;
import com.project.disc_mapper.repo.DriveRepo;
import com.project.disc_mapper.config.ServerCache;
import com.project.disc_mapper.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/drive")
public class DriveController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();

    @Autowired
    private UserService userService;
    @Autowired
    private DriveService driveService;
    @Autowired
    private DrFilesService drFilesService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DriveRepo driveRepo;
    @Autowired
    private PCService pcService;
    @Autowired
    private DrFilesRepo dfRepo;
    @Autowired
    private ServerCache serverCache;

    private void startAsyncDriveMapping(Drives drives) throws IOException {

        Path drivePath = Paths.get(drives.getDrivePath());
        CompletableFuture<Map<String, Object>> driveStructure = CompletableFuture.supplyAsync(() -> {
            try {
                return clientService.mapDirectoryStructure(String.valueOf(drivePath));
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing JSON to Map", e);
            }
        });

        driveStructure
                .thenCompose(structureMap -> {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            StringWriter stringWriter = new StringWriter();
                            JsonGenerator gen = factory.createGenerator(stringWriter);
                            mapper.writeValue(gen, structureMap);
                            gen.close();
                            String json = stringWriter.toString();

                            if (!json.isEmpty()) {
                                drives.setDriveMap(json);
                                driveRepo.save(drives);
                            }
                            return drives;
                        } catch (IOException e) {
                            throw new RuntimeException("Error serializing drive structure", e);
                        }
                    });
                })
                .thenAccept(savedDrive -> {
                    try {
                        drFilesService.importFilesInBackground(savedDrive.getDrivePath(), savedDrive.getId());
                    } catch (Exception e) {
                        throw new RuntimeException("Error starting file import", e);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in drive mapping chain: " + throwable.getMessage());
                    return null;
                });
    }


    @GetMapping("/add")
    public String getMapper(Model model) {

        String gUsername = userService.getAuthUsername();
        Users user = userService.findByUsername(gUsername);

        ClientService.ClientState state = ClientService.ClientState.getInstance();

        if (!state.isBusy()) {
            serverCache.saveClientData(user.getId());
        }

        ClientDTO cdt = serverCache.getClientData(user.getId());

        model.addAttribute("users_name", gUsername);
        model.addAttribute("user_id", userService.getCurrentUserIdByUsername(gUsername));

        model.addAttribute("page_name", "Map your Drive");
        model.addAttribute("drive_mapper", new Drives());

        List<DriveDataDTO> ddd = cdt.getDriveData();
        List<String> selectedDriveDisplay = new ArrayList<>();
        for (DriveDataDTO item : ddd) {
            selectedDriveDisplay.add(item.getDriveRoot() + " " + item.getTotalStorage());
        }

        model.addAttribute("drive_opt", selectedDriveDisplay);
        model.addAttribute("pcs_opt", pcService.myPCs(gUsername));

        return "drives-add";
    }

    @PostMapping("/create")
    public String mapDrive(@ModelAttribute Drives drives,
                           RedirectAttributes redirectA) {

        String gUsername = userService.getAuthUsername();
        Users user = userService.findByUsername(gUsername);

        String json = "";

        boolean error = false;
        String regInfo = "Mapping Failed: \n";

        ClientService.ClientState state = ClientService.ClientState.getInstance();

        if (drives.getDriveName() == null || drives.getDriveName().trim().isEmpty()) {
            regInfo += "  • Disk Name is not set! \n";
            redirectA.addFlashAttribute("message", regInfo);
            return "redirect:/drive/add";
        }

        if (drFilesService.isDriveMappingInProgressForUser(gUsername) || state.isBusy()) {
            regInfo += "  • A Disk is already being mapped, please wait. \n";
            redirectA.addFlashAttribute("message", regInfo);
            return "redirect:/drive/add";
        }

        Path drivePath = Paths.get(drives.getDrivePath());
        if (!Files.exists(drivePath) || !Files.isDirectory(drivePath)) {
            regInfo += "  • Invalid drive path provided! \n";
            redirectA.addFlashAttribute("message", regInfo);
            return "redirect:/drive/add";
        }

        List<DriveDataDTO> ddd = serverCache.getClientData(user.getId()).getDriveData();

        Map<String, String> dddMapTotalStorage = new HashMap<>();
        Map<String, String> dddMapAvailableStorage = new HashMap<>();

        for (DriveDataDTO item : ddd) {
            dddMapTotalStorage.put(item.getDriveRoot(), item.getTotalStorage());
            dddMapAvailableStorage.put(item.getDriveRoot(), item.getAvailableStorage());
        }

        try {
            boolean external = drives.getIsExternal();
            if (!driveService.driveNameExistForCurrentUser(drives.getDriveName())) {
                drives.setDriveName(drives.getDriveName());

                if (!dddMapTotalStorage.containsKey(drives.getDrivePath())) {

                    regInfo += "  • Drive not available! \n";
                    redirectA.addFlashAttribute("message", regInfo);

                    return "redirect:/drive/add";
                }

                if (!external) {
                    drives.setPcs(drives.getPcs());
                } else {
                    drives.setPcs(null);
                }
            } else {
                if (!external) {
                    regInfo += "  • This Drive Name already exists for this user! \n";
                    error = true;
                } else {
                    error = true;
                    regInfo += "  • This External Drive Name is already used! \n";
                }
            }

            if (error) {
                redirectA.addFlashAttribute("message", regInfo);
                return "redirect:/drive/add";
            }

            drives.setDriveSize(dddMapTotalStorage.get(drives.getDrivePath()));
            drives.setStorageLeft(dddMapAvailableStorage.get(drives.getDrivePath()));
            drives.setUser(userService.findByUsername(gUsername));
            drives.setLastMapped(LocalDateTime.now());

            Drives savedDrive = driveRepo.save(drives);

            startAsyncDriveMapping(savedDrive);


            redirectA.addFlashAttribute("driveId", savedDrive.getId());
            redirectA.addFlashAttribute("message", "Drive mapping started! Progress will be shown on the dashboard.");

            return "redirect:/";

        } catch (Exception e) {
            error = true;
            regInfo += "  • Error: " + e.getMessage() + "\n";
            redirectA.addFlashAttribute("message", regInfo);

            return "redirect:/drive/add";
        }
    }

    @GetMapping("/{id}")
    public String getDrivesForPc(@PathVariable Long id,
                                 @RequestParam(value = "fileId", required = false) Long fileId,
                                 Model model) {

        if (!userService.checkDriveProperty(id)) {
            return "redirect:/";
        }

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));
        model.addAttribute("page_name", "File Explorer");
        model.addAttribute("drive_id", id);
        model.addAttribute("drive_id_s", id);

        Map<String, List<DriveFiles>> files = drFilesService.getFilesnFoldersByDriveId(id);
        String rootPath = files.keySet().stream().findFirst().orElse(null);

        if (files.containsKey(rootPath)) {
            model.addAttribute("filesExplorer", files);
            model.addAttribute("rootPath", rootPath);
        }

        Drives drives = driveService.getDriveById(id);

        if (!drives.getIsExternal()) {
            model.addAttribute("pc_link", "/device/" + drives.getPcs().getId());
        }

        model.addAttribute("page_name", "Drives: " + drives.getDriveName());
        model.addAttribute("objMDrive", drives);
        model.addAttribute("objDrive", drives);

        return "drive";
    }

    @GetMapping("/{id}/edit")
    public String getDriveEdit(@PathVariable Long id,
                           Model model) {

        if (!userService.checkDriveProperty(id)) {
            return "redirect:/";
        }

        String gUsername = userService.getAuthUsername();

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));
        model.addAttribute("drive_id", id);

        if (userService.isUserLoggedIn()) {
            Drives drives = driveService.getDriveById(id);

            model.addAttribute("pc_link", "/drive/" + drives.getId());

            model.addAttribute("page_name", "Update Drive: " + drives.getDriveName());
            model.addAttribute("drive_id", id);
            model.addAttribute("objDrive", drives);
            model.addAttribute("pcs_opt", pcService.myPCs(gUsername));

            if (drives.getIsExternal()) {
                model.addAttribute("externalDrive", true);
            } else {
                model.addAttribute("externalDrive", false);
            }
        }

        return "drives-edit";
    }

    @PostMapping("/{id}/edit/update")
    public String editDrive(Drives drive,
                            @PathVariable Long id,
                            RedirectAttributes redirectA) {

        if (!userService.checkDriveProperty(id)) {
            return "redirect:/";
        }

        if (userService.isUserLoggedIn()) {
            Drives currentDrive = driveService.getDriveById(id);

            String gUsername = userService.getAuthUsername();
            Users user = userService.findByUsername(gUsername);

            ClientService.ClientState state = ClientService.ClientState.getInstance();

            if (!state.isBusy()) {
                serverCache.saveClientData(user.getId());
            }

            List<DriveDataDTO> ddd = serverCache.getClientData(user.getId()).getDriveData();
            Map<String, String> dddMapTotalStorage = new HashMap<>();

            for (DriveDataDTO item : ddd) {
                dddMapTotalStorage.put(item.getDriveRoot(), item.getTotalStorage());
            }

            boolean error = false;
            String regInfo = "Update Failed: \n";

            boolean isValid = currentDrive.getIsExternal() ||
                    (currentDrive.getDriveSize().equals(dddMapTotalStorage.get(currentDrive.getDrivePath())) &&
                            currentDrive.getPcs().getPcModel().equals(serverCache.getClientData(user.getId()).getPcModel()) &&
                            dddMapTotalStorage.containsKey(currentDrive.getDrivePath()));

            if (isValid) {
                boolean external = currentDrive.getIsExternal();
                if (!driveService.driveNameExistForOtherUsers(currentDrive.getDriveName())) {
                    currentDrive.setDriveName(drive.getDriveName());

                    if (!external) {
                        currentDrive.setPcs(drive.getPcs());
                    } else {
                        currentDrive.setPcs(null);
                    }
                } else {
                    if (!external) {
                        regInfo += "  • This Drive Name already exists for this user! \n";
                        error = true;
                    } else {
                        error = true;
                        regInfo += "  • This External Drive Name is already used! \n";
                    }
                }
            } else {
                regInfo += "  • Drive path, storage size or PC model doesn't match with the data of the current drive! \n";
                redirectA.addFlashAttribute("message", regInfo);
                return "redirect:/drive/" + id;
            }

            if (error) {
                redirectA.addFlashAttribute("message", regInfo);
            } else {
                driveRepo.save(currentDrive);
            }
        }

        return "redirect:/drive/" + id;
    }

    @PostMapping("/{id}/edit/remap")
    public String remapDrive(@PathVariable Long id,
                             RedirectAttributes redirectA) {

        if (!userService.checkDriveProperty(id)) {
            return "redirect:/";
        }

        if (userService.isUserLoggedIn()) {
            String gUsername = userService.getAuthUsername();
            Users user = userService.findByUsername(gUsername);

            ClientService.ClientState state = ClientService.ClientState.getInstance();

            if (!state.isBusy()) {
                serverCache.saveClientData(user.getId());
            }

            List<DriveDataDTO> ddd = serverCache.getClientData(user.getId()).getDriveData();

            Map<String, String> dddMapTotalStorage = new HashMap<>();
            Map<String, String> dddMapAvailableStorage = new HashMap<>();

            for (DriveDataDTO item : ddd) {
                dddMapTotalStorage.put(item.getDriveRoot(), item.getTotalStorage());
                dddMapAvailableStorage.put(item.getDriveRoot(), item.getAvailableStorage());
            }

            String json = "";

            Drives currentDrive = driveService.getDriveById(id);

            boolean error = false;
            String regInfo = "Mapping Failed: \n";

            if (drFilesService.isDriveMappingInProgressForUser(gUsername) || state.isBusy()) {
                regInfo += "  • A Disk is already being mapped, please wait. \n";
                redirectA.addFlashAttribute("message", regInfo);

                return "redirect:/drive/" + id;
            }

            boolean isValid = currentDrive.getIsExternal()
                    ? currentDrive.getDriveSize().equals(dddMapTotalStorage.get(currentDrive.getDrivePath()))
                    : (currentDrive.getDriveSize().equals(dddMapTotalStorage.get(currentDrive.getDrivePath())) &&
                    currentDrive.getPcs().getPcModel().equals(serverCache.getClientData(user.getId()).getPcModel()) &&
                    dddMapTotalStorage.containsKey(currentDrive.getDrivePath()));

            if (isValid) {
                Path drivePath = Paths.get(currentDrive.getDrivePath());
                if (!Files.exists(drivePath) || !Files.isDirectory(drivePath)) {
                    regInfo += "  • Invalid drive path provided! \n";
                    redirectA.addFlashAttribute("message", regInfo);
                    return "redirect:/drive/" + id;
                }

                try {
                    boolean external = currentDrive.getIsExternal();

                    if (error) {
                        redirectA.addFlashAttribute("message", regInfo);
                        return "redirect:/drive/" + id;
                    }

                    dfRepo.deleteAllByDriveId(currentDrive.getId());

                    currentDrive.setStorageLeft(dddMapAvailableStorage.get(currentDrive.getDrivePath()));
                    currentDrive.setLastMapped(LocalDateTime.now());

                    Drives savedDrive = driveRepo.save(currentDrive);
                    startAsyncDriveMapping(savedDrive);

                    redirectA.addFlashAttribute("driveId", savedDrive.getId());
                    redirectA.addFlashAttribute("message_pb", "Connected. Waiting for progress updates...");
                } catch (Exception e) {
                    error = true;
                    regInfo += "  • Error: " + e.getMessage() + "\n";
                    redirectA.addFlashAttribute("message", regInfo);
                }
            }
        }

        return "redirect:/drive/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePc(Drives drive,
                           @PathVariable Long id,
                           RedirectAttributes redirectA) {

        if (!userService.checkDriveProperty(id)) {
            return "redirect:/";
        }

        if (userService.isUserLoggedIn()) {
            Long driveId = drive.getId();
            Double mapProgress = drFilesService.getProgressValue(driveId);

            String regInfo = "Delete Failed: \n";

            if (mapProgress != null && mapProgress > 0 && mapProgress < 100) {
                regInfo += "  • The Disk is being uploaded, please wait. \n";
                redirectA.addFlashAttribute("message", regInfo);
                return "redirect:/drive/" + id;
            }

            driveRepo.deleteById(id);
        }

        return "redirect:/";
    }
}