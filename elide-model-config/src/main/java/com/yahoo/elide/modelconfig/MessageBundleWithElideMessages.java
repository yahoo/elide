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
import com.yahoo.elide.modelconfig.jsonformats.ElideTimeFieldTypeFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameWithExtFormatAttr;
import com.yahoo.elide.modelconfig.jsonformats.ValidateDimPropertiesValidator;
import com.yahoo.elide.modelconfig.jsonformats.ValidateTimeDimPropertiesValidator;
import com.github.fge.jsonschema.messages.JsonSchemaValidationBundle;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.github.fge.msgsimple.source.MapMessageSource;
import com.github.fge.msgsimple.source.MapMessageSource.Builder;
import lombok.Getter;

/**
 * Augment the {@link MessageBundle} with error messages for custom format attributes and keywords.
 */
public class MessageBundleWithElideMessages {
    @Getter
    private MessageBundle msgBundle;

    public MessageBundleWithElideMessages() {
        Builder msgSourceBuilder = MapMessageSource.newBuilder();

        // For Format errors
        msgSourceBuilder.put(ElideFieldNameFormatAttr.FORMAT_KEY, ElideFieldNameFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(ElideFieldNameFormatAttr.NAME_KEY, ElideFieldNameFormatAttr.NAME_MSG);
        msgSourceBuilder.put(ElideCardinalityFormatAttr.TYPE_KEY, ElideCardinalityFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideFieldTypeFormatAttr.TYPE_KEY, ElideFieldTypeFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideGrainTypeFormatAttr.TYPE_KEY, ElideGrainTypeFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideJoinTypeFormatAttr.TYPE_KEY, ElideJoinTypeFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideJoinKindFormatAttr.TYPE_KEY, ElideJoinKindFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideTimeFieldTypeFormatAttr.TYPE_KEY, ElideTimeFieldTypeFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideNameFormatAttr.FORMAT_KEY, ElideNameFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(ElideRSQLFilterFormatAttr.FORMAT_KEY, ElideRSQLFilterFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(JavaClassNameWithExtFormatAttr.FORMAT_KEY, JavaClassNameWithExtFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(JavaClassNameFormatAttr.FORMAT_KEY, JavaClassNameFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(ElideJDBCUrlFormatAttr.FORMAT_KEY, ElideJDBCUrlFormatAttr.FORMAT_MSG);

        // for Keyword errors
        msgSourceBuilder.put(ValidateDimPropertiesValidator.ATMOST_ONE_KEY,
                        ValidateDimPropertiesValidator.ATMOST_ONE_MSG);
        msgSourceBuilder.put(ValidateDimPropertiesValidator.ADDITIONAL_KEY,
                        ValidateDimPropertiesValidator.ADDITIONAL_MSG);
        msgSourceBuilder.put(ValidateTimeDimPropertiesValidator.ADDITIONAL_KEY,
                        ValidateTimeDimPropertiesValidator.ADDITIONAL_MSG);

        this.msgBundle = MessageBundles.getBundle(JsonSchemaValidationBundle.class).thaw()
                        .appendSource(msgSourceBuilder.build())
                        .freeze();
    }
}
