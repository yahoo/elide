package com.yahoo.elide.datastores.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.metadata.models.AnalyticView;

import com.google.common.collect.Sets;

import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MetaDataStore extends HashMapDataStore {

    public MetaDataStore() {
        super(MetaDataStore.class.getPackage());
    }

    @Override
    public void populateEntityDictionary(EntityDictionary entityDictionary) {
        super.populateEntityDictionary(entityDictionary);

        for (Class<?> cls : entityDictionary.getBindings()) {
            populateMetaData(new Schema(cls, entityDictionary));
        }
    }

    private void populateMetaData(Schema schema) {
        Class<?> entityClass = schema.getEntityClass();

        if (entityClass.isAnnotationPresent(FromTable.class) || entityClass.isAnnotationPresent(FromSubquery.class)) {
            populateAnalyticView(schema);
        } else {
            populateDimensionTable(schema);
        }
    }

    private void populateAnalyticView(Schema schema) {

    }

    private void populateDimensionTable(Schema schema) {

    }
}
