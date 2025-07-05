package com.project.disc_mapper.controller;

import com.project.disc_mapper.dto.entity.ResetTokens;
import com.project.disc_mapper.dto.entity.Users;
import com.project.disc_mapper.repo.TokenRepo;
import com.project.disc_mapper.repo.UserRepo;
import com.project.disc_mapper.service.MailSenderService;
import com.project.disc_mapper.service.PCService;
import com.project.disc_mapper.service.TokenService;
import com.project.disc_mapper.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {
    private static int AUTH_KEY_ACTIVE_SECONDS = 90;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private PCService pcService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private MailSenderService mailSenderService;

    @Autowired
    private TokenRepo tokenRepo;

    @Autowired
    private TokenService tokenService;


    @GetMapping("/login")
    public String loginUser(@RequestParam(name = "mode", defaultValue = "login") String mode,
                            @RequestParam(value = "error", required = false) String error,
                            RedirectAttributes redirectA,
                            Model model) {

        if (mode.equals("login")) {
            model.addAttribute("page_name", "Login");
            model.addAttribute("resetToken", null);
        } else {
            model.addAttribute("page_name", "Password Recovery");
            model.addAttribute("resetToken", new ResetTokens());
        }

        if (error != null) {
            redirectA.addFlashAttribute("message", "Wrong Credentials!");
            return "redirect:/user/login";
        }

        model.addAttribute("mode", mode);
        model.addAttribute("users_creator", new Users());

        return "login";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute Users user,
                               RedirectAttributes redirectA) {

        boolean error = false;
        String regInfo = "Registration Failed: \n";

        user.setFullName(user.getFullName());

        if (userRepo.existsByUsername(user.getUsername())) {
            regInfo += "  • Username already exists! \n";
            error = true;
        } else {
            user.setUsername(user.getUsername());
        }

        if (userService.isEmailPresent(user.getEmail()) && !user.getEmail().isEmpty()) {
            regInfo += "  • Email already exists! \n";
            error = true;
        } else {
            user.setUsername(user.getUsername());
        }

        if (!user.getPassword().equals(user.getReTypePassword())) {
            regInfo += "   • Passwords do not match! ";
            error = true;
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        if (error) {
            redirectA.addFlashAttribute("message", regInfo);
        } else {
            userRepo.save(user);
            redirectA.addFlashAttribute("message", "User created successfully!");
        }

        return "redirect:/user/login";
    }

    @GetMapping("/login/checkUsername")
        @ResponseBody
        ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam("username") String username) {
            boolean exists = userRepo.existsByUsername(username.trim());
            Map<String, Boolean> response = Collections.singletonMap("exists", exists);

            return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery")
    public String recoverPassword(@ModelAttribute("resetToken") ResetTokens prt,
                                  RedirectAttributes redirectA) {

        boolean error = false;
        String regInfo = "Recovery Failed: \n";

        double elapsedSeconds = 0;
        long remains = 0;

        Users currentUser = userService.findByUsername(prt.getUsername());

        if (currentUser == null) {
            error = true;
            regInfo += "   • Username does not exist!";
        } else {
            if (currentUser.getEmail() == null) {
                error = true;
                regInfo += "   • User doesn't have set e-mail in the system!";
            } else {
                prt.setCreatedAt(LocalDateTime.now());
                prt.setUser(currentUser);
            }
        }

        if (error) {
            redirectA.addFlashAttribute("message", regInfo);

            return "redirect:/user/login";
        } else {
            ResetTokens existingToken = tokenService.getRecoveryByUserId(currentUser.getId());

            boolean tokenAlive = existingToken.getUser() != null &&
                    !tokenService.deleteIfIsExpired(currentUser.getId(), AUTH_KEY_ACTIVE_SECONDS);
            if (!tokenAlive) {
                long start = System.currentTimeMillis();

                String tokenGen = tokenService.generateRecoveryKey();
                prt.setToken(tokenGen);

                tokenRepo.save(prt);

                mailSenderService.sendEmail(
                        currentUser.getEmail(),
                        "Password recovery e-mail",
                        mailSenderService.buildPasswordRecoveryEmail(
                                currentUser.getUsername(),
                                tokenGen
                        )
                );

                long end = System.currentTimeMillis();
                long elapsedMillis = end - start;
                elapsedSeconds = elapsedMillis / 1000.0;

                AUTH_KEY_ACTIVE_SECONDS += (int) Math.ceil(elapsedSeconds);


                remains = mailSenderService.getRemainingSeconds(prt.getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS);

                redirectA.addFlashAttribute("message", "Recovery key sent to your e-mail! \n Check your Spam box!");
            } else {
                ResetTokens currentPrt = tokenService.getRecoveryByUserId(currentUser.getId());
                remains = mailSenderService.getRemainingSeconds(currentPrt.getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS);

                redirectA.addFlashAttribute("message", "Recovery key still alive!");
            }

            redirectA.addFlashAttribute("timerInitial", Math.max(0, remains));

            return "redirect:/user/login/" + currentUser.getUsername();
        }
    }

    @GetMapping("/login/{username}")
    public String authKey(@PathVariable String username,
                          Model model) {

        model.addAttribute("page_name", "Authenticate");
        model.addAttribute("authToken", new ResetTokens());

        Users currentUser = userService.findByUsername(username);
        model.addAttribute("userName", currentUser.getUsername());

        tokenService.getToken(currentUser.getId()).ifPresent(e -> {
            long remains = mailSenderService.getRemainingSeconds(e.getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS);

            model.addAttribute("timerInitial", remains);
            if (!mailSenderService.isValidToken(e.getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS)) {
                tokenService.deleteIfIsExpired(currentUser.getId(), AUTH_KEY_ACTIVE_SECONDS);
            }
        });

        return "auth";
    }

    @PostMapping("/login/{username}/auth")
    public String editPassword(@ModelAttribute("authToken") ResetTokens prt,
                               @PathVariable String username,
                               RedirectAttributes redirectA) {

        long remains = 0;

        boolean error = false;
        String regInfo = "";

        Users currentUser = userService.findByUsername(username);

        Optional<ResetTokens> currentOrt = tokenService.getToken(currentUser.getId());
        ResetTokens currentResToken = currentOrt.get();

        if (currentOrt.isEmpty() ||
                !mailSenderService.isValidToken(currentOrt.get().getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS)) {
            regInfo += "   • Validation Key has expired! ";
            error = true;

            tokenService.deleteIfIsExpired(currentUser.getId(), AUTH_KEY_ACTIVE_SECONDS);
        }

        if (!currentResToken.getToken().equals(prt.getToken())) {
            regInfo += "   • Validation Key isn't correct!";
            error = true;

            remains = mailSenderService.getRemainingSeconds(currentResToken.getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS);
            redirectA.addFlashAttribute("timerInitial", Math.max(0, remains));
        }


        if (error) {
            redirectA.addFlashAttribute("message", regInfo);
            currentUser.setRecoveryMode(false);
            userRepo.save(currentUser);

            return "redirect:/user/login/" + username;
        } else {
            currentUser.setRecoveryMode(true);
            userRepo.save(currentUser);

            return "redirect:/user/profile/" + username + "/pass-edit";
        }
    }

    @GetMapping("/profile")
    public String userPage(Model model) {

        model.addAttribute("users_name", userService.getAuthUsername());
        model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));

        if (userService.isUserLoggedIn()) {
            Users user = userService.findByUsername(userService.getAuthUsername());

            model.addAttribute("page_name", "Profile");
            model.addAttribute("authUsername", userService.getAuthUsername());

            model.addAttribute("userId", user.getId());
            model.addAttribute("objUser", user);
            model.addAttribute("objPassUser", user);
        } else {
            return "redirect:/user/login";
        }

        return "profile";
    }

    @PostMapping("/profile/info-update")
    public String infoUpdateUser(@ModelAttribute("objUser") Users user,
                                 RedirectAttributes redirectA) {

        if (userService.isUserLoggedIn()) {
            Users currentUser = userService.findByUsername(userService.getAuthUsername());

            if (!passwordEncoder.matches(user.getPassword(), currentUser.getPassword())) {
                redirectA.addFlashAttribute("message", "Current password is incorrect!");
                return "redirect:/user/profile";
            } else {
                if (!userService.emailExistForOtherUsers(user.getEmail())) {
                    currentUser.setEmail(user.getEmail());
                } else {
                    redirectA.addFlashAttribute("message", "Email is already used by another user!");
                    return "redirect:/user/profile";
                }

                currentUser.setUsername(currentUser.getUsername());
                currentUser.setFullName(user.getFullName());
            }

            userRepo.save(currentUser);
            redirectA.addFlashAttribute("message", "Your Data is saved successfully!");
        } else {
            return "redirect:/user/login";
        }

        return "redirect:/user/profile";
    }

    @GetMapping("/profile/{username}/pass-edit")
    public String userPasswordEdit(@PathVariable String username,
                                   Model model,
                                   HttpSession session) {

        Users currentUser = userService.findByUsername(username);

        boolean hasValidToken = tokenService.existsValid(currentUser.getId(), AUTH_KEY_ACTIVE_SECONDS);
        if (hasValidToken) {
            ResetTokens currentPrt = tokenService.getRecoveryByUserId(currentUser.getId());
            long remains = mailSenderService.getRemainingSeconds(currentPrt.getCreatedAt(), AUTH_KEY_ACTIVE_SECONDS);
            model.addAttribute("timerInitial", remains);
        } else {
            tokenService.deleteIfIsExpired(currentUser.getId(), AUTH_KEY_ACTIVE_SECONDS);
        }


        if (currentUser.isRecoveryMode()) {
            model.addAttribute("page_name", "Recover Password");
            model.addAttribute("objPassUser", currentUser);
            model.addAttribute("authUsername", currentUser.getUsername());

            currentUser.setRecoveryMode(false);
            userRepo.save(currentUser);

            session.setAttribute("inRecovery", true);
        } else {
            model.addAttribute("page_name", "Change Password");

            if (userService.isUserLoggedIn()) {
                model.addAttribute("users_name", userService.getAuthUsername());
                model.addAttribute("pcs_opt", pcService.myPCs(userService.getAuthUsername()));


                model.addAttribute("authUsername", userService.getAuthUsername());
                model.addAttribute("profileEdit", true);
                model.addAttribute("objPassUser", currentUser);
            } else {
                return "redirect:/user/profile";
            }
        }

        return "passedit";
    }

    @PostMapping("/profile/{username}/pass-update")
    public String passUpdateUser(@ModelAttribute("objPassUser") Users user,
                                 @PathVariable String username,
                                 RedirectAttributes redirectA,
                                 HttpSession session) {

        Users currentUser = userService.findByUsername(username);

        boolean sessionReco = session.getAttribute("inRecovery") != null &&
                                    (boolean) session.getAttribute("inRecovery");

        if (!sessionReco) {
            if (userService.isUserLoggedIn()) {
                if (user.getNewPassword().equals(user.getReTypePassword())) {
                    if (passwordEncoder.matches(user.getNewPassword(), currentUser.getPassword())) {
                        redirectA.addFlashAttribute("message", "You are already using this password!");
                        return "redirect:/user/profile";
                    } else {
                        currentUser.setPassword(passwordEncoder.encode(user.getNewPassword()));
                    }
                } else {
                    redirectA.addFlashAttribute("message", "Your passwords doesn't match!");
                    return "redirect:/user/profile";
                }

                userRepo.save(currentUser);
                redirectA.addFlashAttribute("message", "Your Password is changed successfully!");
            } else {
                redirectA.addFlashAttribute("message", "You're not logged in!");

                return "redirect:/user/login";
            }

            return "redirect:/user/profile";
        } else {
            if (user.getNewPassword().equals(user.getReTypePassword())) {
                currentUser.setPassword(passwordEncoder.encode(user.getNewPassword()));
                userRepo.save(currentUser);

                session.removeAttribute("inRecovery");
            } else {
                redirectA.addFlashAttribute("message", "Your passwords doesn't match!");
                return "redirect:/user/profile/" + username + "/pass-edit";
            }
            redirectA.addFlashAttribute("message", "Your Password is changed successfully!");

            return "redirect:/user/login";
        }
    }

    @PostMapping("/profile/{id}/delete")
    public String deleteUser(@PathVariable Long id) {

        if (userService.isUserLoggedIn()) {
            userRepo.deleteById(id);
        }

        return "redirect:/user/login";
    }
}