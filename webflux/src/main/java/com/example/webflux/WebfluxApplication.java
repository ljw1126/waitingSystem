package com.example.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WebfluxApplication {

  public static void main(String[] args) {
    SpringApplication.run(WebfluxApplication.class, args);
  }
}
