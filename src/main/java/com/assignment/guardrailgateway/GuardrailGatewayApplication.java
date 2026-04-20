package com.assignment.guardrailgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GuardrailGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuardrailGatewayApplication.class, args);
    }
}
