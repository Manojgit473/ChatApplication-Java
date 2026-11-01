package com.connectchat.util;

import com.connectchat.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreJavaUtilsTest {

    @Test
    void testGenericPagination() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> page0 = GenericCollectionUtils.paginateList(numbers, 0, 4);
        assertEquals(4, page0.size());
        assertEquals(Arrays.asList(1, 2, 3, 4), page0);

        List<Integer> page2 = GenericCollectionUtils.paginateList(numbers, 2, 4);
        assertEquals(2, page2.size());
        assertEquals(Arrays.asList(9, 10), page2);

        List<Integer> emptyPage = GenericCollectionUtils.paginateList(numbers, 5, 4);
        assertTrue(emptyPage.isEmpty());
    }

    @Test
    void testGenericFiltering() {
        List<String> names = Arrays.asList("Spring", "Hibernate", "Java", "WebSocket", "AOP");
        List<String> filtered = GenericCollectionUtils.filterList(names, s -> s.length() <= 4);

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains("Java"));
        assertTrue(filtered.contains("AOP"));
    }

    @Test
    void testStringValidationRegex() {
        assertTrue(StringValidationUtils.isValidUsername("valid_user.123"));
        assertFalse(StringValidationUtils.isValidUsername("in")); // too short (<3)
        assertFalse(StringValidationUtils.isValidUsername("invalid user with spaces"));

        assertTrue(StringValidationUtils.isValidEmail("alex@example.com"));
        assertFalse(StringValidationUtils.isValidEmail("invalid-email-address"));
    }

    @Test
    void testHtmlEscaping() {
        String unsafe = "<script>alert('XSS & danger')</script>";
        String safe = StringValidationUtils.escapeHtml(unsafe);

        assertFalse(safe.contains("<script>"));
        assertTrue(safe.contains("&lt;script&gt;"));
        assertTrue(safe.contains("&amp;"));
    }

    @Test
    void testReflectionMetadataInspection() {
        Map<String, Object> metadata = ReflectionDemoUtils.inspectClassMetadata(User.class);

        assertNotNull(metadata);
        assertEquals("User", metadata.get("simpleName"));
        assertEquals(true, metadata.get("isEntity"));
        assertEquals("users", metadata.get("tableName"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) metadata.get("fields");
        assertNotNull(fields);
        assertTrue(fields.stream().anyMatch(f -> "username".equals(f.get("name"))));
    }

    @Test
    void testJavaObjectSerialization() {
        User user = new User("serialtest", "serial@test.com", "hash", "Serial User");
        user.setId(777L);

        Map<String, Object> result = SerializationDemoUtils.demonstrateJavaSerialization(user);
        assertNotNull(result);
        assertEquals(true, result.get("objectsEqual"));
        assertNotNull(result.get("base64EncodedBytes"));
    }
}

