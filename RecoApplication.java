package com.escoladoestudante.reco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecoApplication {
  public static void main(String[] args) {
    SpringApplication.run(RecoApplication.class, args);
  }
}
