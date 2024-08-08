/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the field or property that represents the identity of the entity.
 * <p>
 * This is intended to be used if the primary key undesirably leaks information
 * about the record and to designate another field or property as the entity
 * identifier.
 * <p>
 * Another option instead of using another field or column as the entity
 * identifier is to obfuscate the id directly.
 * <p>
 * The following are some things to consider when choosing the entity id.
 * <ul>
 * <li>Opaque token</li>
 * <li>Cryptographically secure</li>
 * <li>Implementation only based on Randomness</li>
 * <li>Predictability of the ID</li>
 * <li>Leaks the count of items</li>
 * <li>Leaks information about the machine/process</li>
 * <li>Leaks the date of creation</li>
 * </ul>
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface EntityId {
}
