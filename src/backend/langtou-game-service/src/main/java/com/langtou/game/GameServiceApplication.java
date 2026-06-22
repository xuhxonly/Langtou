package com.langtou.game;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.langtou.game", "com.langtou.common"})
@EnableDiscoveryClient
@MapperScan("com.langtou.game.mapper")
public class GameServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
