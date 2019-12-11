/*
 * Copyright 2018, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A convenience annotation that help you register elide function hook.
 * <br><br>
 * Example: <br>
 * <pre>
 * <code>@Hook(lifeCycle = OnUpdatePreCommit.class)</code>
 * public class AccountUpdatePreCommit implements{@literal LifeCycleHook<Account>} {
 *
 *   <code>@Autowired</code>
 *   private SomeComponent someComponent;
 *
 *   <code>@Override</code>
 *   public void execute(Account account, RequestScope requestScope,
 *      {@literal Optional<ChangeSpec>} changes) {
 *     // do something.
 *   }
 * }
 * </pre>
 *
 * <b>NOTE: </b> The class you annotated must implements {@literal LifeCycleHook<T>},
 * otherwise a RuntimeException is thrown.
 *
 * @author olOwOlo
 *
 *  This class is based on https://github.com/illyasviel/elide-spring-boot/blob/master
 *  /elide-spring-boot-autoconfigure/src/main/java/org/illyasviel/elide/spring/boot/annotation/ElideHook.java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Hook {

    /**
     * Define the life cycle phase(OnReadPostCommit.class, OnUpdatePreSecurity.class, etc).
     * @return OnXXX.class
     */
    Class<? extends Annotation> lifeCycle();

    /**
     * If set, the lifecycle hook will invoke for the specific field in the class.
     * This is ignored if allFields is set to true.
     * @return The name of the field or method
     */
    String fieldOrMethodName() default "";

    /**
     * If set to true, the lifecycle hook will invoke for all fields in the class.
     * @return Whether or not to include all fields for callbacks.
     */
    boolean allFields() default false;
}
