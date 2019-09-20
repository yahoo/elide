package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.aggregation.example.Author;
import com.yahoo.elide.datastores.aggregation.example.Book;
import com.yahoo.elide.request.EntityProjection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;

public class TestDataStoreTransaction implements DataStoreTransaction {
    private final Map<Class<?>, Map<String, Object>> dataStore;

    public TestDataStoreTransaction(Map<Class<?>, Map<String, Object>> dataStore){
        this.dataStore = dataStore;
    }

    @Override
    public void save(Object entity, RequestScope scope) {

    }

    @Override
    public void delete(Object entity, RequestScope scope) {

    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {

    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        System.out.println("Creating object");
    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        System.out.println("Loading data");
        List<Object> data = new ArrayList<>();
        PlayerStats ps = new PlayerStats();
        Country germany = new Country();
        germany.setName("Germany");
        ps.setHighScore(681);
        ps.setCountry(germany);
        PlayerStats ps2 = new PlayerStats();
        Country italy = new Country();
        italy.setName("Italy");
        ps2.setHighScore(421);
        ps2.setCountry(italy);
        data.add(ps);
        data.add(ps2);
        return data;
    }

    @Override
    public void close() throws IOException {

    }
}
