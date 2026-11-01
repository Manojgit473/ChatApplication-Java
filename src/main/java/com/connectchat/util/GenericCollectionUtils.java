package com.connectchat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Reusable Core Java Generics and Collections utility.
 * 
 * Demonstrates:
 * - Generic type parameters (<T, R, K, V>).
 * - Upper bounded and unbounded wildcards.
 * - Integration with Java 8+ Stream API (map, filter, collect).
 */
public final class GenericCollectionUtils {

    private GenericCollectionUtils() {
    }

    /**
     * Paginates any in-memory Java Collection using generics.
     */
    public static <T> List<T> paginateList(List<T> source, int page, int size) {
        if (source == null || source.isEmpty() || page < 0 || size <= 0) {
            return Collections.emptyList();
        }

        int fromIndex = page * size;
        if (fromIndex >= source.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + size, source.size());
        return new ArrayList<>(source.subList(fromIndex, toIndex));
    }

    /**
     * Filters a list based on a generic Predicate.
     */
    public static <T> List<T> filterList(List<T> list, Predicate<? super T> predicate) {
        if (list == null || predicate == null) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of elements into a lookup Map keyed by an extracted attribute.
     */
    public static <K, V> Map<K, V> toLookupMap(List<V> list, Function<? super V, ? extends K> keyExtractor) {
        if (list == null || keyExtractor == null) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.toMap(
                        keyExtractor,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }
}

