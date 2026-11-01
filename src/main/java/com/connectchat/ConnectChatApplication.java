package com.connectchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Connect Chat Application Entry Point.
 * 
 * Demonstrates:
 * - Spring Boot 3 auto-configuration and component scanning.
 * - Declarative transaction management via @EnableTransactionManagement.
 * - Spring AOP cross-cutting concerns via @EnableAspectJAutoProxy.
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class ConnectChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectChatApplication.class, args);
    }
}
