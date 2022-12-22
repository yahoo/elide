/*
 * Copyright 2022 Yahoo! Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.elide.extension.test.models;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;
import java.util.Optional;

/**
 * A security check that denies access unconditionally.
 *
 */
@SecurityCheck("Deny")
public class DenyCheck extends OperationCheck<Supplier> {

    @Override
    public boolean ok(Supplier object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return false;
    }

}