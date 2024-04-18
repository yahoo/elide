/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.paiondata.elide.annotation.Include;

import example.models.jpa.usertypes.JsonType;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


@Include(name = "export")
@Entity
@Data
public class Export {
    @Id
    private String name = "";

    @Convert(converter = CommaDelimitedStringConverter.class)
    private Set<String> alternatives;

    @Type(value = JsonType.class, parameters = { @Parameter(name = "class", value = "example.models.jpa.AddressFragment") })
    private AddressFragment address;

    public static class CommaDelimitedStringConverter implements AttributeConverter<Set<String>, String> {

        @Override
        public String convertToDatabaseColumn(Set<String> attribute) {
            if (attribute == null) {
                return null;
            }
            return String.join(",", attribute);
        }

        @Override
        public Set<String> convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            Set<String> result = new LinkedHashSet<>();
            Collections.addAll(result, dbData.split(","));
            return result;
        }
    }
}
