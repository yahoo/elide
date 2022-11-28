/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonDeserialize(using = Price.PriceDeserializer.class)
public class Price {
    private BigDecimal units;
    private Currency currency;

    public static class PriceDeserializer extends StdDeserializer<Price> {
        public PriceDeserializer() {
            super(Price.class);
        }


        @Override
        public Price deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            if (node == null) {
                return null;
            }

            JsonNode unitNode = node.get("units");

            if (unitNode == null) {
                return null;
            }

            JsonNode currencyNode = node.get("currency");

            if (currencyNode == null) {
                return new Price(new BigDecimal(unitNode.asDouble()), null);
            }

            Currency currency = Currency.getInstance(node.get("currency").get("currencyCode").asText(""));

            return new Price(new BigDecimal(unitNode.asDouble()), currency);
        }
    }
}
