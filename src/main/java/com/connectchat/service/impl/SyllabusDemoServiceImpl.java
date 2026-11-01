package com.connectchat.service.impl;

import com.connectchat.dto.response.SyllabusDemoResultDto;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.service.SyllabusDemoService;
import com.connectchat.util.GenericCollectionUtils;
import com.connectchat.util.ReflectionDemoUtils;
import com.connectchat.util.SerializationDemoUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SyllabusDemoService.
 * 
 * Demonstrates:
 * - Direct use of Hibernate SessionFactory, Session, and Transaction for viva explanation.
 * - Core Java Reflection, Object Serialization, Generics, and Streams.
 */
@Service
public class SyllabusDemoServiceImpl implements SyllabusDemoService {

    private final SessionFactory sessionFactory;

    public SyllabusDemoServiceImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public SyllabusDemoResultDto demonstrateHibernateSessionAndTransaction() {
        long start = System.nanoTime();
        Map<String, Object> details = new LinkedHashMap<>();

        details.put("sessionFactoryStatus", sessionFactory.isOpen() ? "OPEN & ACTIVE" : "CLOSED");

        // Open an explicit Hibernate Session and begin an explicit Hibernate Transaction
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                // Execute a native Hibernate HQL query
                List<User> users = session.createQuery("FROM User", User.class)
                        .setMaxResults(5)
                        .list();

                details.put("sessionIsOpen", session.isOpen());
                details.put("activeTransaction", tx.isActive());
                details.put("usersRetrievedCount", users.size());
                details.put("hqlQueryExecuted", "FROM User (Max results: 5)");

                // Commit the Hibernate transaction
                tx.commit();
                details.put("transactionStatus", "COMMITTED_SUCCESSFULLY");

            } catch (Exception ex) {
                if (tx.isActive()) {
                    tx.rollback();
                    details.put("transactionStatus", "ROLLED_BACK_ON_ERROR");
                }
                throw ex;
            }
        }

        long durationNanos = System.nanoTime() - start;

        details.put("vivaExplanation", "1. SessionFactory: Heavyweight, thread-safe factory bean configured once per database.\n" +
                "2. Session: Lightweight, single-threaded connection wrapper opened per unit of work.\n" +
                "3. Transaction: Manages the atomic commit/rollback boundaries of persistence operations.\n" +
                "4. Spring vs Hibernate: Spring Data JPA's EntityManager wraps Hibernate Session under the hood, and @Transactional manages commit/rollback declaratively.");

        return new SyllabusDemoResultDto(
                "Hibernate SessionFactory, Session & Transaction Management",
                "Demonstration of explicit Session opening, HQL query execution, and Transaction commit",
                durationNanos,
                details
        );
    }

    @Override
    public SyllabusDemoResultDto demonstrateReflection() {
        long start = System.nanoTime();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("userEntityInspection", ReflectionDemoUtils.inspectClassMetadata(User.class));
        details.put("messageEntityInspection", ReflectionDemoUtils.inspectClassMetadata(Message.class));
        details.put("explanation", "Java Reflection API allows runtime inspection of classes, fields, annotations (@Entity, @Table, @Column), and modifiers without compile-time hardcoding.");

        long durationNanos = System.nanoTime() - start;

        return new SyllabusDemoResultDto(
                "Java Reflection API",
                "Runtime inspection of JPA Entity classes, annotations, and metadata",
                durationNanos,
                details
        );
    }

    @Override
    public SyllabusDemoResultDto demonstrateSerialization() {
        long start = System.nanoTime();

        // Create a sample serializable entity
        User sample = new User("demouser", "demo@connectchat.local", "hash123", "Demo User");
        sample.setId(999L);

        Map<String, Object> details = SerializationDemoUtils.demonstrateJavaSerialization(sample);

        long durationNanos = System.nanoTime() - start;

        return new SyllabusDemoResultDto(
                "Java Object Serialization",
                "Binary serialization using ObjectOutputStream and deserialization using ObjectInputStream",
                durationNanos,
                details
        );
    }

    @Override
    public SyllabusDemoResultDto demonstrateGenericsAndStreams() {
        long start = System.nanoTime();

        List<String> rawUsernames = Arrays.asList("alex", "samira", "jordan", "elena", "bob", "alice", "charlie");

        // 1. Filter using Generics & Predicate
        List<String> filtered = GenericCollectionUtils.filterList(rawUsernames, s -> s.length() > 4);

        // 2. Paginate using Generic Pagination Utility
        List<String> page1 = GenericCollectionUtils.paginateList(rawUsernames, 0, 3);
        List<String> page2 = GenericCollectionUtils.paginateList(rawUsernames, 1, 3);

        // 3. Streams: Uppercase and collect to comma-separated string
        String streamResult = rawUsernames.stream()
                .filter(s -> s.startsWith("a") || s.startsWith("e"))
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.joining(", "));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("rawList", rawUsernames);
        details.put("filteredListLengthGreaterThan4", filtered);
        details.put("genericPaginationPage1", page1);
        details.put("genericPaginationPage2", page2);
        details.put("streamProcessedFilteredSorted", streamResult);

        long durationNanos = System.nanoTime() - start;

        return new SyllabusDemoResultDto(
                "Java Generics & Stream API",
                "Generic utilities combined with functional operations (filter, map, sorted, collect)",
                durationNanos,
                details
        );
    }
}
