package com.langtou.ad;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.langtou.ad", "com.langtou.common"})
@EnableDiscoveryClient
@MapperScan("com.langtou.ad.mapper")
public class AdServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdServiceApplication.class, args);
    }
}
