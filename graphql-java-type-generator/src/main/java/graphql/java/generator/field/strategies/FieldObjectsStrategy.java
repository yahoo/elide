package graphql.java.generator.field.strategies;

import java.util.List;

import graphql.java.generator.strategies.Strategy;

public interface FieldObjectsStrategy extends Strategy {
    /**
     * Given an object derived from the {@linkplain graphql.java.generator.type.TypeGenerator TypeGenerator}, find out what
     * should become GraphQL Fields.
     * @param object A representative "field" object, the exact type of which is contextual
     * @return null to indicate this object should not be built,
     * or List (of any size) containing objects that will be passed to other strategies.
     */
    List<Object> getFieldRepresentativeObjects(Object object);
}
