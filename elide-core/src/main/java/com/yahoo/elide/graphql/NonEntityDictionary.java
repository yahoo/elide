package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.WordUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A set of reflection utilities for non-Elide entities.
 */
@Slf4j
public class NonEntityDictionary extends EntityDictionary {
    public NonEntityDictionary() {
        super(new HashMap<>());
    }

    /**
     * Add given class to dictionary.
     *
     * @param cls Entity bean class
     */
    @Override
    public void bindEntity(Class<?> cls) {
        String type = WordUtils.uncapitalize(cls.getSimpleName());

        Class<?> duplicate = bindJsonApiToEntity.put(type, cls);

        if (duplicate != null && !duplicate.equals(cls)) {
            log.error("Duplicate binding {} for {}, {}", type, cls, duplicate);
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + duplicate.getName());
        }
    }

    /**
     * Returns whether or not a class is already bound.
     * @param cls
     * @return
     */
    public boolean hasBinding(Class<?> cls) {
        return bindJsonApiToEntity.contains(cls);
    }
}
