/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideCardinalityFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideClassNameFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideFieldNameFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideFieldTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideGrainTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideJoinTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideNameFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideRSQLFilterFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ElideTimeFieldTypeFormatAttr;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ValidateDimPropertiesKeyword;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ValidateTimeDimPropertiesKeyword;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.library.LibraryBuilder;

import lombok.Getter;

/**
 * Augment the {@link DraftV4Library} with custom format attributes and keywords.
 */
public class DraftV4LibraryWithElideFormatAttr {
    @Getter
    private Library library;

    public DraftV4LibraryWithElideFormatAttr() {
        LibraryBuilder builder = DraftV4Library.get().thaw();

        builder.addFormatAttribute(ElideFieldNameFormatAttr.FORMAT_NAME, new ElideFieldNameFormatAttr());
        builder.addFormatAttribute(ElideCardinalityFormatAttr.FORMAT_NAME, new ElideCardinalityFormatAttr());
        builder.addFormatAttribute(ElideFieldTypeFormatAttr.FORMAT_NAME, new ElideFieldTypeFormatAttr());
        builder.addFormatAttribute(ElideGrainTypeFormatAttr.FORMAT_NAME, new ElideGrainTypeFormatAttr());
        builder.addFormatAttribute(ElideJoinTypeFormatAttr.FORMAT_NAME, new ElideJoinTypeFormatAttr());
        builder.addFormatAttribute(ElideTimeFieldTypeFormatAttr.FORMAT_NAME, new ElideTimeFieldTypeFormatAttr());
        builder.addFormatAttribute(ElideNameFormatAttr.FORMAT_NAME, new ElideNameFormatAttr());
        builder.addFormatAttribute(ElideRSQLFilterFormatAttr.FORMAT_NAME, new ElideRSQLFilterFormatAttr());
        builder.addFormatAttribute(ElideClassNameFormatAttr.FORMAT_NAME, new ElideClassNameFormatAttr());

        builder.addKeyword(new ValidateDimPropertiesKeyword().getKeyword());
        builder.addKeyword(new ValidateTimeDimPropertiesKeyword().getKeyword());

        library = builder.freeze();
    }
}
