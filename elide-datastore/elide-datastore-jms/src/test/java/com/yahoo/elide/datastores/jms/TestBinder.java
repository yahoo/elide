package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.SubscriptionDataFetcher;
import com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Author;
import example.Book;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import graphql.GraphQL;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;

import java.util.Calendar;
import java.util.Set;
import javax.jms.ConnectionFactory;

public class TestBinder extends AbstractBinder {

    private final AuditLogger auditLogger;
    private final ServiceLocator injector;

    public TestBinder(final AuditLogger auditLogger, final ServiceLocator injector) {
        this.auditLogger = auditLogger;
        this.injector = injector;
    }

    @Override
    protected void configure() {
        EntityDictionary dictionary = EntityDictionary.builder()
                .injector(injector::inject)
                .build();

        bind(dictionary).to(EntityDictionary.class);

        // Elide instance
        bindFactory(new Factory<Elide>() {
            @Override
            public Elide provide() {

                HashMapDataStore inMemoryStore = new HashMapDataStore(Set.of(Book.class, Author.class));
                Elide elide = buildElide(inMemoryStore, dictionary);

                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://0");
                SubscriptionScanner subscriptionScanner = new SubscriptionScanner(
                        connectionFactory,
                        elide.getMapper().getObjectMapper(),
                        elide.getElideSettings().getDictionary(),
                        elide.getScanner()
                );

                subscriptionScanner.bindLifecycleHooks();
                return elide;
            }

            @Override
            public void dispose(Elide elide) {

            }
        }).to(Elide.class).named("elide");
    }

    protected Elide buildElide(DataStore store, EntityDictionary dictionary) {
        RSQLFilterDialect rsqlFilterStrategy = new RSQLFilterDialect(dictionary);

        return new Elide(new ElideSettingsBuilder(store)
                .withAuditLogger(auditLogger)
                .withJoinFilterDialect(rsqlFilterStrategy)
                .withSubqueryFilterDialect(rsqlFilterStrategy)
                .withEntityDictionary(dictionary)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                .build());
    }
}
