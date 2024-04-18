/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.embeddedid;

import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.paiondata.elide.core.utils.coerce.converters.Serde;

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
        long number = Long.parseLong(matcher.group(1));
        String street = matcher.group(2);
        long zipCode = Long.parseLong(matcher.group(3));

        return new Address(number, street, zipCode);
    }

    @Override
    public String serialize(Address val) {
        return Base64.getEncoder().encodeToString(val.toString().getBytes());
    }
}
