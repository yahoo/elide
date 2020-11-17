/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideCardinalityFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideFieldNameFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideFieldTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideGrainTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideJoinTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideNameFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideTimeFieldTypeFormatAttr;

import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.library.LibraryBuilder;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class DraftV4LibraryWithElideFormatAttr {
    private static Library LIBRARY = null;

    public static Library getInstance() {
        if (LIBRARY == null) {
            LibraryBuilder builder = DraftV4Library.get().thaw();

            builder.addFormatAttribute(ElideFieldNameFormatAttr.FORMAT_NAME,
                            ElideFieldNameFormatAttr.getInstance());
            builder.addFormatAttribute(ElideCardinalityFormatAttr.FORMAT_NAME,
                            ElideCardinalityFormatAttr.getInstance());
            builder.addFormatAttribute(ElideFieldTypeFormatAttr.FORMAT_NAME,
                            ElideFieldTypeFormatAttr.getInstance());
            builder.addFormatAttribute(ElideGrainTypeFormatAttr.FORMAT_NAME,
                            ElideGrainTypeFormatAttr.getInstance());
            builder.addFormatAttribute(ElideJoinTypeFormatAttr.FORMAT_NAME,
                            ElideJoinTypeFormatAttr.getInstance());
            builder.addFormatAttribute(ElideTimeFieldTypeFormatAttr.FORMAT_NAME,
                            ElideTimeFieldTypeFormatAttr.getInstance());
            builder.addFormatAttribute(ElideNameFormatAttr.FORMAT_NAME,
                            ElideNameFormatAttr.getInstance());

            LIBRARY = builder.freeze();
        }

        return LIBRARY;
    }
}
