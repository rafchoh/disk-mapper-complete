package com.project.disc_mapper.repo;

import com.project.disc_mapper.dto.entity.PCs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
public interface PCRepo extends JpaRepository<PCs, Long> {

    @Query("SELECT p FROM PCs p WHERE p.user.id = :id")
    List<PCs> findAllPcByUserId(@Param("id") Long id);

    @Query("SELECT COUNT(p) > 0  FROM PCs p WHERE p.user.id = :userId AND p.id = :pcId")
    boolean findAllPcByUserId(@Param("userId") Long userId, @Param("pcId") Long pcId);

    @Query("SELECT COUNT(p) > 0 FROM PCs p WHERE (p.pcName = :pcName AND p.pcModel = :pcModel) AND p.user.id = :id")
    boolean existsByPcNameAndPcModelAndUserId(@Param("pcName") String pcName, @Param("pcModel") String pcModel, @Param("id") Long id);

    @Query("SELECT COUNT(p) > 0 FROM PCs p WHERE (p.pcName = :pcName AND p.pcModel = :pcModel) AND p.user.id <> :id")
    boolean existsByPcNameAndPcModelAndOtherUserId(@Param("pcName") String pcName, @Param("pcModel") String pcModel, @Param("id") Long id);


    @Query("SELECT p FROM PCs p WHERE " +
            "(p.user.id = :userId) " +
            "AND ((:searchTerm IS NULL OR :searchTerm = '') OR " +
            "LOWER(p.pcName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.pcModel) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.pcInfo) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:startDate IS NULL OR p.pcAdded >= :startDate) " +
            "AND (:endDate IS NULL OR p.pcAdded <= :endDate) " +
            "ORDER BY p.pcAdded DESC")
    List<PCs> findPCsWithFilters(@Param("searchTerm") String searchTerm,
                                 @Param("userId") Long userId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}
