package com.islehub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.islehub.**.mapper")
@EnableScheduling
public class IsleHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(IsleHubApplication.class, args);
    }
}
