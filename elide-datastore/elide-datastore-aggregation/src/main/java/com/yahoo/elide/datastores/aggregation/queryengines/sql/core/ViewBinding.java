package com.yahoo.elide.datastores.aggregation.queryengines.sql.core;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.core.EntityBinding;

import java.lang.reflect.AccessibleObject;
import java.util.Collection;
import java.util.List;

/**
 * A binding for a view to store all fields and relationship in that view.
 */
public class ViewBinding extends EntityBinding {
    public ViewBinding(Class<?> cls, String name) {
        entityClass = cls;
        entityName = name;

        // Map id's, attributes, and relationships
        List<AccessibleObject> fieldOrMethodList = getAllFields();

        bindViewFields(fieldOrMethodList);

        attributes = dequeToList(attributesDeque);
        relationships = dequeToList(relationshipsDeque);
    }

    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param fieldOrMethodList List of fields and methods on entity
     */
    private void bindViewFields(Collection<AccessibleObject> fieldOrMethodList) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            if (!fieldOrMethod.isAnnotationPresent(Exclude.class)) {
                bindAttrOrRelation(fieldOrMethod);
            }
        }
    }

    public ViewBinding() {}
}
