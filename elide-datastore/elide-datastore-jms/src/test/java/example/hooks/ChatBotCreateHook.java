/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.hooks;

import static example.Chat.CHAT;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.graphql.subscriptions.hooks.NotifyTopicLifeCycleHook;
import com.google.gson.GsonBuilder;
import example.Chat;
import example.ChatBot;

import lombok.Data;

import java.util.Optional;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;

@Data
public class ChatBotCreateHook implements LifeCycleHook<ChatBot> {

    @Inject
    ConnectionFactory connectionFactory;

    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            ChatBot bot,
            RequestScope requestScope,
            Optional<ChangeSpec> changes) {

        NotifyTopicLifeCycleHook<Chat> publisher = new NotifyTopicLifeCycleHook<>(
                connectionFactory,
                JMSContext::createProducer,
                new GsonBuilder().create()
        );

        publisher.publish(new Chat(1, "Hello!"), CHAT);
        publisher.publish(new Chat(2, "How is your day?"), CHAT);
        publisher.publish(new Chat(3, "My name is " + bot.getName()), CHAT);
    }
}
