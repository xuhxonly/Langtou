package com.langtou.interact;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.langtou.interact", "com.langtou.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.langtou.common.client")
@MapperScan("com.langtou.interact.mapper")
public class InteractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InteractServiceApplication.class, args);
    }
}
