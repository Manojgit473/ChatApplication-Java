package com.connectchat;

import com.connectchat.config.SessionFactoryConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Connect Chat Application Entry Point.
 * 
 * Demonstrates:
 * - Spring Boot 3 auto-configuration and component scanning.
 * - Declarative transaction management via @EnableTransactionManagement.
 * - Spring AOP cross-cutting concerns via @EnableAspectJAutoProxy.
 * - Background task scheduling via @EnableScheduling for message self-destruction.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "com.connectchat",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SessionFactoryConfig.class)
)
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableScheduling
public class ConnectChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectChatApplication.class, args);
    }
}
