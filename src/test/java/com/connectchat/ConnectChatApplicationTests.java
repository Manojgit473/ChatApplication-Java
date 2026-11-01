package com.connectchat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic ApplicationContext integration test verifying Spring context bootstrapping.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ConnectChatApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring ApplicationContext and all Bean definitions load without conflict
    }
}

