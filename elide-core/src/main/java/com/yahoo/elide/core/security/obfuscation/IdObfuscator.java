/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.obfuscation;

import com.yahoo.elide.core.type.Type;

/**
 * Obfuscator that obfuscates the identifier of the entity which is the primary
 * key.
 * <p>
 * This is intended to be used if the primary key undesirably leaks information
 * about the record.
 * <p>
 * The following are things to consider when deciding to obfuscate the id.
 * <ul>
 * <li>Predictability of the ID</li>
 * <li>Leaks the count of items</li>
 * <li>Leaks information about the machine/process</li>
 * <li>Leaks the date of creation</li>
 * </ul>
 * <p>
 * Note that the sorting will be based on the original value prior to
 * obfuscation.
 * <p>
 * Alternatively a separate field or property can be used to represent the
 * entity identifier using {@link com.yahoo.elide.annotation.EntityId}.
 */
public interface IdObfuscator {
    /**
     * Obfuscates the id.
     *
     * @param id the id to obfuscate
     * @return the obfuscated id
     */
    String obfuscate(Object id);

    /**
     * Deobfuscates the obfuscated id string.
     * @param obfuscatedId the obfuscated id
     * @param the id type
     *
     * @return the deobfuscated id
     */
    <T> T deobfuscate(String obfuscatedId, Type<?> type);
}
