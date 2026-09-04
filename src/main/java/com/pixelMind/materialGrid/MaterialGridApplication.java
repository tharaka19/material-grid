package com.pixelMind.materialGrid;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MaterialGridApplication {

    @PostConstruct
    public void init() {
        // Set application default timezone to Sri Lanka (Asia/Colombo, UTC+05:30)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MaterialGridApplication.class, args);
    }
}
