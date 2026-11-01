package com.memoryassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MemoryAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryAssistantApplication.class, args);
    }
}
