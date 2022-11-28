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
import com.yahoo.elide.modelconfig.jsonformats.ValidateArgsPropertiesValidator;
import com.yahoo.elide.modelconfig.jsonformats.ValidateDimPropertiesValidator;
import com.yahoo.elide.modelconfig.jsonformats.ValidateTimeDimPropertiesValidator;
import com.github.fge.jsonschema.messages.JsonSchemaValidationBundle;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.github.fge.msgsimple.source.MapMessageSource;

import lombok.Getter;

/**
 * Augment the {@link MessageBundle} with error messages for custom format attributes and keywords.
 */
public class MessageBundleWithElideMessages {
    @Getter
    private MessageBundle msgBundle;

    public MessageBundleWithElideMessages() {
        this.msgBundle = MessageBundles.getBundle(JsonSchemaValidationBundle.class).thaw()
                        .appendSource(MapMessageSource.newBuilder()

                                        // For Format errors
                                        .put(ElideFieldNameFormatAttr.FORMAT_KEY, ElideFieldNameFormatAttr.FORMAT_MSG)
                                        .put(ElideFieldNameFormatAttr.NAME_KEY, ElideFieldNameFormatAttr.NAME_MSG)
                                        .put(ElideArgumentNameFormatAttr.FORMAT_KEY,
                                                        ElideArgumentNameFormatAttr.FORMAT_MSG)
                                        .put(ElideArgumentNameFormatAttr.NAME_KEY, ElideArgumentNameFormatAttr.NAME_MSG)
                                        .put(ElideCardinalityFormatAttr.TYPE_KEY, ElideCardinalityFormatAttr.TYPE_MSG)
                                        .put(ElideFieldTypeFormatAttr.TYPE_KEY, ElideFieldTypeFormatAttr.TYPE_MSG)
                                        .put(ElideGrainTypeFormatAttr.TYPE_KEY, ElideGrainTypeFormatAttr.TYPE_MSG)
                                        .put(ElideJoinTypeFormatAttr.TYPE_KEY, ElideJoinTypeFormatAttr.TYPE_MSG)
                                        .put(ElideJoinKindFormatAttr.TYPE_KEY, ElideJoinKindFormatAttr.TYPE_MSG)
                                        .put(ElideTimeFieldTypeFormatAttr.TYPE_KEY,
                                                        ElideTimeFieldTypeFormatAttr.TYPE_MSG)
                                        .put(ElideNameFormatAttr.FORMAT_KEY, ElideNameFormatAttr.FORMAT_MSG)
                                        .put(ElideNamespaceNameFormatAttr.FORMAT_KEY,
                                                        ElideNamespaceNameFormatAttr.FORMAT_KEY_MSG)
                                        .put(ElideNamespaceNameFormatAttr.FORMAT_ADDITIONAL_KEY,
                                                ElideNamespaceNameFormatAttr.FORMAT_ADDITIONAL_KEY_MSG)
                                        .put(ElideRSQLFilterFormatAttr.FORMAT_KEY, ElideRSQLFilterFormatAttr.FORMAT_MSG)
                                        .put(JavaClassNameWithExtFormatAttr.FORMAT_KEY,
                                                        JavaClassNameWithExtFormatAttr.FORMAT_MSG)
                                        .put(JavaClassNameFormatAttr.FORMAT_KEY, JavaClassNameFormatAttr.FORMAT_MSG)
                                        .put(ElideJDBCUrlFormatAttr.FORMAT_KEY, ElideJDBCUrlFormatAttr.FORMAT_MSG)
                                        .put(ElideRoleFormatAttr.FORMAT_KEY, ElideRoleFormatAttr.FORMAT_MSG)

                                        // for Keyword errors
                                        .put(ValidateDimPropertiesValidator.ATMOST_ONE_KEY,
                                                        ValidateDimPropertiesValidator.ATMOST_ONE_MSG)
                                        .put(ValidateDimPropertiesValidator.ADDITIONAL_KEY,
                                                        ValidateDimPropertiesValidator.ADDITIONAL_MSG)
                                        .put(ValidateTimeDimPropertiesValidator.ADDITIONAL_KEY,
                                                        ValidateTimeDimPropertiesValidator.ADDITIONAL_MSG)
                                        .put(ValidateArgsPropertiesValidator.ATMOST_ONE_KEY,
                                                        ValidateArgsPropertiesValidator.ATMOST_ONE_MSG)
                                        .build())
                        .freeze();
    }
}
