package com.langtou.interact;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.langtou.interact", "com.langtou.common"})
@MapperScan("com.langtou.interact.mapper")
public class InteractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InteractServiceApplication.class, args);
    }
}
