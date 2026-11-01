package com.connectchat.util;

import java.io.*;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Educational utility demonstrating Core Java Object Serialization.
 * 
 * Demonstrates:
 * - java.io.Serializable interface and serialVersionUID.
 * - ObjectOutputStream and ByteArrayOutputStream to serialize an object into bytes.
 * - ObjectInputStream and ByteArrayInputStream to deserialize bytes back to an object.
 * - Explains differences between binary Java serialization and JSON serialization.
 */
public final class SerializationDemoUtils {

    private SerializationDemoUtils() {
    }

    public static Map<String, Object> demonstrateJavaSerialization(Serializable sampleObject) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 1. Serialize to binary byte array
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(byteStream)) {
                out.writeObject(sampleObject);
                out.flush();
            }

            byte[] serializedBytes = byteStream.toByteArray();
            String base64Encoded = Base64.getEncoder().encodeToString(serializedBytes);

            result.put("originalObjectType", sampleObject.getClass().getName());
            result.put("binaryLengthBytes", serializedBytes.length);
            result.put("base64EncodedBytes", base64Encoded);

            // 2. Deserialize back from binary byte array
            ByteArrayInputStream inStream = new ByteArrayInputStream(serializedBytes);
            Object deserializedObject;
            try (ObjectInputStream in = new ObjectInputStream(inStream)) {
                deserializedObject = in.readObject();
            }

            result.put("deserializedObjectType", deserializedObject.getClass().getName());
            result.put("objectsEqual", sampleObject.equals(deserializedObject));
            result.put("explanation", "Demonstrates native Java Serialization using ObjectOutputStream and ObjectInputStream. In modern REST microservices, Jackson JSON serialization is preferred for language-agnostic interoperability.");

            return result;

        } catch (IOException | ClassNotFoundException e) {
            result.put("error", e.getMessage());
            return result;
        }
    }
}
