package com.yahoo.elide.datastores.aggregation;

import com.google.common.collect.Sets;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapStoreTransaction;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.persistence.Entity;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TestAggregationDataStore implements DataStore {

    private final Map<Class<?>, Map<String, Object>> dataStore = Collections.synchronizedMap(new HashMap<>());
    @Getter
    private EntityDictionary dictionary;
    @Getter private final Set<Package> beanPackages;
    @Getter private final ConcurrentHashMap<Class<?>, AtomicLong> typeIds = new ConcurrentHashMap<>();

    public TestAggregationDataStore(Package beanPackage) {
        this(Sets.newHashSet(beanPackage));
    }

    public TestAggregationDataStore(Set<Package> beanPackages) {
        this.beanPackages = beanPackages;
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        for (Package beanPackage : beanPackages) {
            configurationBuilder.addUrls(ClasspathHelper.forPackage(beanPackage.getName()));
        }
        configurationBuilder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());

        Reflections reflections = new Reflections(configurationBuilder);

        reflections.getTypesAnnotatedWith(Entity.class).stream()
                .forEach((cls) -> {
                    for (Package beanPackage : beanPackages) {
                        if (cls.getName().startsWith(beanPackage.getName())) {
                            dataStore.put(cls, Collections.synchronizedMap(new LinkedHashMap<>()));
                            break;
                        }
                    }
                });
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Class<?> clazz : dataStore.keySet()) {
            dictionary.bindEntity(clazz);
        }

        this.dictionary = dictionary;
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        System.out.println("Doing stuff from database");
        return new TestDataStoreTransaction(dataStore);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Data store contents ");
        for (Class<?> cls : dataStore.keySet()) {
            sb.append("\n Table ").append(cls).append(" contents \n");
            Map<String, Object> data = dataStore.get(cls);
            for (Map.Entry<String, Object> e : data.entrySet()) {
                sb.append(" Id: ").append(e.getKey()).append(" Value: ").append(e.getValue());
            }
        }
        return sb.toString();
    }
}
