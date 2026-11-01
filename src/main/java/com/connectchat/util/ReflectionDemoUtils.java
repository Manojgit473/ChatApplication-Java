package com.connectchat.util;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Educational utility demonstrating Java Reflection API.
 * 
 * Demonstrates:
 * - Runtime class inspection (Class<?>).
 * - Inspecting class-level and field-level JPA annotations (@Entity, @Table, @Column, @Id).
 * - Accessing declared fields, modifier masks, and field types.
 */
public final class ReflectionDemoUtils {

    private ReflectionDemoUtils() {
    }

    public static Map<String, Object> inspectClassMetadata(Class<?> clazz) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("className", clazz.getName());
        metadata.put("simpleName", clazz.getSimpleName());
        metadata.put("package", clazz.getPackageName());
        metadata.put("isEntity", clazz.isAnnotationPresent(Entity.class));

        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            metadata.put("tableName", table.name());
        }

        List<Map<String, Object>> fieldsInfo = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue; // Skip static fields such as serialVersionUID
                }

                Map<String, Object> fieldData = new LinkedHashMap<>();
                fieldData.put("name", field.getName());
                fieldData.put("type", field.getType().getSimpleName());
                fieldData.put("isId", field.isAnnotationPresent(Id.class));

                if (field.isAnnotationPresent(Column.class)) {
                    Column col = field.getAnnotation(Column.class);
                    fieldData.put("columnName", col.name().isEmpty() ? field.getName() : col.name());
                    fieldData.put("nullable", col.nullable());
                }

                fieldsInfo.add(fieldData);
            }
            current = current.getSuperclass();
        }

        metadata.put("fields", fieldsInfo);
        return metadata;
    }
}

