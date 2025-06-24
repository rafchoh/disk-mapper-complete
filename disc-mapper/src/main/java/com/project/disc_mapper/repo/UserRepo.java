package com.project.disc_mapper.repo;

import com.project.disc_mapper.dto.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM Users u WHERE u.email = :email AND u.id <> :id")
    boolean existsByEmailForOtherUsers(@Param("email") String email, @Param("id") Long id);
}
