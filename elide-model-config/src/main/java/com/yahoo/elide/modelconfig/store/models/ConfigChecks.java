/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store.models;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.security.checks.InjectableOperationCheck;
import com.yahoo.elide.core.security.checks.OperationCheck;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Utility class which contains a set of injectable operation checks to override the security
 * rules for reading and manipulating configuration files.
 */
public class ConfigChecks {

    public static final String CAN_READ_CONFIG = "Can Read Config";
    public static final String CAN_UPDATE_CONFIG = "Can Update Config";
    public static final String CAN_DELETE_CONFIG = "Can Delete Config";
    public static final String CAN_CREATE_CONFIG = "Can Create Config";

    @SecurityCheck(CAN_READ_CONFIG)
    public static class CanReadConfigCheck extends InjectableOperationCheck {

        @Inject
        @Named("ConfigReadCheck")
        Optional<OperationCheck> actualCheck;

        @Override
        public Optional<OperationCheck> getActualCheck() {
            return actualCheck;
        }
    }

    @SecurityCheck(CAN_UPDATE_CONFIG)
    public static class CanUpdateConfigCheck extends InjectableOperationCheck {

        @Inject
        @Named("ConfigUpdateCheck")
        Optional<OperationCheck> actualCheck;

        @Override
        public Optional<OperationCheck> getActualCheck() {
            return actualCheck;
        }
    }

    @SecurityCheck(CAN_DELETE_CONFIG)
    public static class CanDeleteConfigCheck extends InjectableOperationCheck {
        @Inject
        @Named("ConfigDeleteCheck")
        Optional<OperationCheck> actualCheck;

        @Override
        public Optional<OperationCheck> getActualCheck() {
            return actualCheck;
        }
    }

    @SecurityCheck(CAN_CREATE_CONFIG)
    public static class CanCreateConfigCheck extends InjectableOperationCheck {

        @Inject
        @Named("ConfigCreateCheck")
        Optional<OperationCheck> actualCheck;

        @Override
        public Optional<OperationCheck> getActualCheck() {
            return actualCheck;
        }
    }
}
