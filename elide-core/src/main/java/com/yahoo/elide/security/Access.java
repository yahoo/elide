/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.RequestScope;

import java.util.Optional;

/**
 * Simple checks to always grant or deny.
 */
public class Access {
    /**
     * Check which always grants.
     */
    public static class ALL implements Check {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional optional) {
            return true;
        }

        @Override
        public boolean ok(RequestScope requestScope, Optional optional) {
            return true;
        }
    }

    /**
     * Check which always denies.
     */
    public static class NONE implements Check {
        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional optional) {
            return false;
        }

        @Override
        public boolean ok(RequestScope requestScope, Optional optional) {
            return false;
        }
    }
}
