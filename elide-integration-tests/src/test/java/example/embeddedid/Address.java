/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.embeddedid;

import lombok.Data;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Base64;

@Embeddable
@Data
public class Address implements Serializable {
    private long number;
    private String street;
    private long zipCode;
}
