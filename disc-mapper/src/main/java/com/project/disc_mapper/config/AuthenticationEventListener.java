package com.project.disc_mapper.config;

import com.project.disc_mapper.api.ClientController;
import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class AuthenticationEventListener {

    @Autowired
    private final UserService userService;

    @Autowired
    private final ServerCache serverCache;

    @Autowired
    private final ClientController clientController;


    public AuthenticationEventListener(UserService userService,
                                       ServerCache serverCache,
                                       ClientController clientController) {
        this.userService = userService;
        this.serverCache = serverCache;
        this.clientController = clientController;
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        Users user = userService.findByUsername(username);

//        serverCache.saveClientData(user.getId());
    }

    @EventListener
    public void handleLogoutSuccess(LogoutSuccessEvent event) {
        String username = event.getAuthentication().getName();
        if (username == null) {
            return;
        }
        Users user = userService.findByUsername(username);

        serverCache.clearClientData(user.getId());
        serverCache.clearSearch(user.getId());
        SecurityContextHolder.clearContext();

        clientController.revokeAccess();

        System.out.println("Cleanup done for user on logout: " + username);
    }
}
