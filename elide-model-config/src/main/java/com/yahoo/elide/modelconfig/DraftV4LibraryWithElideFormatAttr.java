/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.jsonformats.ElideCardinalityFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideFieldNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideFieldTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideGrainTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideJDBCUrlFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideJoinKindFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideJoinTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideRSQLFilterFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideRoleFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideTimeFieldTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameWithExtFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ValidateDimPropertiesKeyword;
import com.yahoo.elide.modelconfig.jsonformats.ValidateTimeDimPropertiesKeyword;
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
        builder.addFormatAttribute(ElideJoinKindFormatAttr.FORMAT_NAME, new ElideJoinKindFormatAttr());
        builder.addFormatAttribute(ElideTimeFieldTypeFormatAttr.FORMAT_NAME, new ElideTimeFieldTypeFormatAttr());
        builder.addFormatAttribute(ElideNameFormatAttr.FORMAT_NAME, new ElideNameFormatAttr());
        builder.addFormatAttribute(ElideRSQLFilterFormatAttr.FORMAT_NAME, new ElideRSQLFilterFormatAttr());
        builder.addFormatAttribute(JavaClassNameWithExtFormatAttr.FORMAT_NAME, new JavaClassNameWithExtFormatAttr());
        builder.addFormatAttribute(ElideJDBCUrlFormatAttr.FORMAT_NAME, new ElideJDBCUrlFormatAttr());
        builder.addFormatAttribute(JavaClassNameFormatAttr.FORMAT_NAME, new JavaClassNameFormatAttr());
        builder.addFormatAttribute(ElideRoleFormatAttr.FORMAT_NAME, new ElideRoleFormatAttr());

        builder.addKeyword(new ValidateDimPropertiesKeyword().getKeyword());
        builder.addKeyword(new ValidateTimeDimPropertiesKeyword().getKeyword());

        library = builder.freeze();
    }
}
