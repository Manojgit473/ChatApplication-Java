# Connect Chat — College Viva Examination & Technical Interview Notes

> Quick reference guide for explaining architecture, Spring concepts, Hibernate ORM, WebSocket, security, and Core Java design patterns during a project viva or technical defense.

---

## 1. High-Frequency Viva Questions & Concise Answers

### Q1: What is the architectural flow of Connect Chat?
**Answer**:
Connect Chat implements a clean, layered Enterprise MVC architecture:
`Client Browser (HTML/CSS/JS + STOMP) -> Spring Controller -> Service Layer (@Transactional) -> Repository (Spring Data JPA) -> Hibernate ORM -> MySQL Database`.
- **Controllers** handle HTTP requests or STOMP frames and return DTO responses.
- **Services** encapsulate business rules, authorization checks, and transaction boundaries.
- **Repositories** define query methods and parameter-bound JPQL statements.
- **Entities** represent persisted relational tables with JPA annotations.
- **Mappers & DTOs** decouple database representations from external API contracts to prevent N+1 queries and sensitive data leaks.

---

### Q2: How does Spring Dependency Injection (DI) work in this project?
**Answer**:
Dependency Injection is an implementation of Inversion of Control (IoC). Instead of creating dependencies manually using `new ServiceImpl()`, Spring's `ApplicationContext` scans classes annotated with `@Component`, `@Service`, `@Repository`, and `@Controller`, instantiates them as singletons, and injects them into consuming classes.
Connect Chat strictly uses **Constructor Injection** (e.g., in `ConversationServiceImpl` and `AuthServiceImpl`).
*Benefits*:
1. Immutability: Dependencies can be declared `private final`.
2. Testability: Easy to pass Mockito mock objects during unit testing without Spring context overhead.
3. Fails at startup if a required bean is missing.

---

### Q3: Explain the difference between Hibernate `SessionFactory`, `Session`, and `Transaction`.
**Answer**:
- **`SessionFactory`**: A heavyweight, thread-safe factory bean created once at application startup. It compiles mapping metadata and manages second-level caching and JDBC connection pooling.
- **`Session`**: A lightweight, non-thread-safe connection wrapper representing a single conversational unit of work with the database. It maintains the First-Level Cache (Persistence Context) and tracks dirty entity states.
- **`Transaction`**: Demarcates the physical database transaction boundary (`beginTransaction()`, `commit()`, `rollback()`), ensuring atomicity and isolation.
- **Spring Integration**: In standard Spring Boot, JPA's `EntityManager` wraps the Hibernate `Session`, and Spring's `@Transactional` manages commit/rollback declaratively. Connect Chat provides an isolated educational demonstration in `SyllabusDemoServiceImpl` where `SessionFactory.openSession()`, `session.beginTransaction()`, HQL query execution, and `tx.commit()` are executed explicitly.

---

### Q4: How does Connect Chat achieve Real-Time Messaging? Why WebSocket over HTTP polling?
**Answer**:
Traditional HTTP is a half-duplex request-response protocol; client polling causes high latency and wasted server resources.
Connect Chat uses **WebSocket with STOMP**:
1. The client performs an initial HTTP handshake on `/ws`.
2. Once upgraded to full-duplex WebSocket, STOMP frames flow bidirectionally with minimal header overhead.
3. The server runs an in-memory STOMP broker routing messages to destination topics (`/topic/conversations/{id}`, `/topic/presence`).
4. `SimpMessagingTemplate` broadcasts new messages, typing indicators, and read receipts instantly to connected recipients.

---

### Q5: How is WebSocket communication secured?
**Answer**:
1. **Handshake Authentication**: The WebSocket handshake inherits the authenticated Spring Security `Principal` from the HTTP session cookie (`JSESSIONID`).
2. **Channel Interceptor (`WebSocketAuthChannelInterceptor`)**: Every STOMP `SUBSCRIBE` frame is intercepted. When a user tries to subscribe to `/topic/conversations/{id}`, the interceptor verifies against `ConversationMemberRepository` that the user belongs to that conversation. Non-members receive an `AccessDeniedException`.
3. **Payload Zero-Trust**: Incoming message frames derive the sender identity from `principal.getName()` rather than trusting client-provided JSON sender IDs.

---

### Q6: How are passwords secured in the database?
**Answer**:
Passwords are never stored in plain text. Connect Chat uses **BCrypt password hashing** (`BCryptPasswordEncoder(10)`) via Spring Security.
- BCrypt incorporates an automatically generated salt and 10 rounds of key expansion (work factor).
- Even if two users choose identical passwords, their BCrypt hashes are completely distinct.
- BCrypt is computationally expensive, mitigating brute-force and rainbow table attacks.

---

### Q7: What is Spring AOP and how is it used in this project?
**Answer**:
Aspect-Oriented Programming (AOP) modularizes cross-cutting concerns that span multiple classes.
Connect Chat demonstrates two distinct aspects:
1. `LoggingAspect`: Uses `@Around("execution(* com.connectchat.service.impl.*.*(..))")` to calculate service execution latency without modifying business logic.
2. `AuditLoggingAspect`: Uses `@AfterReturning` advice to write non-sensitive security audit entries for critical business actions (e.g. user registration, group creation, message deletion, user blocking).

---

### Q8: How does the application prevent duplicate messages during network retries?
**Answer**:
Connect Chat implements **Idempotency Keys**:
1. When sending a message, the client generates a unique UUID `idempotencyKey`.
2. Before inserting, `MessageService` checks if a message with that `conversation_id` and `idempotency_key` already exists.
3. If found (e.g., due to a temporary network timeout on client acknowledgment), the existing message is returned without creating a duplicate record in MySQL.

---

### Q9: What Core Java Unit II syllabus concepts are integrated into Connect Chat?
**Answer**:
1. **Collections & Generics**: `GenericCollectionUtils.java` demonstrates generic methods (`<T> List<T> paginateList(...)`), bounded wildcards (`Predicate<? super T>`), and map aggregations.
2. **Stream API**: Filtering active contacts, mapping entities to DTOs, sorting message histories.
3. **Java Time (`Instant`)**: Storing UTC timestamps consistently and formatting localized transcripts with `DateTimeFormatter`.
4. **File I/O (`java.nio.file`)**: Secure attachment streaming and file-type whitelist validation in `AttachmentServiceImpl`.
5. **Object Serialization**: `SerializationDemoUtils.java` shows native binary serialization with `ObjectOutputStream` and `ObjectInputStream` alongside modern Jackson JSON serialization.
6. **Java Reflection API**: `ReflectionDemoUtils.java` inspects JPA annotations (`@Entity`, `@Table`, `@Column`, `@Id`) and fields at runtime.
7. **Regular Expressions**: `StringValidationUtils.java` validates usernames and email addresses using precompiled `Pattern` instances.

---

### Q10: Why is Connect Chat a Modular Monolith instead of Microservices?
**Answer**:
A modular monolith is the recommended starting architecture for college projects and modern software systems:
- Avoids distributed transaction issues (two-phase commit / saga pattern).
- Eliminates network serialization latency between services.
- Simplifies local deployment and viva demonstrations (single executable JAR).
- Demonstrates clear separation of boundaries so that User, Message, and Notification modules can be cleanly extracted into independent Spring Boot microservices with Spring Cloud Gateway and Eureka in the future.

---

## 2. Key Annotations Cheat Sheet

| Annotation | Location | Meaning for Viva |
|---|---|---|
| `@SpringBootApplication` | `ConnectChatApplication` | Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. |
| `@EnableAspectJAutoProxy` | `ConnectChatApplication` | Enables Spring AOP proxy generation for `@Aspect` classes. |
| `@Entity` & `@Table` | Entity classes | Marks a Java POJO as a JPA persisted relational table. |
| `@MappedSuperclass` | `BaseEntity` | Designates an inheritance parent class whose fields are mapped into child tables. |
| `@Id` & `@GeneratedValue` | `BaseEntity` | Declares primary key with MySQL `AUTO_INCREMENT` (`IDENTITY`) strategy. |
| `@Transactional` | Service implementations | Manages atomic database transaction boundaries and rollbacks on runtime exceptions. |
| `@RestController` | API Controllers | Combines `@Controller` and `@ResponseBody` to serialize returned Java objects to JSON. |
| `@Valid` | Controller methods | Instructs Spring to validate incoming request DTOs against Jakarta Bean Validation constraints. |
| `@MessageMapping` | WebSocket Controller | Routes STOMP message frames sent to `/app/*` destinations to Java handler methods. |
| `@Aspect` & `@Around` | AOP Aspects | Defines a cross-cutting module and wraps around method invocation to measure performance. |

