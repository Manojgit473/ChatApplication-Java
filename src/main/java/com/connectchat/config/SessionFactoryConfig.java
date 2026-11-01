package com.connectchat.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Syllabus Integration: Hibernate SessionFactory Bean Configuration.
 * 
 * Configured as an AutoConfiguration running strictly after HibernateJpaAutoConfiguration.
 * This guarantees that Spring Boot's standard JPA EntityManagerFactory is created first,
 * and the custom Hibernate SessionFactory is unwrapped and made available without triggering
 * bean definition naming conflicts.
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnBean(name = "entityManagerFactory")
public class SessionFactoryConfig {

    @Bean
    public SessionFactory sessionFactory(EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.unwrap(SessionFactory.class);
    }
}
