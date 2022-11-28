/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.jsonformats.ElideArgumentNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideCardinalityFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideFieldNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideFieldTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideGrainTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideJDBCUrlFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideJoinKindFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideJoinTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideNamespaceNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideRSQLFilterFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideRoleFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ElideTimeFieldTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameWithExtFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ValidateArgsPropertiesKeyword;
import com.yahoo.elide.modelconfig.jsonformats.ValidateDimPropertiesKeyword;
import com.yahoo.elide.modelconfig.jsonformats.ValidateTimeDimPropertiesKeyword;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.jsonschema.library.Library;

import lombok.Getter;

/**
 * Augment the {@link DraftV4Library} with custom format attributes and keywords.
 */
public class DraftV4LibraryWithElideFormatAttr {
    @Getter
    private Library library;

    public DraftV4LibraryWithElideFormatAttr() {
        library = DraftV4Library.get().thaw()
                        .addFormatAttribute(ElideFieldNameFormatAttr.FORMAT_NAME, new ElideFieldNameFormatAttr())
                        .addFormatAttribute(ElideArgumentNameFormatAttr.FORMAT_NAME, new ElideArgumentNameFormatAttr())
                        .addFormatAttribute(ElideCardinalityFormatAttr.FORMAT_NAME, new ElideCardinalityFormatAttr())
                        .addFormatAttribute(ElideFieldTypeFormatAttr.FORMAT_NAME, new ElideFieldTypeFormatAttr())
                        .addFormatAttribute(ElideGrainTypeFormatAttr.FORMAT_NAME, new ElideGrainTypeFormatAttr())
                        .addFormatAttribute(ElideJoinTypeFormatAttr.FORMAT_NAME, new ElideJoinTypeFormatAttr())
                        .addFormatAttribute(ElideJoinKindFormatAttr.FORMAT_NAME, new ElideJoinKindFormatAttr())
                        .addFormatAttribute(ElideTimeFieldTypeFormatAttr.FORMAT_NAME,
                                        new ElideTimeFieldTypeFormatAttr())
                        .addFormatAttribute(ElideNameFormatAttr.FORMAT_NAME, new ElideNameFormatAttr())
                        .addFormatAttribute(ElideNamespaceNameFormatAttr.FORMAT_NAME,
                                        new ElideNamespaceNameFormatAttr())
                        .addFormatAttribute(ElideRSQLFilterFormatAttr.FORMAT_NAME, new ElideRSQLFilterFormatAttr())
                        .addFormatAttribute(JavaClassNameWithExtFormatAttr.FORMAT_NAME,
                                        new JavaClassNameWithExtFormatAttr())
                        .addFormatAttribute(ElideJDBCUrlFormatAttr.FORMAT_NAME, new ElideJDBCUrlFormatAttr())
                        .addFormatAttribute(JavaClassNameFormatAttr.FORMAT_NAME, new JavaClassNameFormatAttr())
                        .addFormatAttribute(ElideRoleFormatAttr.FORMAT_NAME, new ElideRoleFormatAttr())
                        .addKeyword(new ValidateDimPropertiesKeyword().getKeyword())
                        .addKeyword(new ValidateTimeDimPropertiesKeyword().getKeyword())
                        .addKeyword(new ValidateArgsPropertiesKeyword().getKeyword())
                        .freeze();
    }
}
