package com.discordBot.demo.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<Set<String>, String> {

    private static final String SPLIT_CHAR = ",";

    // Java Set -> DB String으로 변환
    @Override
    public String convertToDatabaseColumn(Set<String> stringSet) {
        if (stringSet == null || stringSet.isEmpty()) {
            return null;
        }
        return String.join(SPLIT_CHAR, stringSet);
    }

    // DB String -> Java Set으로 변환
    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(dbData.split(SPLIT_CHAR))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}