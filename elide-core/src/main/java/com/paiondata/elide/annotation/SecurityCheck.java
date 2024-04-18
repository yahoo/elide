/*
 * Copyright 2018, the original author or authors.
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A convenience annotation that help you register elide check.
 * <br><br>
 * Example: <br>
 * <pre>
 * <code>@SecurityCheck("i am an expression")</code>
 * public static class{@literal Inline<Post>} extends{@literal OperationCheck<Post>} {
 *   <code>@Override</code>
 *   public boolean ok(Post object, RequestScope requestScope,
 *       {@literal Optional<ChangeSpec>} changeSpec) {
 *     return false;
 *   }
 * }
 * </pre>
 *
 * <b>NOTE: </b> The class you annotated must be a {@link com.paiondata.elide.core.security.checks.Check},
 * otherwise a RuntimeException is thrown.
 *
 * @author olOwOlo
 *
 * This class is based on https://github.com/illyasviel/elide-spring-boot/blob/master
 * /elide-spring-boot-autoconfigure/src/main/java/org/illyasviel/elide/spring/boot/annotation/ElideCheck.java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface SecurityCheck {

    /**
     * The expression which will be used for
     * {@link com.paiondata.elide.annotation.ReadPermission#expression()},
     * {@link com.paiondata.elide.annotation.UpdatePermission#expression()},
     * {@link com.paiondata.elide.annotation.CreatePermission#expression()},
     * {@link com.paiondata.elide.annotation.DeletePermission#expression()}.
     * @return The expression you want to defined.
     */
    String value();
}
