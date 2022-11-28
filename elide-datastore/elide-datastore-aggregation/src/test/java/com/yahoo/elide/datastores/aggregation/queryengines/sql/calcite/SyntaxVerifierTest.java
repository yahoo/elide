/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.MySQLDialect;
import org.junit.jupiter.api.Test;

public class SyntaxVerifierTest {

    @Test
    public void testValidVerification() {
        SyntaxVerifier verifier = new SyntaxVerifier(new MySQLDialect());

        assertTrue(verifier.verify("CAST(`2017-08-29` AS DATE)"));
    }

    @Test
    public void testInvalidOperator() {
        SyntaxVerifier verifier = new SyntaxVerifier(new MySQLDialect());

        assertFalse(verifier.verify("CAST(`2017-08-29` AS DATE) + FOOBAR(123)"));
        assertEquals("Unknown operator: FOOBAR", verifier.getLastError());
    }

    @Test
    public void testInvalidSyntax() {
        SyntaxVerifier verifier = new SyntaxVerifier(new MySQLDialect());

        assertFalse(verifier.verify("A /+/ B"));
        assertTrue(verifier.getLastError().startsWith("Encountered"));
    }
}
