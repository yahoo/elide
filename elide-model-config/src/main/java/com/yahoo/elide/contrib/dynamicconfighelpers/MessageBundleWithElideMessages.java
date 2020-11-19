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
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ValidateDimPropertiesValidator;
import com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats.ValidateTimeDimPropertiesValidator;
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
        msgSourceBuilder.put(ElideTimeFieldTypeFormatAttr.TYPE_KEY, ElideTimeFieldTypeFormatAttr.TYPE_MSG);
        msgSourceBuilder.put(ElideNameFormatAttr.FORMAT_KEY, ElideNameFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(ElideRSQLFilterFormatAttr.FORMAT_KEY, ElideRSQLFilterFormatAttr.FORMAT_MSG);
        msgSourceBuilder.put(ElideClassNameFormatAttr.FORMAT_KEY, ElideClassNameFormatAttr.FORMAT_MSG);

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
