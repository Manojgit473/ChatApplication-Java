package com.connectchat.service;

import com.connectchat.dto.response.SyllabusDemoResultDto;

/**
 * Service interface dedicated to demonstrating syllabus concepts:
 * - Hibernate SessionFactory, Session, and Transaction management.
 * - Core Java Reflection.
 * - Core Java Object Serialization.
 * - Core Java Generics and Stream API.
 */
public interface SyllabusDemoService {

    SyllabusDemoResultDto demonstrateHibernateSessionAndTransaction();

    SyllabusDemoResultDto demonstrateReflection();

    SyllabusDemoResultDto demonstrateSerialization();

    SyllabusDemoResultDto demonstrateGenericsAndStreams();
}

