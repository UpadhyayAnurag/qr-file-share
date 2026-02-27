package com.qrfileshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QrFileShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrFileShareApplication.class, args);
    }

}
