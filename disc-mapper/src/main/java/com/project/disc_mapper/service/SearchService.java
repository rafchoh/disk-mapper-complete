package com.project.disc_mapper.service;

import com.project.disc_mapper.dto.DevicesDTO;
import com.project.disc_mapper.dto.SearchRequestDTO;
import com.project.disc_mapper.dto.SearchResponseDTO;
import com.project.disc_mapper.dto.entity.DriveFiles;
import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.dto.entity.PCs;
import com.project.disc_mapper.repo.DrFilesRepo;
import com.project.disc_mapper.repo.DriveRepo;
import com.project.disc_mapper.repo.PCRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SearchService {

    @Autowired
    private UserService userService;

    @Autowired
    private PCRepo pcRepo;

    @Autowired
    private PCService pcService;

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveRepo driveRepo;

    @Autowired
    private DrFilesRepo drFilesRepo;

    public SearchResponseDTO performSearch(SearchRequestDTO request) {

        String gUsername = userService.getAuthUsername();
        Long userId = userService.getCurrentUserIdByUsername(gUsername);

        SearchResponseDTO.SearchResponseDTOBuilder responseBuilder = SearchResponseDTO.builder()
                .userId(userId)
                .searchTerm(request.getSearchTerm())
                .searchType(request.getSearchType().toString());

        try {
            switch (request.getSearchType()) {
                case PCs:
                    List<PCs> pcsPage = pcRepo.findPCsWithFilters(
                            request.getSearchTerm(),
                            request.getUserId(),
                            request.getStartDate(),
                            request.getEndDate()
                    );
                    List<DevicesDTO> pcDevices = pcsPage.stream()
                            .map(pcService::convertPCToDeviceDTO)
                            .collect(Collectors.toList());

                    responseBuilder
                            .success(true)
                            .devices(pcDevices)
                            .totalResults(pcsPage.size())
                            .isFile(false);
                    break;
                case Drives:
                    List<Drives> drivesPage = driveRepo.findDrivesWithFilters(
                            request.getSearchTerm(),
                            request.getUserId(),
                            request.getIsExternal(),
                            request.getStartDate(),
                            request.getEndDate()
                    );
                    List<DevicesDTO> driveDevices = drivesPage.stream()
                            .map(driveService::convertDriveToDeviceDTO)
                            .collect(Collectors.toList());

                    responseBuilder
                            .success(true)
                            .devices(driveDevices)
                            .totalResults(drivesPage.size())
                            .isFile(false);
                    break;
                case DriveFiles:
                    List<DriveFiles> filesPage = drFilesRepo.findFilesWithFilters(
                            request.getSearchTerm(),
                            request.getUserId(),
                            request.getIsExternal(),
                            request.getCurrentDriveId(),
                            request.getIsDirectory(),
                            request.getStartDate(),
                            request.getEndDate()
                    );

                    responseBuilder
                            .success(true)
                            .fileResults(filesPage)
                            .totalResults(filesPage.size())
                            .isFile(true);
                    break;
                default:
                    responseBuilder
                            .success(false)
                            .message("Invalid search type");
                    break;
            }
        } catch (Exception e) {
            responseBuilder
                    .success(false)
                    .message("Search failed: " + e.getMessage());
        }

        return responseBuilder.build();
    }
}