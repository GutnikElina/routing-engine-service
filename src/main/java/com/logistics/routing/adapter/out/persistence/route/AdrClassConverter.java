package com.logistics.routing.adapter.out.persistence.route;

import com.logistics.routing.domain.route.model.enums.AdrClass;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AdrClassConverter implements AttributeConverter<AdrClass, String> {

    @Override
    public String convertToDatabaseColumn(AdrClass attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public AdrClass convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AdrClass.fromCode(dbData);
    }
}
