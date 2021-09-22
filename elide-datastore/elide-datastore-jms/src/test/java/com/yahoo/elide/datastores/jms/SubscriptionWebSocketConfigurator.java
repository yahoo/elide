package com.yahoo.elide.datastores.jms;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.SubscriptionDataFetcher;
import com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Author;
import example.Book;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import graphql.GraphQL;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;

import java.util.Calendar;
import java.util.Set;
import javax.jms.ConnectionFactory;
import javax.websocket.server.ServerEndpointConfig;

public class SubscriptionWebSocketConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (endpointClass.equals(SubscriptionWebSocket.class)) {

            EntityDictionary dictionary = EntityDictionary.builder().build();

            DataStore store = buildDataStore(dictionary, new ObjectMapper());

            Elide elide = buildElide(store, dictionary);

            return (T) buildWebSocket(elide, store, dictionary);

        }

        return super.getEndpointInstance(endpointClass);
    }

    protected Elide buildElide(DataStore store, EntityDictionary dictionary) {
        RSQLFilterDialect rsqlFilterStrategy = new RSQLFilterDialect(dictionary);

        return new Elide(new ElideSettingsBuilder(store)
                .withAuditLogger(new Slf4jLogger())
                .withJoinFilterDialect(rsqlFilterStrategy)
                .withSubqueryFilterDialect(rsqlFilterStrategy)
                .withEntityDictionary(dictionary)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                .build());
    }

    protected DataStore buildDataStore(EntityDictionary dictionary, ObjectMapper mapper) {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://0");

        return new JMSDataStore(
                Set.of(ClassType.of(Book.class), ClassType.of(Author.class)),
                connectionFactory, dictionary,
                mapper, -1);
    }

    protected SubscriptionWebSocket buildWebSocket(Elide elide, DataStore topicStore, EntityDictionary dictionary) {

        NonEntityDictionary nonEntityDictionary =
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup);

        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary, nonEntityDictionary,
                new SubscriptionDataFetcher(nonEntityDictionary), NO_VERSION);

        GraphQL api = GraphQL.newGraphQL(builder.build())
                .queryExecutionStrategy(new AsyncSerialExecutionStrategy())
                .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                .build();

        return SubscriptionWebSocket.builder()
                .topicStore(topicStore)
                .elide(elide)
                .api(api)
                .build();
    }
}
