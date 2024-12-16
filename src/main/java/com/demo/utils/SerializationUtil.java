package com.demo.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class SerializationUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(Object object) throws IOException {
        if (object == null) {
            return null;
        }
        return objectMapper.writeValueAsString(object);
    }

    public static <T> T deserialize(String data, Class<T> clazz) throws IOException {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return objectMapper.readValue(data, clazz);
    }

}
