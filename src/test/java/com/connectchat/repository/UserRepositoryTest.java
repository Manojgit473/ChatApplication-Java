package com.connectchat.repository;

import com.connectchat.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByUsername() {
        User user = new User("demotester", "tester@connectchat.local", "secureHash123", "Demo Tester");
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("demotester");
        assertTrue(found.isPresent());
        assertEquals("Demo Tester", found.get().getDisplayName());
    }

    @Test
    void testSearchUsersJPQL() {
        User u1 = new User("clara", "clara@test.com", "hash", "Clara Oswald");
        User u2 = new User("amy", "amy@test.com", "hash", "Amy Pond");
        userRepository.save(u1);
        userRepository.save(u2);

        List<User> results = userRepository.searchUsers("Clara", 999L);
        assertEquals(1, results.size());
        assertEquals("clara", results.get(0).getUsername());
    }
}

