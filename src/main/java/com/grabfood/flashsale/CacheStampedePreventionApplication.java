package com.grabfood.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CacheStampedePreventionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheStampedePreventionApplication.class, args);
    }
}
