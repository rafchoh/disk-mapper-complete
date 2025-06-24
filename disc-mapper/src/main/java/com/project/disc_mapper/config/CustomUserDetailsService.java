package com.project.disc_mapper.config;

import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<Users> userOptional = userRepo.findByUsername(username);

        if (userOptional.isPresent()) {

            var objUser = userOptional.get();

            return User.builder()
                    .username(objUser.getUsername())
                    .password(objUser.getPassword())
                    .build();
        } else {
            throw new UsernameNotFoundException(username + " not found");
        }
    }
}