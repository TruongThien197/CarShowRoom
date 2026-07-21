package com.hsf302.carshowroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarShowRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarShowRoomApplication.class, args);
    }

}
