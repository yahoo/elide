/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.cache;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;

import lombok.Setter;
import redis.clients.jedis.UnifiedJedis;

/**
 * A Redis cache.
 */
public class RedisCache implements Cache {
    @Setter private UnifiedJedis jedis;
    @Setter private long defaultExprirationMinutes;
    Kryo kryo;

    /**
     * Constructor.
     * @param jedis Jedis Connection Pool to Redis clusteer.
     * @param defaultExprirationMinutes Expiration Time for results on Redis.
     */
    public RedisCache(UnifiedJedis jedis, long defaultExprirationMinutes) {
        this.jedis = jedis;
        this.defaultExprirationMinutes = defaultExprirationMinutes;
        this.kryo = new Kryo();
        kryo.setRegistrationRequired(false);
       // kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        /*kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.lang.Class.class);
        try {
            kryo.register(Class.forName("java.util.Collections$EmptySet"));
        } catch (ClassNotFoundException e) {
           throw new IllegalStateException(e);
        }
        kryo.register(java.util.Collections.class);

        kryo.register(QueryResult.class);
        kryo.register(com.yahoo.elide.core.request.Argument.class);
        kryo.register(com.yahoo.elide.core.request.Attribute.class);
        kryo.register(com.yahoo.elide.core.type.ClassType.class);
        kryo.register(com.yahoo.elide.core.type.Dynamic.class);
        kryo.register(com.yahoo.elide.core.type.FieldType.class);
        kryo.register(com.yahoo.elide.core.type.Member.class);
        kryo.register(com.yahoo.elide.core.type.Package.class);
        kryo.register(com.yahoo.elide.core.type.ParameterizedAttribute.class);
        kryo.register(com.yahoo.elide.core.type.ParameterizedModel.class);
        kryo.register(com.yahoo.elide.core.type.Type.class);
        kryo.register(com.yahoo.elide.datastores.aggregation.dynamic.DynamicModelInstance.class);
        kryo.register(com.yahoo.elide.datastores.aggregation.dynamic.FieldType.class);
        kryo.register(com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.class);
        kryo.register(com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.class);
        kryo.register(com.yahoo.elide.datastores.aggregation.dynamic.TableType.class);
        kryo.register(com.yahoo.elide.datastores.aggregation.timegrains.Time.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Argument.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Dimension.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Grain.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Join.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Measure.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Named.class);
        kryo.register(com.yahoo.elide.modelconfig.model.NamespaceConfig.class);
        kryo.register(com.yahoo.elide.modelconfig.model.Table.class);
        kryo.register(com.yahoo.elide.modelconfig.model.TableSource.class);*/
    }

    public void registerWithKryo(Class type) {
        kryo.register(type);
    }

    @Override
    public QueryResult get(Object key) {
        Output keyBytes = new Output(1024, -1);
        kryo.writeObject(keyBytes, key);

        byte[] bytes = jedis.get(keyBytes.getBuffer());
        if (bytes == null) {
            return null;
        }
        Input input = new Input(bytes, 0, bytes.length);
        return kryo.readObject(input, QueryResult.class);
    }

    @Override
    public void put(Object key, QueryResult result) {
        Output keyBytes = new Output(1024, -1);
        kryo.writeObject(keyBytes, key);

        Output output = new Output(1024, -1);
        kryo.writeObject(output, result);
        jedis.set(keyBytes.getBuffer(), output.getBuffer());
        jedis.expire(keyBytes.getBuffer(), defaultExprirationMinutes * 60);
    }
}
