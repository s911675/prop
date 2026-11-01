package com.example.prop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.prop.property")
public class PropApplication {
    public static void main(String[] args) {
        SpringApplication.run(PropApplication.class, args);
    }
}
