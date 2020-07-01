/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.embeddedid;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ElideTypeConverter(type = Address.class, name = "Address")
public class AddressSerde implements Serde<String, Address> {
    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("Address\\(number=(\\d+), street=([a-zA-Z0-9 ]+), zipCode=(\\d+)\\)");

    @Override
    public Address deserialize(String val) {
        byte[] decodedBytes = Base64.getDecoder().decode(val);
        String decodedString = new String(decodedBytes);

        Matcher matcher = ADDRESS_PATTERN.matcher(decodedString);
        if (! matcher.matches()) {
            throw new InvalidValueException(decodedString);
        }
        long number = Long.valueOf(matcher.group(1));
        String street = matcher.group(2);
        long zipCode = Long.valueOf(matcher.group(3));

        Address address = new Address(number, street, zipCode);

        return address;
    }

    @Override
    public String serialize(Address val) {
        return Base64.getEncoder().encodeToString(val.toString().getBytes());
    }
}
