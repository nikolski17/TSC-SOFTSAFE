package com.tsc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Esta línea inicia el servidor web y toda la magia de Spring Boot
        SpringApplication.run(Application.class, args);
    }

}
