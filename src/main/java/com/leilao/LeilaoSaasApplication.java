package com.leilao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeilaoSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeilaoSaasApplication.class, args);
    }
}
