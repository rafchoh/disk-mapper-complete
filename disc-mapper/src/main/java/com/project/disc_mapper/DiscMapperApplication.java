package com.project.disc_mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Configuration
@EnableAsync
public class DiscMapperApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscMapperApplication.class, args);
    }
}
