/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurity;

import org.junit.jupiter.api.Test;

/**
 * ElideSecurityToPojo test.
 */
public class ElideSecurityToPojoTest {

    private ElideSecurityToPojo testClass = new ElideSecurityToPojo();
    private static final String VALID_SECURITY = "{\n"
                    + "    roles : [\n"
                    + "        admin, \n"
                    + "        guest, \n"
                    + "        member\n"
                    + "        ]\n"
                    + "    rules: [\n"
                    + "        {\n"
                    + "            type: filter //type is optional.\n"
                    + "            filter: company_id=in=${principal.companies}\n"
                    + "            name: User belongs to company\n"
                    + "        },\n"
                    + "        {\n"
                    + "            filter: id==${principal.id}\n"
                    + "            name: Principal is owner\n"
                    + "        },\n"
                    + "    ]\n"
                    + "}";

    @Test
    public void testValidSecurity() throws Exception {
        ElideSecurity sec = testClass.parseSecurityConfig(VALID_SECURITY);
        assertEquals(3, sec.getRoles().size());
    }

    @Test
    public void testInValidSecurity() throws Exception {
        assertNull(testClass.parseSecurityConfig(""));
    }
}
