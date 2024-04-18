/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.hooks;

import static example.Chat.CHAT;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.lifecycle.LifeCycleHook;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.graphql.subscriptions.hooks.NotifyTopicLifeCycleHook;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Chat;
import example.ChatBot;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import lombok.Data;

import java.util.Optional;

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
                new ObjectMapper(),
                JMSContext::createProducer
        );

        publisher.publish(new Chat(1, "Hello!"), CHAT);
        publisher.publish(new Chat(2, "How is your day?"), CHAT);
        publisher.publish(new Chat(3, "My name is " + bot.getName()), CHAT);
    }
}
