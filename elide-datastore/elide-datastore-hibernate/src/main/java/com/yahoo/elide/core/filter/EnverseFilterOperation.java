package com.yahoo.elide.core.filter;


import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.AuditProperty;

public class EnverseFilterOperation implements FilterOperation<AuditCriterion> {

    private EntityDictionary dictionary;
    private Class<?> entityClass;

    public EnverseFilterOperation(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }
    @Override
    public AuditCriterion apply(FilterPredicate filterPredicate) {
        if (filterPredicate.getPath().getPathElements().size() > 1) {
            throw new RuntimeException("Entity traversal is not supported in revision datastore");
        }
        Path.PathElement field = filterPredicate.getPath().getPathElements().get(0);
        String fieldName = field.getFieldName();
        Class fieldType = field.getFieldType();
        Class entityType = field.getType();

        switch(filterPredicate.getOperator()) {
            case IN:
                AuditCriterion criterion = null;
                if (dictionary.getRelationships(entityType).contains(fieldName)) {
                    if (dictionary.getRelationshipType(entityType, fieldName).isToMany()) {
                        throw new RuntimeException("FilterPath can move only along ToOne relationships");
                    }
                    for (Object value : filterPredicate.getValues()){
                        if (criterion == null){
                            criterion = AuditEntity.relatedId(fieldName).eq(value);
                        } else {
                            criterion = AuditEntity.or(criterion, AuditEntity.relatedId(fieldName).eq(value));
                        }
                    }
                } else if (dictionary.getIdFieldName(entityType).equals(fieldName)){
                    for (Object value : filterPredicate.getValues()){
                        if (criterion == null){
                            criterion = AuditEntity.id().eq(value);
                        } else {
                            criterion = AuditEntity.or(criterion, AuditEntity.id().eq(value));
                        }
                    }

                } else {
                    criterion = AuditEntity.property(fieldName).in(filterPredicate.getValues());
                }
                return criterion;
            case GE:
                return AuditEntity.property(fieldName).ge(filterPredicate.getParameters().get(0));
            case LE:
                return AuditEntity.property(fieldName).le(filterPredicate.getParameters().get(0));
            case GT:
                return AuditEntity.property(fieldName).gt(filterPredicate.getParameters().get(0));
            case LT:
                return AuditEntity.property(fieldName).lt(filterPredicate.getParameters().get(0));
            case FALSE:
                return AuditEntity.not(AuditEntity.property(fieldName).eqProperty(fieldName));
            case TRUE:
                return AuditEntity.property(fieldName).eqProperty(fieldName);
            default:
                throw new RuntimeException("unsupported operation");
        }
    }

    public AuditCriterion apply(FilterExpression filterExpression) {
        AuditCriterionVisitor visitor = new AuditCriterionVisitor();
        return filterExpression.accept(visitor);

    }


    public class AuditCriterionVisitor implements FilterExpressionVisitor<AuditCriterion> {
        public static final String TWO_NON_FILTERING_EXPRESSIONS =
                "Cannot build a filter from two non-filtering expressions";
        private boolean prefixWithAlias;

        @Override
        public AuditCriterion visitPredicate(FilterPredicate filterPredicate) {
            return apply(filterPredicate);
        }

        @Override
        public AuditCriterion visitAndExpression(AndFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            return AuditEntity.and(left.accept(this), right.accept(this));
        }

        @Override
        public AuditCriterion visitOrExpression(OrFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            return AuditEntity.or(left.accept(this), right.accept(this));
        }

        @Override
        public AuditCriterion visitNotExpression(NotFilterExpression expression) {
            AuditCriterion negated = expression.getNegated().accept(this);
            return AuditEntity.not(negated);
        }
    }
}
