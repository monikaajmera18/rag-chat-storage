package com.ragchat.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class RagChatStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagChatStorageApplication.class, args);
    }
}