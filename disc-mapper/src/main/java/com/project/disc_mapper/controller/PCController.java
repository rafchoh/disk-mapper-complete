package com.project.disc_mapper.controller;

import com.project.disc_mapper.api.ClientService;
import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.dto.entity.PCs;
import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.repo.DriveRepo;
import com.project.disc_mapper.repo.PCRepo;
import com.project.disc_mapper.config.ServerCache;
import com.project.disc_mapper.service.DriveService;
import com.project.disc_mapper.service.PCService;
import com.project.disc_mapper.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/device")
public class PCController {

    @Autowired
    private UserService userService;

    @Autowired
    private PCService pcService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DriveService driveService;

    @Autowired
    private PCRepo pcRepo;

    @Autowired
    private DriveRepo driveRepo;

    @Autowired
    private ServerCache serverCache;


    @GetMapping("/add")
    public String createPcs(Model model) {

        String gUsername = userService.getAuthUsername();
        Users user = userService.findByUsername(gUsername);
        ClientService.ClientState state = ClientService.ClientState.getInstance();

        if (!state.isBusy()) {
            serverCache.saveClientData(user.getId());
        }

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pc_name", serverCache.getClientData(user.getId()).getPcName());
        model.addAttribute("pc_model", serverCache.getClientData(user.getId()).getPcModel());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));

        model.addAttribute("page_name", "Add new PC");
        model.addAttribute("pc_creator", new PCs());

        return "devices-add";
    }

    @PostMapping("/create")
    public String newPcs(@ModelAttribute PCs pcs,
                         RedirectAttributes redirectA) {

        String gUsername = userService.getAuthUsername();
        Users user = userService.findByUsername(gUsername);
        String userPcModel = serverCache.getClientData(user.getId()).getPcModel();

        boolean error = false;
        String regInfo = "Creation Failed: \n";

        if (pcs.getPcName() == null || pcs.getPcName().trim().isEmpty()) {
            regInfo += "  • PC Name is not set!";
            error = true;
        } else {
            if (!pcService.pcExistsForCurrentUser(pcs.getPcName(), userPcModel)) {
                pcs.setPcName(pcs.getPcName());
            } else {
                regInfo += "  • This PC Name already exists for this PC Model!";
                error = true;
            }
        }


        if (error) {
            redirectA.addFlashAttribute("message", regInfo);
            return "redirect:/device/add";
        } else {
            pcs.setPcModel(userPcModel);
            pcs.setPcInfo(pcs.getPcInfo());
            pcs.setUser(userService.findByUsername(gUsername));
            pcs.setPcAdded(LocalDateTime.now());

            pcRepo.save(pcs);
            redirectA.addFlashAttribute("message", "New PC created successfully!");
            return "redirect:/";
        }
    }

    @GetMapping("/{id}")
    public String getDisksForPc(@PathVariable Long id,
                                Model model) {

        if (!userService.checkPcProperty(id)) {
            return "redirect:/";
        }

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));

        PCs pcs = pcService.getPCById(id);
        List<Drives> diskForCurrentDevice = driveService.getDrivesByPcId(id);

        model.addAttribute("device_id", id);
        model.addAttribute("page_name", "Drives for " + pcs.getPcName());
        model.addAttribute("drives", diskForCurrentDevice);

        model.addAttribute("pcs", pcs);

        return "device";
    }

    @GetMapping("/{id}/edit")
    public String getPc(@PathVariable Long id,
                        Model model) {

        if (!userService.checkPcProperty(id)) {
            return "redirect:/";
        }

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));

        if (userService.isUserLoggedIn()) {
            PCs pcs = pcService.getPCById(id);

            model.addAttribute("pc_link", "/device/" + pcs.getId());

            model.addAttribute("page_name", "Update PC: " + pcs.getPcName());
            model.addAttribute("objPc", pcs);
            model.addAttribute("pc_id", id);
        }

        return "devices-edit";
    }

    @PostMapping("/{id}/edit/update")
    public String editPc(PCs pcs,
                         @PathVariable Long id,
                         Model model) {

        if (!userService.checkPcProperty(id)) {
            return "redirect:/";
        }

        if (userService.isUserLoggedIn()) {
            PCs currentPc = pcService.getPCById(id);

            String gUsername = userService.getAuthUsername();
            Users user = userService.findByUsername(gUsername);

            boolean error = false;
            String regInfo = "Update Failed: \n";

            if (pcs.getPcName() == null || pcs.getPcName().trim().isEmpty()) {
                regInfo += "  • PC Name is not set!";
                error = true;
            } else {
                if (!pcService.pcNotExistForCurrentUser(pcs.getPcName(), serverCache.getClientData(user.getId()).getPcModel())) {
                    currentPc.setPcName(pcs.getPcName());
                } else {
                    regInfo += "  • This PC Name already exists for this PC Model!";
                    error = true;
                }
            }

            if (error) {
                model.addAttribute("message", regInfo);
            } else {
                currentPc.setPcInfo(pcs.getPcInfo());

                pcRepo.save(currentPc);
                model.addAttribute("message", "PC updated successfully!");
            }
        }

        return "redirect:/device/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePc(@PathVariable Long id) {

        if (!userService.checkPcProperty(id)) {
            return "redirect:/";
        }

        if (userService.isUserLoggedIn()) {
            pcRepo.deleteById(id);
        }

        return "redirect:/";
    }
}
