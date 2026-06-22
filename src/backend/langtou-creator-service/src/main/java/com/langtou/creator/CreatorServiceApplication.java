package com.langtou.creator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.langtou.creator", "com.langtou.common"})
@EnableDiscoveryClient
@MapperScan("com.langtou.creator.mapper")
public class CreatorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreatorServiceApplication.class, args);
    }
}
