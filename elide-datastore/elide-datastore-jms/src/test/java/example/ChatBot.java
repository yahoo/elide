/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import example.hooks.ChatBotCreateHook;
import lombok.Data;

import javax.persistence.Id;

@Include
@Data
@LifeCycleHookBinding(
        hook = ChatBotCreateHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT
)
public class ChatBot {

    @Id
    long id;

    String name;
}
