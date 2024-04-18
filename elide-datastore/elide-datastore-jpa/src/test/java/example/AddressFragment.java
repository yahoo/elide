/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.datastores.jpa.usertypes.JsonConverter;

public class AddressFragment {
    public String street;
    public String state;
    public ZipCode zip;

    public static class ZipCode {
        public String zip;
        public String plusFour;
    }

    public static class Converter extends JsonConverter<AddressFragment> {
        public Converter() {
            super(AddressFragment.class);
        }
    }
}
