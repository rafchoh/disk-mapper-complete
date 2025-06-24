package com.project.disc_mapper.repo;

import com.project.disc_mapper.dto.entity.DriveFiles;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DrFilesRepo extends JpaRepository<DriveFiles, Long> {

    @Query("SELECT df FROM DriveFiles df WHERE df.drive.id = :id")
    List<DriveFiles> findAllFilesnFoldersByDriveId(@Param("id") Long id);

    @Query("SELECT COUNT(df) > 0 FROM DriveFiles df JOIN df.drive d WHERE d.user.id = :userId AND df.id = :fileId")
    boolean findAllFilesForUserId(@Param("userId") Long userId, @Param("fileId") Long fileId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DriveFiles f WHERE f.drive.id = :driveId")
    void deleteAllByDriveId(@Param("driveId") Long driveId);


    @Query("SELECT df FROM DriveFiles df JOIN df.drive d WHERE " +
            "(d.user.id = :userId) " +
            "AND (:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(df.fileName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:isExternal IS NULL OR d.isExternal = :isExternal) " +
            "AND (:driveId IS NULL OR d.id = :driveId) " +
            "AND (:isDirectory IS NULL OR df.isDirectory = :isDirectory) " +
            "AND (:startDate IS NULL OR df.lastModified >= :startDate) " +
            "AND (:endDate IS NULL OR df.lastModified <= :endDate) " +
            "ORDER BY df.lastModified DESC")
    List<DriveFiles> findFilesWithFilters(@Param("searchTerm") String searchTerm,
                                          @Param("userId") Long userId,
                                          @Param("isExternal") Boolean isExternal,
                                          @Param("driveId") Long currentDriveId,
                                          @Param("isDirectory") Boolean isDirectory,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}
