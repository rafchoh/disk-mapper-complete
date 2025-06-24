package com.project.disc_mapper.repo;

import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.dto.entity.ResetTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepo extends JpaRepository<ResetTokens, Long> {

    Optional<ResetTokens> findByUserId (Long id);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
