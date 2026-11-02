package com.connectchat.config;

import com.connectchat.entity.Conversation;
import com.connectchat.entity.ConversationMember;
import com.connectchat.entity.Message;
import com.connectchat.entity.User;
import com.connectchat.enums.GroupRole;
import com.connectchat.enums.ConversationType;
import com.connectchat.enums.MessageStatus;
import com.connectchat.enums.MessageType;
import com.connectchat.enums.OnlineStatus;
import com.connectchat.repository.ConversationRepository;
import com.connectchat.repository.MessageRepository;
import com.connectchat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * DataInitializer - Seeds demonstration accounts and conversations on initial startup.
 * 
 * Demonstrates:
 * - Spring Boot ApplicationRunner lifecycle hook.
 * - Database seeding via JPA repositories and declarative transaction management.
 * - BCrypt password encoding for seeded demonstration accounts.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users. Skipping initial demo seed.");
            return;
        }

        log.info("Seeding initial demonstration users and conversations...");

        String encodedPassword = passwordEncoder.encode("password123");

        User alex = new User("alex", "alex@connectchat.local", encodedPassword, "Alex Morgan");
        alex.setPhoneNumber("+91 9876543210");
        alex.setBio("Full-Stack Engineer & Open Source enthusiast");
        alex.setOnlineStatus(OnlineStatus.ONLINE);

        User samira = new User("samira", "samira@connectchat.local", encodedPassword, "Samira Khan");
        samira.setPhoneNumber("+91 9876543211");
        samira.setBio("UI/UX Designer | Clean interfaces & soft palettes");
        samira.setOnlineStatus(OnlineStatus.ONLINE);

        User jordan = new User("jordan", "jordan@connectchat.local", encodedPassword, "Jordan Lee");
        jordan.setPhoneNumber("+91 9876543212");
        jordan.setBio("Backend Developer & Distributed Systems researcher");
        jordan.setOnlineStatus(OnlineStatus.OFFLINE);

        User elena = new User("elena", "elena@connectchat.local", encodedPassword, "Elena Rostova");
        elena.setPhoneNumber("+91 9876543213");
        elena.setBio("DevOps & Cloud architecture apprentice");
        elena.setOnlineStatus(OnlineStatus.OFFLINE);

        userRepository.saveAll(List.of(alex, samira, jordan, elena));

        // Create Direct Conversation between Alex and Samira
        Conversation direct = new Conversation();
        direct.setType(ConversationType.DIRECT);
        direct.setCreatedBy(alex);
        direct.setLastMessageAt(Instant.now());

        ConversationMember m1 = new ConversationMember(direct, alex, GroupRole.MEMBER);
        ConversationMember m2 = new ConversationMember(direct, samira, GroupRole.MEMBER);
        direct.getMembers().add(m1);
        direct.getMembers().add(m2);

        conversationRepository.save(direct);

        Message dm1 = new Message(direct, alex, "Hey Samira! Welcome to Connect Chat.", MessageType.TEXT);
        dm1.setStatus(MessageStatus.READ);
        messageRepository.save(dm1);

        Message dm2 = new Message(direct, samira, "Hi Alex! The soft teal interface looks beautiful and responsive.", MessageType.TEXT);
        dm2.setStatus(MessageStatus.READ);
        messageRepository.save(dm2);

        // Create Group Conversation
        Conversation group = new Conversation();
        group.setType(ConversationType.GROUP);
        group.setTitle("Project Team Workspace");
        group.setDescription("Connect Chat development and architecture coordination");
        group.setCreatedBy(alex);
        group.setLastMessageAt(Instant.now());

        ConversationMember gm1 = new ConversationMember(group, alex, GroupRole.ADMIN);
        ConversationMember gm2 = new ConversationMember(group, samira, GroupRole.MEMBER);
        ConversationMember gm3 = new ConversationMember(group, jordan, GroupRole.MEMBER);
        ConversationMember gm4 = new ConversationMember(group, elena, GroupRole.MEMBER);

        group.getMembers().addAll(List.of(gm1, gm2, gm3, gm4));
        conversationRepository.save(group);

        Message gmMsg1 = new Message(group, alex, "Welcome team! All Spring Boot 3 + Hibernate 6 tests passed successfully.", MessageType.TEXT);
        gmMsg1.setStatus(MessageStatus.SENT);
        messageRepository.save(gmMsg1);

        log.info("Database seeding complete. Seeded demo users: alex, samira, jordan, elena (password: password123).");
    }
}

