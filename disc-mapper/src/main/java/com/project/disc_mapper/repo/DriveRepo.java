package com.project.disc_mapper.repo;

import com.project.disc_mapper.dto.entity.Drives;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;

public interface DriveRepo extends JpaRepository<Drives, Long> {

    @Query("SELECT d FROM Drives d WHERE d.isExternal = :isExternal AND d.user.id = :id")
    List<Drives> findAllDrivesWhereExternalByUserId(@Param("isExternal") boolean isExternal, @Param("id") Long id);

    @Query("SELECT d FROM Drives d WHERE d.isExternal = :isExternal AND d.pcs.id = :id")
    List<Drives> findAllDrivesByPcId(@Param("isExternal") boolean isExternal, @Param("id") Long id);

    @Query("SELECT d.driveMap FROM Drives d WHERE d.id = :driveId")
    String getMapFromDbForDrive(@Param("driveId") Long driveId);

    @Query("SELECT COUNT(d) > 0 FROM Drives d WHERE d.user.id = :userId AND d.id = :driveId")
    boolean findAllDrivesForUserId(@Param("userId") Long userId, @Param("driveId") Long driveId);

    @Query("SELECT COUNT(d) > 0 FROM Drives d WHERE d.driveName = :driveName AND d.user.id = :id")
    boolean existsByDriveNameForUser(@Param("driveName") String driveName, @Param("id") Long userId);

    @Query("SELECT COUNT(d) > 0 FROM Drives d WHERE d.driveName = :driveName AND d.user.id <> :id")
    boolean existsByDriveNameForOtherUsers(@Param("driveName") String driveName, @Param("id") Long userId);


    @Query("SELECT d FROM Drives d WHERE " +
            "(d.user.id = :userId) " +
            "AND (:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(d.driveName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.drivePath) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:isExternal IS NULL OR d.isExternal = :isExternal) " +
            "AND (:startDate IS NULL OR d.lastMapped >= :startDate) " +
            "AND (:endDate IS NULL OR d.lastMapped <= :endDate) " +
            "ORDER BY d.lastMapped DESC")
    List<Drives> findDrivesWithFilters(@Param("searchTerm") String searchTerm,
                                       @Param("userId") Long userId,
                                       @Param("isExternal") Boolean isExternal,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}
