package com.project.disc_mapper.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> {
                    request.requestMatchers(
                            "/css/**",
                            "/js/**",
                            "/icons/**",
                            "/api/**",
                            "/user/**"
                    ).permitAll();
                    request.requestMatchers(
                            "/device/**",
                            "/drive/**",
                            "/user/profile",
                            "/user/profile/info-update"
                    ).authenticated();
                    request.anyRequest().authenticated();
                })
                .formLogin(form -> {
                    form
                            .loginPage("/user/login")
                            .failureUrl("/user/login?error=true")
                            .loginProcessingUrl("/user/login")
                            .defaultSuccessUrl("/", true)
                            .permitAll();
                })
                .logout(logout -> {
                    logout
                            .logoutUrl("/logout")
                            .invalidateHttpSession(true);
                })
                .build();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }
}
