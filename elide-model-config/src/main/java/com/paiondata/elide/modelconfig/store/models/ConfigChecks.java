/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.modelconfig.store.models;

import com.paiondata.elide.core.security.checks.prefab.Role;

/**
 * Utility class which contains a set of check labels.  Clients need to define checks for these
 * labels and bind them to the dictionary at boot.
 */
public class ConfigChecks {
    public static final String CAN_READ_CONFIG = "Can Read Config";
    public static final String CAN_UPDATE_CONFIG = "Can Update Config";
    public static final String CAN_DELETE_CONFIG = "Can Delete Config";
    public static final String CAN_CREATE_CONFIG = "Can Create Config";

    public static class CanNotRead extends Role.NONE {

    };
    public static class CanNotUpdate extends Role.NONE {

    };
    public static class CanNotCreate extends Role.NONE {

    };
    public static class CanNotDelete extends Role.NONE {

    };
    public static class CanRead extends Role.ALL {

    };
    public static class CanUpdate extends Role.ALL {

    };
    public static class CanCreate extends Role.ALL {

    };
    public static class CanDelete extends Role.ALL {

    };
}
