package com.project.disc_mapper.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8081")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean(name = "importExecutor")
    public Executor taskExecutor() {

        ThreadPoolTaskExecutor tExecutor = new ThreadPoolTaskExecutor();

        tExecutor.setCorePoolSize(8);
        tExecutor.setMaxPoolSize(16);
        tExecutor.setQueueCapacity(1000);
        tExecutor.setThreadNamePrefix("AsyncImport-");

        tExecutor.setAllowCoreThreadTimeOut(true);
        tExecutor.setKeepAliveSeconds(30);
        tExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

//        tExecutor.initialize();
        return tExecutor;
    }
}