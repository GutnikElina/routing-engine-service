package com.logistics.routing.adapter.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AdrClassConverter implements AttributeConverter<AdrClass, String> {

    @Override
    public String convertToDatabaseColumn(AdrClass attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AdrClass convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AdrClass.fromCode(dbData);
    }
}