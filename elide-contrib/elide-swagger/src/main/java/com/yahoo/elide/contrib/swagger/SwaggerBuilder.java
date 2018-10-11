/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.model.Data;
import com.yahoo.elide.contrib.swagger.model.Datum;
import com.yahoo.elide.contrib.swagger.property.Relationship;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.filter.Operator;

import com.google.common.collect.Sets;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Builds a 'Swagger' object from Swagger-Core by walking the static Elide entity metadata contained in the
 * 'EntityDictionary'.  The 'Swagger' object can be used to generate a Swagger document.
 */
public class SwaggerBuilder {
    protected EntityDictionary dictionary;
    protected Set<Class<?>> rootClasses;
    protected Set<Class<?>> allClasses;
    protected Swagger swagger;
    protected Map<Integer, Response> globalResponses;
    protected Set<Parameter> globalParams;
    protected Set<Operator> filterOperators;

    public static final Response UNAUTHORIZED_RESPONSE = new Response().description("Unauthorized");
    public static final Response FORBIDDEN_RESPONSE = new Response().description("Forbidden");
    public static final Response NOT_FOUND_RESPONSE = new Response().description("Not Found");
    public static final Response REQUEST_TIMEOUT_RESPONSE = new Response().description("Request Timeout");
    public static final Response TOO_MANY_REQUESTS_RESPONSE = new Response().description("Too Many Requests");

    /**
     * Metadata for constructing URLs and Swagger 'Path' objects.
     */
    @AllArgsConstructor
    public class PathMetaData {

        /**
         * All prior nodes in the path to the root entity.
         */
        private Stack<PathMetaData> lineage;

        /**
         * Either the name of the root collection or the relationship.
         */
        @Getter
        private String name;

        /**
         * Either the type of the root collection of the relationship.
         */
        @Getter
        private Class<?> type;

        /**
         * Constructs a PathMetaData for a 'root' entity.
         * @param type the 'root' entity type of the first segment of the URL.
         */
        public PathMetaData(Class<?> type) {
            this.type = type;
            lineage = new Stack<>();
            name = dictionary.getJsonAliasFor(type);
        }

        /**
         * @return Something like '/book/{bookId}/authors' or '/publisher'
         */
        public String getCollectionUrl() {
            if (lineage.isEmpty()) {
                return "/" + name;
            }
            return lineage.peek().getInstanceUrl() + "/" + name;
        }

        /**
         * @return Something like '/book/{bookId}'
         */
        public String getInstanceUrl() {
            String typeName = dictionary.getJsonAliasFor(type);
            return getCollectionUrl() + "/{" + typeName + "Id}";
        }

        /**
         * @return Something like '/book/{bookId}/relationships/authors'
         */
        public String getRelationshipUrl() {
            if (lineage.isEmpty()) {
                throw new IllegalStateException("Root collections don't have relationships");
            }

            PathMetaData prior = lineage.peek();
            String baseUrl = prior.getInstanceUrl();

            return baseUrl + "/relationships/" + name;
        }

        @Override
        public String toString() {
            return getInstanceUrl();
        }

        /**
         * All Paths are 'tagged' in swagger with the final entity type name in the path.
         * This allows swaggerUI to group the paths by entities.
         * @return the entity type name
         */
        private String getTag() {
            return dictionary.getJsonAliasFor(type);
        }

        /**
         * @return the swagger PathParameter for this particular path segment.
         */
        private Parameter getPathParameter() {
            String typeName = dictionary.getJsonAliasFor(type);

            Parameter param = new PathParameter()
                    .name(typeName + "Id")
                    .description(typeName + " Identifier")
                    .property(new StringProperty());

            return param;
        }

        /**
         * @return the Swagger 'Path' for a relationship URL (/books/{bookId}/relationships/author).
         */
        public Path getRelationshipPath() {
            if (lineage.isEmpty()) {
                throw new IllegalStateException("Root collections don't have relationships");
            }

            Path path = new Path();

            /* The path parameter apply for all operations */
            lineage.stream().forEach((item) -> {
                path.addParameter(item.getPathParameter());
            });

            String typeName = dictionary.getJsonAliasFor(type);

            Response okSingularResponse = new Response()
                    .description("Successful response")
                    .schema(new com.yahoo.elide.contrib.swagger.property.Datum(
                            new Relationship(typeName)));

            Response okPluralResponse = new Response()
                    .description("Successful response")
                    .schema(new com.yahoo.elide.contrib.swagger.property.Data(
                            new Relationship(typeName)));

            Response okEmptyResponse = new Response()
                    .description("Successful response");

            Class<?> parentClass = lineage.peek().getType();
            RelationshipType relationshipType = dictionary.getRelationshipType(parentClass, name);

            if (relationshipType.isToMany()) {
                path.get(new JsonApiOperation()
                        .description("Returns the relationship identifiers for " + name)
                        .tag(getTag())
                        .response(200, okPluralResponse));

                path.patch(new JsonApiOperation()
                        .description("Replaces the relationship " + name)
                        .tag(getTag())
                        .response(204, okEmptyResponse)
                        .parameter(new BodyParameter()
                                .schema(new Data(new Relationship(typeName)))
                                .name("relationship"))
                );
                path.delete(new JsonApiOperation()
                                .description("Deletes items from the relationship " + name)
                                .tag(getTag())
                                .response(204, okEmptyResponse)
                                .parameter(new BodyParameter()
                                        .schema(new Data(new Relationship(typeName)))
                                        .name("relationship"))
                );
                path.post(new JsonApiOperation()
                                .description("Adds items to the relationship " + name)
                                .tag(getTag())
                                .response(201, okPluralResponse)
                                .parameter(new BodyParameter()
                                        .schema(new Data(new Relationship(typeName)))
                                        .name("relationship"))
                );
            } else {
                path.get(new JsonApiOperation()
                        .description("Returns the relationship identifiers for " + name)
                        .tag(getTag())
                        .response(200, okSingularResponse));
                path.patch(new JsonApiOperation()
                        .description("Replaces the relationship " + name)
                        .tag(getTag())
                        .response(204, okEmptyResponse)
                        .parameter(new BodyParameter()
                                .schema(new Datum(new Relationship(typeName)))
                                .name("relationship"))
                );
            }

            for (Parameter param : getFilterParameters()) {
                path.getGet().addParameter(param);
            }

            for (Parameter param : getPageParameters()) {
                path.getGet().addParameter(param);
            }

            decorateGlobalResponses(path);
            decorateGlobalParameters(path);
            return path;
        }

        /**
         * @return the Swagger 'Path' for a relationship URL (/books)
         */
        public Path getCollectionPath() {
            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();

            /* The path parameter apply for all operations */
            if (! lineage.isEmpty()) {
                lineage.stream().forEach((item) -> {
                    path.addParameter(item.getPathParameter());
                });
            }

            Response okSingularResponse = new Response()
                    .description("Successful response")
                    .schema(new com.yahoo.elide.contrib.swagger.property.Datum(typeName, false));

            Response okPluralResponse = new Response()
                    .description("Successful response")
                    .schema(new com.yahoo.elide.contrib.swagger.property.Data(typeName));

            String getDescription;
            String postDescription;
            if (lineage.isEmpty()) {
                getDescription = "Returns the collection of type " + typeName;
                postDescription = "Creates an item of type " + typeName;
            } else {
                getDescription = "Returns the relationship " + name;
                postDescription = "Creates an item of type " + typeName + " and adds it to " + name;
            }

            path.get(new JsonApiOperation()
                    .description(getDescription)
                    .tag(getTag())
                    .parameter(getSparseFieldsParameter())
                    .parameter(getIncludeParameter())
                    .parameter(getSortParameter())
                    .response(200, okPluralResponse));

            for (Parameter param : getFilterParameters()) {
                path.getGet().addParameter(param);
            }

            for (Parameter param : getPageParameters()) {
                path.getGet().addParameter(param);
            }

            path.post(new JsonApiOperation()
                    .description(postDescription)
                    .tag(getTag())
                    .response(201, okSingularResponse)
                    .parameter(new BodyParameter()
                            .schema(new Datum(typeName))
                            .name(typeName))
            );

            decorateGlobalResponses(path);
            decorateGlobalParameters(path);
            return path;
        }

        /**
         * @return the Swagger 'Path' for a relationship URL (/books/{bookID})
         */
        public Path getInstancePath() {
            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();

            /* The path parameter apply for all operations */
            getFullLineage().stream().forEach((item) -> {
                path.addParameter(item.getPathParameter());
            });

            Response okSingularResponse = new Response()
                .description("Successful response")
                .schema(new com.yahoo.elide.contrib.swagger.property.Datum(typeName));

            Response okEmptyResponse = new Response()
                .description("Successful response");

            path.get(new JsonApiOperation()
                    .description("Returns an instance of type " + typeName)
                    .tag(getTag())
                    .parameter(getSparseFieldsParameter())
                    .parameter(getIncludeParameter())
                    .response(200, okSingularResponse));

            path.patch(new JsonApiOperation()
                    .description("Modifies an instance of type " + typeName)
                    .tag(getTag())
                    .response(204, okEmptyResponse)
                    .parameter(new BodyParameter()
                            .schema(new Datum(typeName))
                            .name(typeName))
            );

            path.delete(new JsonApiOperation()
                     .description("Deletes an instance of type " + typeName)
                     .tag(getTag())
                     .response(204, okEmptyResponse));

            decorateGlobalResponses(path);
            decorateGlobalParameters(path);
            return path;
        }

        /**
         * Decorates with path parameters that apply to all paths.
         * @param path the path to decorate
         * @return the decorated path
         */
        private Path decorateGlobalParameters(Path path) {
            globalParams.forEach((param) -> {
                path.addParameter(param);
            });
            return path;
        }

        /**
         * Decorates with responses that apply to all operations for all paths.
         * @param path the path to decorate
         * @return the decorated path
         */
        private Path decorateGlobalResponses(Path path) {
            globalResponses.forEach(
                (code, response) -> {
                        if (path.getGet() != null) {
                            path.getGet().response(code, response);
                        }
                        if (path.getDelete() != null) {
                            path.getDelete().response(code, response);
                        }
                        if (path.getPost() != null) {
                            path.getPost().response(code, response);
                        }
                        if (path.getPatch() != null) {
                            path.getPatch().response(code, response);
                        }
                }
            );
            return path;
        }

        /**
         * @return the JSON-API 'field' query parameter for some GET operations.
         */
        private Parameter getSparseFieldsParameter() {
            String typeName = dictionary.getJsonAliasFor(type);
            List<String> fieldNames = dictionary.getAllFields(type);

            return new QueryParameter()
                    .type("array")
                    .name("fields[" + typeName + "]")
                    .description("Selects the set of " + typeName + " fields that should be returned in the result.")
                    .items(new StringProperty()._enum(fieldNames))
                    .collectionFormat("csv");
        }

        /**
         * @return the JSON-API 'include' query parameter for some GET operations.
         */
        private Parameter getIncludeParameter() {
            List<String> relationshipNames = dictionary.getRelationships(type);

            return new QueryParameter()
                    .type("array")
                    .name("include")
                    .description("Selects the set of relationships that should be expanded as a compound document in "
                            + "the result.")
                    .items(new StringProperty()._enum(relationshipNames))
                    .collectionFormat("csv");
        }

        /**
         * @return the Elide 'page' query parameter for some GET operations.
         */
        private List<Parameter> getPageParameters() {
            List<Parameter> params = new ArrayList<>();

            params.add(new QueryParameter()
                    .name("page[number]")
                    .description("Number of pages to return.  Can be used with page[size]")
                    .type("integer")
            );

            params.add(new QueryParameter()
                            .name("page[size]")
                            .description("Number of elements per page.  Can be used with page[number]")
                            .type("integer")
            );

            params.add(new QueryParameter()
                    .name("page[offset]")
                    .description("Offset from 0 to start paginating.  Can be used with page[limit]")
                    .type("integer")
            );

            params.add(new QueryParameter()
                    .name("page[limit]")
                    .description("Maximum number of items to return.  Can be used with page[offset]")
                    .type("integer")
            );

            params.add(new QueryParameter()
                    .name("page[totals]")
                    .description("For requesting total pages/records be included in the response page meta data")
                    /* Swagger UI doesn't support parameters that don't take args today.  We'll just make
                     * this a string for now */
                    .type("string")
            );

            return params;
        }

        /**
         * @return the JSON-API 'sort' query parameter for some GET operations.
         */
        private Parameter getSortParameter() {
            List<String> filterAttributes = dictionary.getAttributes(type).stream()
                    .filter((name) -> {
                        Class<?> attributeClass = dictionary.getType(type, name);
                        return (attributeClass.isPrimitive() || String.class.isAssignableFrom(attributeClass));
                    })
                    .map((name) -> Arrays.asList(name, "-" + name))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            return new QueryParameter()
                    .name("sort")
                    .type("array")
                    .description("Sorts the collection on the selected attributes.  A prefix of '-' sorts descending")
                    .items(new StringProperty()._enum(filterAttributes))
                    .collectionFormat("csv");
        }

        /**
         * @return the Elide 'filter' query parameter for some GET operations.
         */
        private List<Parameter> getFilterParameters() {
            String typeName = dictionary.getJsonAliasFor(type);
            List<String> attributeNames = dictionary.getAttributes(type);

            List<Parameter> params = new ArrayList<>();

            /* Add RSQL Disjoint Filter Query Param */
            params.add(new QueryParameter()
                    .type("string")
                    .name("filter[" + typeName + "]")
                    .description("Filters the collection of " + typeName + " using a 'disjoint' RSQL expression"));

            if (lineage.isEmpty()) {
                 /* Add RSQL Joined Filter Query Param */
                params.add(new QueryParameter()
                        .type("string")
                        .name("filter")
                        .description("Filters the collection of " + typeName + " using a 'joined' RSQL expression"));
            }

            for (Operator op : filterOperators) {
                attributeNames.forEach((name) -> {
                    Class<?> attributeClass = dictionary.getType(type, name);

                    /* Only filter attributes that can be assigned to strings or primitives */
                    if (attributeClass.isPrimitive() || String.class.isAssignableFrom(attributeClass)) {
                        params.add(new QueryParameter()
                                .type("string")
                                .name("filter[" + typeName + "." + name + "][" + op.getNotation() + "]")
                                .description("Filters the collection of " + typeName + " by the attribute "
                                        + name + " " + "using the operator " + op.getNotation()));
                    }
                });
            }

            return params;
        }

        /**
         * @return ALL of the path segments in the URL including this segment.
         */
        public Stack<PathMetaData> getFullLineage() {
            Stack<PathMetaData> fullLineage = new Stack<>();

            fullLineage.addAll(lineage);
            fullLineage.add(this);
            return fullLineage;
        }

        /**
         * Returns true if this path is a shorter path to the same entity than the given path.
         * @param compare The path to compare against.
         * @return is shorter or same
         */
        public boolean shorterThan(PathMetaData compare) {
            if (lineage.isEmpty()) {
                return this.type.equals(compare.type);
            }

            return this.type.equals(compare.type)
                    && this.name.equals(compare.name)
                    && !compare.lineage.isEmpty()
                    && lineage.peek().shorterThan(compare.lineage.peek());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PathMetaData)) {
                return false;
            }

            PathMetaData that = (PathMetaData) o;

            return lineage.equals(that.lineage)
                    && name.equals(that.name)
                    && type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lineage, name, type);
        }

        /**
         * Checks if a given path segment is already within the URL/lineage (forming a cycle).
         * @param other the segment to search for.
         * @return true if the lineage contains the given segment.  False otherwise.
         */
        private boolean lineageContainsType(PathMetaData other) {
            if (this.type.equals(other.type)) {
                return true;
            }

            if (lineage.isEmpty()) {
                return false;
            }

            for (PathMetaData compare : lineage) {
                if (compare.type.equals(other.type)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @param dictionary The entity dictionary
     * @param info Basic service information that cannot be generated
     */
    public SwaggerBuilder(EntityDictionary dictionary, Info info) {
        this.dictionary = dictionary;
        globalResponses = new HashMap<>();
        globalParams = new HashSet<>();
        allClasses = new HashSet<>();
        filterOperators = Sets.newHashSet(
                Operator.IN,
                Operator.NOT,
                Operator.INFIX,
                Operator.PREFIX,
                Operator.POSTFIX,
                Operator.GE,
                Operator.GT,
                Operator.LE,
                Operator.LT,
                Operator.ISNULL,
                Operator.NOTNULL
        );
        swagger = new Swagger();
        swagger.info(info);
    }

    /**
     * Decorates every operation on every path with the given response.
     * @param code The HTTP status code to associate with the response
     * @param response The global response to add to every operation
     * @return the builder
     */
    public SwaggerBuilder withGlobalResponse(int code, Response response) {
        globalResponses.put(code, response);
        return this;
    }

    /**
     * Decorates every path with the given parameter.
     * @param param the parameter to decorate
     * @return the builder
     */
    public SwaggerBuilder withGlobalParameter(Parameter param) {
        globalParams.add(param);
        return this;
    }

    /**
     * The classes for which API paths will be generated.  All paths that include other entities
     * are dropped.
     * @param classes A subset of the entities in the entity dictionary.
     * @return the builder
     */
    public SwaggerBuilder withExplicitClassList(Set<Class<?>> classes) {
        allClasses = new HashSet<>(classes);
        return this;
    }

    /**
     * Assigns a subset of the complete set of filter operations to support for each GET operation.
     * @param ops The subset of filter operations to support.
     * @return the builder
     */
    public SwaggerBuilder withFilterOps(Set<Operator> ops) {
        filterOperators = new HashSet<>(ops);
        return this;
    }

    /**
     * @return the constructed 'Swagger' object
     */
    public Swagger build() {

        /* Used to convert Elide POJOs into Swagger Model objects */
        ModelConverters converters = ModelConverters.getInstance();
        converters.addConverter(new JsonApiModelResolver(dictionary));

        if (allClasses.isEmpty()) {
            allClasses = dictionary.getBindings();
        } else {
            allClasses = Sets.intersection(dictionary.getBindings(), allClasses);
            if (allClasses.isEmpty()) {
                throw new IllegalArgumentException("None of the provided classes are exported by Elide");
            }
        }

        /* Create a Model for each Elide entity */
        Map<String, Model> models = new HashMap<>();
        for (Class<?> clazz : allClasses) {
            models.putAll(converters.readAll(clazz));
        }
        swagger.setDefinitions(models);

        rootClasses =  allClasses.stream()
                .filter(dictionary::isRoot)
                .collect(Collectors.toSet());

        /* Find all the paths starting from the root entities. */
        Set<PathMetaData> pathData =  rootClasses.stream()
                .map(this::find)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        /* Prune the discovered paths to remove redundant elements */
        Set<PathMetaData> toRemove = new HashSet<>();
        for (PathMetaData path : pathData) {
            for (PathMetaData compare : pathData) {

                /*
                 * We don't prune paths that are redundant with root collections to allow both BOTH
                 * root collection urls as well as relationship urls.
                 */
                if (path.equals(compare) || compare.lineage.isEmpty()) {
                    continue;
                }
                if (compare.shorterThan(path)) {
                    toRemove.add(path);
                    break;
                }
            }
        }
        pathData = Sets.difference(pathData, toRemove);

        /* Each path constructs 3 URLs (collection, instance, and relationship) */
        for (PathMetaData pathDatum : pathData) {
            swagger.path(pathDatum.getCollectionUrl(), pathDatum.getCollectionPath());

            swagger.path(pathDatum.getInstanceUrl(), pathDatum.getInstancePath());

            /* We only construct relationship URLs if the entity is not a root collection */
            if (! pathDatum.lineage.isEmpty()) {
                swagger.path(pathDatum.getRelationshipUrl(), pathDatum.getRelationshipPath());
            }
        }

        /* We create Swagger 'tags' for each entity so Swagger UI organizes the paths by entities */
        List<Tag> tags = allClasses.stream()
                .map((clazz) -> dictionary.getJsonAliasFor(clazz))
                .map((alias) -> new Tag().name(alias))
                .collect(Collectors.toList());

        swagger.tags(tags);

        return swagger;
    }

    /**
     * Finds all the paths by navigating the entity relationship graph - starting at the given root entity.
     * Cycles are avoided.
     * @param rootClass the starting node of the graph
     * @return set of discovered paths.
     */
    protected Set<PathMetaData> find(Class<?> rootClass) {
        Queue<PathMetaData> toVisit = new ArrayDeque<>();
        Set<PathMetaData> paths = new HashSet<>();

        toVisit.add(new PathMetaData(rootClass));

        while (! toVisit.isEmpty()) {
            PathMetaData current = toVisit.remove();

            List<String> relationshipNames;
            try {
                relationshipNames = dictionary.getRelationships(current.getType());

            /* If the entity is not bound in the dictionary, skip it */
            } catch (IllegalArgumentException e) {
                continue;
            }

            for (String relationshipName : relationshipNames) {
                Class<?> relationshipClass = dictionary.getParameterizedType(current.getType(), relationshipName);

                PathMetaData next = new PathMetaData(current.getFullLineage(), relationshipName, relationshipClass);

                /* We don't allow cycles AND we only record paths that traverse through the provided subgraph */
                if (current.lineageContainsType(next) || !allClasses.contains(relationshipClass)) {
                    continue;
                }

                toVisit.add(next);
            }
            paths.add(current);
        }
        return paths;
    }

    /**
     * @param swagger Swagger-Core swagger POJO
     * @return Pretty printed 'Swagger' document in JSON.
     */
    public static String getDocument(Swagger swagger) {
        return Json.pretty(swagger);
    }
}
