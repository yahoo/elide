package com.yahoo.elide.datastores.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import example.Book;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import net.jodah.concurrentunit.Waiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Topic;
import javax.naming.InitialContext;

public class JMSDataStoreTest {

    @Test
    public void testLoadObjects() throws Exception {
        EmbeddedActiveMQ embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", "vm://0");
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();

        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://0");

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);

        Book book = new Book();
        book.setTitle("Enders Game");

        JMSDataStore store = new JMSDataStore(Sets.newHashSet(ClassType.of(Book.class)), cf, dictionary);
        store.populateEntityDictionary(dictionary);

        DataStoreTransaction tx = store.beginReadTransaction();
        Iterable<Book> books = tx.loadObjects(
            EntityProjection.builder().type(Book.class).build(),
            null
        );

        JMSContext context = cf.createContext();
        context.start();
        Destination destination = context.createTopic("bookAdded");

        JMSProducer producer = context.createProducer();
        ObjectMapper mapper = new ObjectMapper();
        producer.send(destination, mapper.writeValueAsString(book));

        Book received = books.iterator().next();
        assertEquals("Enders Game", received.getTitle());
    }
}
