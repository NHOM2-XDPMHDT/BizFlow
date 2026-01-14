package com.example.bizflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BizflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizflowApplication.class, args);
    }
}
