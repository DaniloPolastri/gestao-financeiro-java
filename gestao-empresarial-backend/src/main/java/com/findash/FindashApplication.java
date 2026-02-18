package com.findash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FindashApplication {
    public static void main(String[] args) {
        SpringApplication.run(FindashApplication.class, args);
    }
}
