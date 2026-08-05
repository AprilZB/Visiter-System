package com.maitong.visitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.maitong.visitor.mapper")
public class VisitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisitorApplication.class, args);
        System.out.println("\n=======================================================");
        System.out.println("  园区访客系统 (Visitor Backend) 成功启动！");
        System.out.println("  后端 API 端口: 8096");
        System.out.println("=======================================================\n");
    }
}
