package com.project.disc_mapper.controller;

import com.project.disc_mapper.dto.DevicesDTO;
import com.project.disc_mapper.dto.SearchRequestDTO;
import com.project.disc_mapper.dto.SearchResponseDTO;
import com.project.disc_mapper.dto.entity.DriveFiles;
import com.project.disc_mapper.repo.DriveRepo;
import com.project.disc_mapper.config.ServerCache;
import com.project.disc_mapper.service.DrFilesService;
import com.project.disc_mapper.service.PCService;
import com.project.disc_mapper.service.SearchService;
import com.project.disc_mapper.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {
    private static final String OFFSET_STRING = "0";
    private static final String LOADING_BATCHES_STRING = "100";
    private static final int OFFSET = Integer.parseInt(OFFSET_STRING);
    private static final int LOADING_BATCHES = Integer.parseInt(LOADING_BATCHES_STRING);

    @Autowired
    private SearchService searchService;

    @Autowired
    private PCService pcService;

    @Autowired
    private UserService userService;

    @Autowired
    private DriveRepo driveRepo;

    @Autowired
    private ServerCache serverCache;

    @GetMapping("")
    public String searchPage(Model model) {

        String gUsername = userService.getAuthUsername();
        Long userId = userService.getCurrentUserIdByUsername(gUsername);

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));

        model.addAttribute("searchRequest", new SearchRequestDTO());
        SearchResponseDTO cachedResponse = serverCache.getSearch(userId);


        if (cachedResponse != null) {
            int fetchedCount = 0;
            int totalResults = 0;

            if (cachedResponse.isFile()) {
                List<DriveFiles> allFiles = cachedResponse.getFileResults();
                totalResults = allFiles.size();

                int fromIndex = Math.min(OFFSET, totalResults);
                int toIndex = Math.min(OFFSET + LOADING_BATCHES, totalResults);
                fetchedCount = allFiles.subList(fromIndex, toIndex).size();

                model.addAttribute("fileResults", allFiles.subList(fromIndex, toIndex));
            } else {
                List<DevicesDTO> allDevices = cachedResponse.getDevices();
                totalResults = allDevices.size();

                int fromIndex = Math.min(OFFSET, totalResults);
                int toIndex = Math.min(OFFSET + LOADING_BATCHES, totalResults);
                fetchedCount = allDevices.subList(fromIndex, toIndex).size();

                model.addAttribute("fileResults", allDevices.subList(fromIndex, toIndex));
            }

            int currentLoaded = OFFSET + fetchedCount;

            model.addAttribute("success", cachedResponse.isSuccess());
            model.addAttribute("offset", OFFSET);
            model.addAttribute("size", LOADING_BATCHES);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("currentLoaded", currentLoaded);
            model.addAttribute("hasMore", currentLoaded < totalResults);
            model.addAttribute("isFileSearch", cachedResponse.isFile());
            model.addAttribute("searchTerm", cachedResponse.getSearchTerm());
        } else {
            model.addAttribute("isFileSearch", false);
            model.addAttribute("offset", OFFSET);
            model.addAttribute("size", LOADING_BATCHES);
            model.addAttribute("totalResults", 0);
            model.addAttribute("currentLoaded", OFFSET);
            model.addAttribute("fileResults", Collections.emptyList());
            model.addAttribute("hasMore", false);
            model.addAttribute("searchTerm", "");
        }

        return "search-results";
    }

    @PostMapping("/fetch")
    public String performSearch(@ModelAttribute SearchRequestDTO searchRequest,
                                @RequestParam(defaultValue = OFFSET_STRING) int offset,
                                @RequestParam(defaultValue = LOADING_BATCHES_STRING) int size) {

        String gUsername = userService.getAuthUsername();
        Long userId = userService.getCurrentUserIdByUsername(gUsername);

        searchRequest.setUserId(userId);

        if (!searchRequest.isCurrentDrive() ||
                !driveRepo.findAllDrivesForUserId(userId, searchRequest.getCurrentDriveId())) {
            searchRequest.setCurrentDriveId(null);
        }

        SearchResponseDTO response = searchService.performSearch(searchRequest);

        serverCache.saveSearch(userId, response);

        return "redirect:/search";
    }

    @PostMapping("/load-more")
    public String loadMoreResults(@RequestParam(value = "offset") int offset,
                                  @RequestParam(value = "size") int size,
                                  Model model) {

        String gUsername = userService.getAuthUsername();
        Long userId = userService.getCurrentUserIdByUsername(gUsername);

        SearchResponseDTO optionsList = serverCache.getSearch(userId);

        if (optionsList != null) {
            int fetchedCount = 0;
            int totalResults = optionsList.getTotalResults();

            if (optionsList.isFile()) {
                List<DriveFiles> allFiles = optionsList.getFileResults();

                int fromIndex = Math.min(offset, totalResults);
                int toIndex = Math.min(offset + size, totalResults);

                fetchedCount = allFiles.subList(fromIndex, toIndex).size();

                model.addAttribute("isFileSearch", true);
                model.addAttribute("fileResults", allFiles.subList(fromIndex, toIndex));
            } else {
                List<DevicesDTO> allDevices = optionsList.getDevices();

                int fromIndex = Math.min(offset, allDevices.size());
                int toIndex = Math.min(offset + size, allDevices.size());

                fetchedCount = allDevices.subList(fromIndex, toIndex).size();

                model.addAttribute("isFileSearch", false);
                model.addAttribute("fileResults", allDevices.subList(fromIndex, toIndex));
            }

            int currentLoaded = offset + fetchedCount;

            model.addAttribute("offset", offset);
            model.addAttribute("size", size);
            model.addAttribute("totalResults", totalResults);
            model.addAttribute("currentLoaded", currentLoaded);
            model.addAttribute("hasMore", currentLoaded < totalResults);
        } else {
            model.addAttribute("isFileSearch", false);
            model.addAttribute("offset", offset);
            model.addAttribute("size", size);
            model.addAttribute("totalResults", 0);
            model.addAttribute("currentLoaded", offset);
            model.addAttribute("fileResults", Collections.emptyList());
            model.addAttribute("hasMore", false);
        }

        return "fragments/common-view :: results-container-fragment";
    }
}