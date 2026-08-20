package com.example.herbalife_clubes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HerbalifeClubesApplication {

    public static void main(String[] args) {
        SpringApplication.run(HerbalifeClubesApplication.class, args);
    }

}
