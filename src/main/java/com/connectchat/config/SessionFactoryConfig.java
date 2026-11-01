package com.connectchat.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Syllabus Integration: Hibernate SessionFactory Bean Configuration.
 * 
 * Demonstrates:
 * - Unwrapping JPA 3.x EntityManagerFactory into native Hibernate 6.x SessionFactory.
 * - Bridges standard JPA persistence with syllabus requirements for Hibernate SessionFactory.
 */
@Configuration
public class SessionFactoryConfig {

    @Bean
    public SessionFactory sessionFactory(EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.unwrap(SessionFactory.class);
    }
}
