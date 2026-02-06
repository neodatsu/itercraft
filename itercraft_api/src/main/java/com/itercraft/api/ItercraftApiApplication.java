package com.itercraft.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItercraftApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItercraftApiApplication.class, args);
    }
}
