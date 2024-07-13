/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Paginate;
import com.yahoo.elide.annotation.PaginationMode;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.swagger.converter.JsonApiModelResolver;
import com.yahoo.elide.swagger.models.media.AtomicOperations;
import com.yahoo.elide.swagger.models.media.AtomicResults;
import com.yahoo.elide.swagger.models.media.Data;
import com.yahoo.elide.swagger.models.media.Datum;
import com.yahoo.elide.swagger.models.media.Errors;
import com.yahoo.elide.swagger.models.media.Relationship;
import com.google.common.collect.Sets;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Builds a 'OpenAPI' object from Swagger-Core by walking the static Elide
 * entity metadata contained in the 'EntityDictionary'. The 'OpenAPI' object can
 * be used to generate a OpenAPI document.
 */
public class OpenApiBuilder {
    protected EntityDictionary dictionary;
    protected Set<Type<?>> rootClasses;
    protected Set<Type<?>> managedClasses;
    protected OpenAPI openApi;
    protected Map<String, ApiResponse> globalResponses;
    protected Set<Parameter> globalParameters;
    protected Set<Operator> filterOperators;
    protected boolean supportLegacyFilterDialect;
    protected boolean supportRSQLFilterDialect;
    protected String apiVersion = NO_VERSION;
    protected String basePath = null;

    public static final ApiResponse UNAUTHORIZED_RESPONSE = new ApiResponse().description("Unauthorized");
    public static final ApiResponse FORBIDDEN_RESPONSE = new ApiResponse().description("Forbidden");
    public static final ApiResponse NOT_FOUND_RESPONSE = new ApiResponse().description("Not Found");
    public static final ApiResponse REQUEST_TIMEOUT_RESPONSE = new ApiResponse().description("Request Timeout");
    public static final ApiResponse TOO_MANY_REQUESTS_RESPONSE = new ApiResponse().description("Too Many Requests");

    /**
     * Metadata for constructing URLs and OpenAPI 'Path' objects.
     */
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
        private Type<?> type;

        /**
         * The unique identifying instance URL for the path.
         */
        @Getter
        private String url;

        /**
         * Constructs a PathMetaData for a 'root' entity.
         *
         * @param type the 'root' entity type of the first segment of the URL.
         */
        public PathMetaData(Type<?> type) {
            this(new Stack<>(), dictionary.getJsonAliasFor(type), type);
        }

        /**
         * Required argument constructor.
         *
         * @param lineage The lineage of prior path elements.
         * @param name    The relationship of the path element.
         * @param type    The type associated with the relationship.
         */
        public PathMetaData(Stack<PathMetaData> lineage, String name, Type<?> type) {
            this.lineage = lineage;
            this.type = type;
            this.name = name;
            this.url = constructInstanceUrl();
        }

        /**
         * Returns the root type (first collection) of this path.
         *
         * @return The class that represents the root collection of the path.
         */
        public Type<?> getRootType() {
            if (lineage.isEmpty()) {
                return type;
            }

            return lineage.elementAt(0).type;
        }

        /**
         * Returns a URL that represents the collection.
         *
         * @return Something like '/book/{bookId}/authors' or '/publisher'
         */
        public String getCollectionUrl() {
            if (lineage.isEmpty()) {
                return "/" + name;
            }
            return lineage.peek().getUrl() + "/" + name;
        }

        /**
         * Constructs a URL that returns an instance of the entity.
         *
         * @return Something like '/book/{bookId}'
         */
        private String constructInstanceUrl() {
            String typeName = dictionary.getJsonAliasFor(type);
            return getCollectionUrl() + "/{" + typeName + "Id}";
        }

        /**
         * Constructs a URL that returns a relationship collection.
         *
         * @return Something like '/book/{bookId}/relationships/authors'
         * @throws IllegalStateException for errors.
         */
        public String getRelationshipUrl() {
            if (lineage.isEmpty()) {
                throw new IllegalStateException("Root collections don't have relationships");
            }

            PathMetaData prior = lineage.peek();
            String baseUrl = prior.getUrl();

            return baseUrl + "/relationships/" + name;
        }

        @Override
        public String toString() {
            return getUrl();
        }

        /**
         * All Paths are 'tagged' in swagger with the final entity type name in the
         * path. This allows swaggerUI to group the paths by entities.
         *
         * @return the entity type name
         */
        private String getTag() {
            return tagNameOf(type);
        }

        private List<String> getTags() {
            return Collections.singletonList(getTag());
        }

        /**
         * Returns the path parameter for the instance URL.
         *
         * @return the swagger PathParameter for this particular path segment.
         */
        private Parameter getPathParameter() {
            String typeName = dictionary.getJsonAliasFor(type);

            return new PathParameter().name(typeName + "Id").description(typeName + " Identifier")
                    .schema(new StringSchema());
        }

        /**
         * Returns the OpenAPI path for a relationship URL.
         *
         * @return the OpenAPI 'Path' for a relationship URL
         *         (/books/{bookId}/relationships/author).
         * @throws IllegalStateException for errors.
         */
        public PathItem getRelationshipPath() {
            if (lineage.isEmpty()) {
                throw new IllegalStateException("Root collections don't have relationships");
            }

            PathItem path = new PathItem();

            /* The path parameter apply for all operations */
            lineage.stream().forEach(item -> path.addParametersItem(item.getPathParameter()));

            String schemaName = getSchemaName(type);

            ApiResponse okSingularResponse = new ApiResponse().description("Successful response").content(new Content()
                    .addMediaType(JsonApi.MEDIA_TYPE, new MediaType().schema(new Datum(new Relationship(schemaName)))));

            ApiResponse okPluralResponse = new ApiResponse().description("Successful response").content(new Content()
                    .addMediaType(JsonApi.MEDIA_TYPE, new MediaType().schema(new Data(new Relationship(schemaName)))));

            ApiResponse okEmptyResponse = new ApiResponse().description("Successful response");

            Type<?> parentClass = lineage.peek().getType();
            RelationshipType relationshipType = dictionary.getRelationshipType(parentClass, name);

            if (relationshipType.isToMany()) {
                if (canRead(parentClass, name) && canRead(type)) {
                    path.get(new Operation().tags(getTags())
                            .description("Returns the relationship identifiers for " + name)
                            .responses(new ApiResponses().addApiResponse("200", okPluralResponse)));
                }

                if (canUpdate(parentClass, name)) {
                    path.post(new Operation().tags(getTags()).description("Adds items to the relationship " + name)
                            .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                                    new MediaType().schema(new Data(new Relationship(schemaName))))))
                            .responses(new ApiResponses().addApiResponse("201", okPluralResponse)));

                    path.patch(new Operation().tags(getTags()).description("Replaces the relationship " + name)
                            .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                                    new MediaType().schema(new Data(new Relationship(schemaName))))))
                            .responses(new ApiResponses().addApiResponse("204", okEmptyResponse)));

                    path.delete(new Operation().tags(getTags())
                            .description("Deletes items from the relationship " + name)
                            .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                                    new MediaType().schema(new Data(new Relationship(schemaName))))))
                            .responses(new ApiResponses().addApiResponse("204", okEmptyResponse)));
                }
            } else {
                if (canRead(parentClass, name) && canRead(type)) {
                    path.get(new Operation().tags(getTags())
                            .description("Returns the relationship identifiers for " + name)
                            .responses(new ApiResponses().addApiResponse("200", okSingularResponse)));
                }

                if (canUpdate(parentClass, name)) {
                    path.patch(new Operation().tags(getTags()).description("Replaces the relationship " + name)
                            .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                                    new MediaType().schema(new Datum(new Relationship(schemaName))))))
                            .responses(new ApiResponses().addApiResponse("204", okEmptyResponse)));
                }
            }

            if (path.getGet() != null) {
                for (Parameter param : getFilterParameters()) {
                    path.getGet().addParametersItem(param);
                }

                for (Parameter param : getPageParameters(type)) {
                    path.getGet().addParametersItem(param);
                }
            }

            decorateGlobalResponses(path);
            decorateGlobalParameters(path);
            return path;
        }

        /**
         * Returns the OpenAPI Path for a collection URL.
         *
         * @return the OpenAPI 'Path' for a collection URL (/books).
         */
        public PathItem getCollectionPath() {
            String typeName = dictionary.getJsonAliasFor(type);
            String schemaName = getSchemaName(type);
            PathItem path = new PathItem();

            /* The path parameter apply for all operations */
            lineage.stream().forEach(item -> path.addParametersItem(item.getPathParameter()));

            ApiResponse okSingularResponse = new ApiResponse().description("Successful response").content(
                    new Content().addMediaType(JsonApi.MEDIA_TYPE, new MediaType().schema(new Datum(schemaName))));

            ApiResponse okPluralResponse = new ApiResponse().description("Successful response").content(
                    new Content().addMediaType(JsonApi.MEDIA_TYPE, new MediaType().schema(new Data(schemaName))));

            String getDescription;
            String postDescription;
            boolean canPost = false;
            boolean canGet = false;
            if (lineage.isEmpty()) {
                getDescription = "Returns the collection of type " + typeName;
                postDescription = "Creates an item of type " + typeName;

                canGet = canRead(type);
                canPost = canCreate(type);
            } else {
                getDescription = "Returns the relationship " + name;
                postDescription = "Creates an item of type " + typeName + " and adds it to " + name;

                Type<?> parentClass = lineage.peek().getType();
                canGet = canRead(parentClass, name) && canRead(type);
                canPost = canUpdate(parentClass, name) && canCreate(type);
            }

            List<Parameter> parameters = new ArrayList<>();
            parameters.add(getSortParameter());
            parameters.add(getSparseFieldsParameter());
            getIncludeParameter().ifPresent(parameters::add);

            if (canPost) {
                path.post(new Operation().tags(getTags()).description(postDescription)
                        .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                                new MediaType().schema(new Datum(schemaName)))))
                        .responses(new ApiResponses().addApiResponse("201", okSingularResponse)));
            }

            if (canGet) {
                path.get(new Operation().tags(getTags()).description(getDescription).parameters(parameters)
                        .responses(new ApiResponses().addApiResponse("200", okPluralResponse)));

                for (Parameter param : getFilterParameters()) {
                    path.getGet().addParametersItem(param);
                }
                for (Parameter param : getPageParameters(type)) {
                    path.getGet().addParametersItem(param);
                }
            }

            decorateGlobalResponses(path);
            decorateGlobalParameters(path);
            return path;
        }

        /**
         * Returns the OpenAPI Path for an instance URL.
         *
         * @return the OpenAPI 'Path' for a instance URL (/books/{bookID}).
         */
        public PathItem getInstancePath() {
            String typeName = dictionary.getJsonAliasFor(type);
            String schemaName = getSchemaName(type);
            PathItem path = new PathItem();

            /* The path parameter apply for all operations */
            getFullLineage().stream().forEach(item -> path.addParametersItem(item.getPathParameter()));

            ApiResponse okSingularResponse = new ApiResponse().description("Successful response")
                    .content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                            new MediaType().schema(new com.yahoo.elide.swagger.models.media.Datum(schemaName))));

            ApiResponse okEmptyResponse = new ApiResponse().description("Successful response");

            List<Parameter> parameters = new ArrayList<>();
            parameters.add(getSparseFieldsParameter());
            getIncludeParameter().ifPresent(parameters::add);

            boolean canGet = false;
            boolean canPatch = false;
            boolean canDelete = false;

            if (lineage.isEmpty()) {
                // Root entity
                canGet = canReadById(type);
                canPatch = canUpdateById(type);
                canDelete = canDeleteById(type);
            } else {
                // Relationship
                Type<?> parentClass = lineage.peek().getType();
                canGet = canRead(parentClass, name) && canReadById(type);
                canPatch = canUpdate(parentClass, name);
                canDelete = canUpdate(parentClass, name);
            }

            if (canGet) {
                path.get(new Operation().tags(getTags()).description("Returns an instance of type " + typeName)
                        .parameters(parameters)
                        .responses(new ApiResponses().addApiResponse("200", okSingularResponse)));
            }

            if (canPatch) {
                path.patch(new Operation().tags(getTags()).description("Modifies an instance of type " + typeName)
                        .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.MEDIA_TYPE,
                                new MediaType().schema(new Datum(schemaName)))))
                        .responses(new ApiResponses().addApiResponse("204", okEmptyResponse)));
            }

            if (canDelete) {
                path.delete(new Operation().tags(getTags()).description("Deletes an instance of type " + typeName)
                        .responses(new ApiResponses().addApiResponse("204", okEmptyResponse)));
            }

            decorateGlobalResponses(path);
            decorateGlobalParameters(path);
            return path;
        }

        /**
         * Decorates with path parameters that apply to all paths.
         *
         * @param path the path to decorate
         * @return the decorated path
         */
        private PathItem decorateGlobalParameters(PathItem path) {
            globalParameters.forEach(path::addParametersItem);
            return path;
        }

        /**
         * Decorates with responses that apply to all operations for all paths.
         *
         * @param path the path to decorate.
         * @return the decorated path.
         */
        private PathItem decorateGlobalResponses(PathItem path) {
            globalResponses.forEach((code, response) -> {
                if (path.getGet() != null) {
                    path.getGet().getResponses().addApiResponse(code, response);
                }
                if (path.getDelete() != null) {
                    path.getDelete().getResponses().addApiResponse(code, response);
                }
                if (path.getPost() != null) {
                    path.getPost().getResponses().addApiResponse(code, response);
                }
                if (path.getPatch() != null) {
                    path.getPatch().getResponses().addApiResponse(code, response);
                }
            });
            return path;
        }

        /**
         * Returns the sparse fields query parameter.
         *
         * @return the JSON-API 'field' query parameter for some GET operations.
         */
        private Parameter getSparseFieldsParameter() {
            String typeName = dictionary.getJsonAliasFor(type);
            List<String> fieldNames = dictionary.getAllExposedFields(type);

            return new QueryParameter().schema(new ArraySchema().items(new StringSchema()._enum(fieldNames)))
                    .name("fields[" + typeName + "]")
                    .description("Selects the set of " + typeName + " fields that should be returned in the result.")
                    .style(StyleEnum.FORM).explode(false); // style form explode false is collection format csv
        }

        /**
         * Returns the include parameter.
         *
         * @return the JSON-API 'include' query parameter for some GET operations.
         */
        private Optional<Parameter> getIncludeParameter() {
            List<String> relationshipNames = dictionary.getRelationships(type);
            if (relationshipNames.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new QueryParameter()
                    .schema(new ArraySchema().items(new StringSchema()._enum(relationshipNames))).name("include")
                    .description("Selects the set of relationships that should be expanded as a compound document in "
                            + "the result.")
                    .style(StyleEnum.FORM).explode(false)); // style form explode false is collection format csv
        }

        /**
         * Returns the pagination parameter.
         *
         * @param type the type
         * @return the Elide 'page' query parameter for some GET operations.
         */
        private List<Parameter> getPageParameters(Type<?> type) {
            Paginate paginate = dictionary.getAnnotation(type, Paginate.class);
            boolean pageTotalsSupported = true;
            boolean offsetSupported = true;
            boolean cursorSupported = false;
            if (paginate != null) {
                pageTotalsSupported = paginate.countable();
                if (paginate.modes() != null) {
                    offsetSupported = Arrays.stream(paginate.modes()).anyMatch(PaginationMode.OFFSET::equals);
                    cursorSupported = Arrays.stream(paginate.modes()).anyMatch(PaginationMode.CURSOR::equals);
                }
            }
            return getPageParameters(pageTotalsSupported, offsetSupported, cursorSupported);
        }

        /**
         * Returns the pagination parameter.
         *
         * @param pageTotalsSupported support page totals
         * @offsetSupported support offset pagination
         * @cursorSupported support cursor pagination
         * @return the Elide 'page' query parameter for some GET operations.
         */
        private List<Parameter> getPageParameters(boolean pageTotalsSupported, boolean offsetSupported,
                boolean cursorSupported) {
            List<Parameter> params = new ArrayList<>();

            if (offsetSupported) {
                params.add(new QueryParameter().name("page[number]")
                        .description("Number of pages to return.  Can be used with page[size]")
                        .schema(new IntegerSchema()));
            }

            if (offsetSupported || cursorSupported) {
                List<String> usedWith = new ArrayList<>();
                if (offsetSupported) {
                    usedWith.add("page[number]");
                }
                if (cursorSupported) {
                    usedWith.add("page[after]");
                    usedWith.add("page[before]");
                }
                String usedWithString = usedWith.stream().collect(Collectors.joining(", "));
                params.add(new QueryParameter().name("page[size]")
                        .description(
                                "Number of elements per page.  Can be used with " + usedWithString)
                        .schema(new IntegerSchema()));
            }

            if (offsetSupported) {
                params.add(new QueryParameter().name("page[offset]")
                        .description("Offset from 0 to start paginating.  Can be used with page[limit]")
                        .schema(new IntegerSchema()));
                params.add(new QueryParameter().name("page[limit]")
                        .description("Maximum number of items to return.  Can be used with page[offset]")
                        .schema(new IntegerSchema()));
            }

            if (cursorSupported) {
                // Cursor pagination
                params.add(new QueryParameter().name("page[first]")
                        .description("Get first number of items and return the cursors ")
                        .schema(new IntegerSchema()));

                params.add(new QueryParameter().name("page[after]")
                        .description("Get next items after the cursor.  Can be used with page[size] ")
                        .schema(new StringSchema()));

                params.add(new QueryParameter().name("page[last]")
                        .description("Get last number of items and return the cursors ")
                        .schema(new IntegerSchema()));

                params.add(new QueryParameter().name("page[before]")
                        .description("Get previous items before the cursor.  Can be used with page[size] ")
                        .schema(new StringSchema()));
            }

            if (pageTotalsSupported) {
                params.add(new QueryParameter().name("page[totals]")
                        .description("For requesting total pages/records be included in the response page meta data")
                        /*
                         * Swagger UI doesn't support parameters that don't take args today. We'll just
                         * make this a string for now
                         */
                        .schema(new StringSchema()));
            }
            return params;
        }

        /**
         * Returns the sort parameter.
         *
         * @return the JSON-API 'sort' query parameter for some GET operations.
         */
        private Parameter getSortParameter() {
            List<String> filterAttributes = dictionary.getAttributes(type).stream().filter(name -> {
                Type<?> attributeClass = dictionary.getType(type, name);
                return (attributeClass.isPrimitive() || ClassType.STRING_TYPE.isAssignableFrom(attributeClass));
            }).map(name -> Arrays.asList(name, "-" + name)).flatMap(Collection::stream).collect(Collectors.toList());

            filterAttributes.add("id");
            filterAttributes.add("-id");

            return new QueryParameter().name("sort")
                    .schema(new ArraySchema().items(new StringSchema()._enum(filterAttributes)))
                    .description("Sorts the collection on the selected attributes.  A prefix of '-' sorts descending")
                    .style(StyleEnum.FORM).explode(false); // style form explode false is collection format csv
        }

        /**
         * Returns the filter parameter.
         *
         * @return the Elide 'filter' query parameter for some GET operations.
         */
        private List<Parameter> getFilterParameters() {
            String typeName = dictionary.getJsonAliasFor(type);
            List<String> attributeNames = dictionary.getAttributes(type);

            List<Parameter> params = new ArrayList<>();

            if (supportRSQLFilterDialect) {
                /* Add RSQL Disjoint Filter Query Param */
                params.add(new QueryParameter().schema(new StringSchema()).name("filter[" + typeName + "]")
                        .description("Filters the collection of " + typeName + " using a 'disjoint' RSQL expression"));

                if (lineage.isEmpty()) {
                    /* Add RSQL Joined Filter Query Param */
                    params.add(new QueryParameter().schema(new StringSchema()).name("filter").description(
                            "Filters the collection of " + typeName + " using a 'joined' RSQL expression"));
                }
            }

            if (supportLegacyFilterDialect) {
                for (Operator op : filterOperators) {
                    attributeNames.forEach(name -> {
                        Type<?> attributeClass = dictionary.getType(type, name);

                        /* Only filter attributes that can be assigned to strings or primitives */
                        if (attributeClass.isPrimitive() || ClassType.STRING_TYPE.isAssignableFrom(attributeClass)) {
                            params.add(new QueryParameter().schema(new StringSchema())
                                    .name("filter[" + typeName + "." + name + "][" + op.getNotation() + "]")
                                    .description("Filters the collection of " + typeName + " by the attribute " + name
                                            + " " + "using the operator " + op.getNotation()));
                        }
                    });
                }
            }

            return params;
        }

        /**
         * Constructs a new lineage including the current path element.
         *
         * @return ALL of the path segments in the URL including this segment.
         */
        public Stack<PathMetaData> getFullLineage() {
            Stack<PathMetaData> fullLineage = new Stack<>();

            fullLineage.addAll(lineage);
            fullLineage.add(this);
            return fullLineage;
        }

        /**
         * Returns true if this path is a shorter path to the same entity than the given
         * path.
         *
         * @param compare The path to compare against.
         * @return is shorter or same
         */
        public boolean shorterThan(PathMetaData compare) {
            return url.split("/").length < compare.getUrl().split("/").length;
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

            return url.equals(that.getUrl());
        }

        @Override
        public int hashCode() {
            return Objects.hash(lineage, name, type);
        }

        /**
         * Checks if a given path segment is already within the URL/lineage (forming a
         * cycle).
         *
         * @param other the segment to search for.
         * @return true if the lineage contains the given segment. False otherwise.
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
     * Constructor.
     * <p>
     * The customizer can be used to set the OpenAPI SpecVersion.
     *
     * @param dictionary        The entity dictionary.
     * @param openApiCustomizer The OpenAPI customizer.
     */
    public OpenApiBuilder(EntityDictionary dictionary, Consumer<OpenAPI> openApiCustomizer) {
        this.dictionary = dictionary;
        this.supportLegacyFilterDialect = true;
        this.supportRSQLFilterDialect = true;
        this.globalResponses = new HashMap<>();
        this.globalParameters = new HashSet<>();
        this.managedClasses = new HashSet<>();
        this.filterOperators = Sets.newHashSet(Operator.IN, Operator.NOT, Operator.INFIX, Operator.PREFIX,
                Operator.POSTFIX, Operator.GE, Operator.GT, Operator.LE, Operator.LT, Operator.ISNULL,
                Operator.NOTNULL);
        this.openApi = new OpenAPI();
        if (openApiCustomizer != null) {
            openApiCustomizer.accept(this.openApi);
        }
    }

    /**
     * Constructor.
     *
     * @param dictionary The entity dictionary.
     */
    public OpenApiBuilder(EntityDictionary dictionary) {
        this(dictionary, null);
    }

    /**
     * Decorates every operation on every path with the given response.
     *
     * @param code     The HTTP status code to associate with the response
     * @param response The global response to add to every operation
     * @return the builder
     */
    public OpenApiBuilder globalResponse(String code, ApiResponse response) {
        this.globalResponses.put(code, response);
        return this;
    }

    /**
     * Turns on or off the legacy filter dialect.
     *
     * @param enableLegacyFilterDialect Whether or not to enable the legacy filter
     *                            dialect.
     * @return the builder
     */
    public OpenApiBuilder supportLegacyFilterDialect(boolean enableLegacyFilterDialect) {
        this.supportLegacyFilterDialect = enableLegacyFilterDialect;
        return this;
    }

    /**
     * Turns on or off the RSQL filter dialect.
     *
     * @param enableRSQLFilterDialect Whether or not to enable the RSQL filter dialect.
     * @return the builder
     */
    public OpenApiBuilder supportRSQLFilterDialect(boolean enableRSQLFilterDialect) {
        this.supportRSQLFilterDialect = enableRSQLFilterDialect;
        return this;
    }

    /**
     * Decorates every path with the given parameter.
     *
     * @param parameter the parameter to decorate
     * @return the builder
     */
    public OpenApiBuilder globalParameter(Parameter parameter) {
        this.globalParameters.add(parameter);
        return this;
    }

    /**
     * The classes for which API paths will be generated. All paths that include
     * other entities are dropped.
     *
     * @param classes A subset of the entities in the entity dictionary.
     * @return the builder
     */
    public OpenApiBuilder managedClasses(Set<Type<?>> classes) {
        this.managedClasses = new LinkedHashSet<>(classes);
        return this;
    }

    /**
     * Assigns a subset of the complete set of filter operations to support for each
     * GET operation.
     *
     * @param ops The subset of filter operations to support.
     * @return the builder
     */
    public OpenApiBuilder filterOperators(Set<Operator> ops) {
        this.filterOperators = new LinkedHashSet<>(ops);
        return this;
    }

    /**
     * Customize the set of filter operations to support for each
     * GET operation.
     *
     * @param customizer the customizer
     * @return
     */
    public OpenApiBuilder filterOperators(Consumer<Set<Operator>> customizer) {
        customizer.accept(this.filterOperators);
        return this;
    }

    /**
     * Sets the version of the API that is to be documented.
     *
     * @param apiVersion version of the API
     * @return
     */
    public OpenApiBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        if (this.apiVersion == null) {
            this.apiVersion = NO_VERSION;
        }
        return this;
    }

    /**
     * Builds a OpenAPI object.
     * @param openApi Apply configuration on 'OpenAPI' object
     * @return the builder
     */
    public OpenApiBuilder applyTo(OpenAPI openApi) {
        if (managedClasses.isEmpty()) {
            managedClasses = dictionary.getBoundClassesByVersion(this.apiVersion);
        } else {
            managedClasses = Sets.intersection(dictionary.getBoundClassesByVersion(this.apiVersion), managedClasses);
            if (managedClasses.isEmpty()) {
                throw new IllegalArgumentException("None of the provided classes are exported by Elide");
            }
        }
        managedClasses = managedClasses.stream()
                .sorted((left, right) -> left.getSimpleName().compareTo(right.getSimpleName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        /*
         * Create a Model for each Elide entity.
         * Elide entity could be of ClassType or DynamicType.
         * For ClassType, extract the class and pass it to ModelConverters#readAll method.
         * ModelConverters#readAll doesn't support Elide Dynamic Type, so calling the
         * JsonApiResolver#resolve method directly when its not a ClassType.
         */
        ModelConverters converters = ModelConverters.getInstance();
        ModelConverter converter = new JsonApiModelResolver(this.dictionary);
        converters.addConverter(converter);
        for (Type<?> clazz : managedClasses) {
            if (clazz instanceof ClassType<?> classType) {
                converters.readAll(classType.getCls()).forEach(openApi::schema);
            } else {
                ModelConverterContextImpl context = new ModelConverterContextImpl(Arrays.asList(converter));
                context.resolve(new AnnotatedType().type(clazz));
                context.getDefinedModels().forEach(openApi::schema);
            }
        }

        rootClasses = managedClasses.stream()
                .filter(dictionary::isRoot)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        /* Find all the paths starting from the root entities. */
        Set<PathMetaData> pathData =  rootClasses.stream()
                .map(this::find)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        /* Prune the discovered paths to remove redundant elements */
        Set<PathMetaData> toRemove = new HashSet<>();

        pathData.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getType(), p.getName())))
                .values()
                .forEach(pathSet -> {
                    for (PathMetaData path : pathSet) {
                        for (PathMetaData compare : pathSet) {
                            /*
                             * We don't prune paths that are redundant with root collections to allow both BOTH
                             * root collection urls as well as relationship urls.
                             */
                            if (compare.lineage.isEmpty() || path == compare) {
                                continue;
                            }

                            /*
                             * Find the unique shortest path to the node.
                             */
                            if (compare.shorterThan(path)) {
                                toRemove.add(path);
                                break;
                            }
                        }
                    }
                }
        );

        pathData = Sets.difference(pathData, toRemove);

        /* Each path constructs 3 URLs (collection, instance, and relationship) */
        for (PathMetaData pathDatum : pathData) {
            openApi.path(pathOf(pathDatum.getCollectionUrl()), pathDatum.getCollectionPath());

            openApi.path(pathOf(pathDatum.getUrl()), pathDatum.getInstancePath());

            /* We only construct relationship URLs if the entity is not a root collection */
            if (! pathDatum.lineage.isEmpty()) {
                openApi.path(pathOf(pathDatum.getRelationshipUrl()), pathDatum.getRelationshipPath());
            }
        }

        /* We create OpenAPI 'tags' for each entity so Swagger UI organizes the paths by entities */
        managedClasses.stream()
                .map(type -> new Tag().name(tagNameOf(type))
                        .description(EntityDictionary.getEntityDescription(type)))
                .forEach(openApi::addTagsItem);

        /* Atomic operations */
        atomicOperations(openApi);
        return this;
    }

    /**
     * Adds the operations path for JSON API atomic operations.
     *
     * @param openApi the open api
     */
    protected void atomicOperations(OpenAPI openApi) {
        String tagName = tagNameOf("atomic");
        openApi.addTagsItem(new Tag().name(tagName).description("Atomic operations."));
        Map<String, Example> examples = atomicOperationsExamples();
        Map<String, Object> example = null;
        AtomicOperations atomicOperations = new AtomicOperations();
        // Try to determine more specific examples for atomic operations
        Optional<Type<?>> optionalCanCreateType = this.rootClasses.stream().filter(this::canCreate).findFirst();
        if (optionalCanCreateType.isPresent()) {
            Type<?> type = optionalCanCreateType.get();
            String typeName = dictionary.getJsonAliasFor(type);

            Map<String, Object> attributes = dataAttributes(type);
            Map<String, Object> creatingResourcesData = new LinkedHashMap<>();
            creatingResourcesData.put("type", typeName);
            creatingResourcesData.put("lid", "string");
            creatingResourcesData.put("attributes", attributes);
            Map<String, Object> creatingResourcesOp = new LinkedHashMap<>();
            creatingResourcesOp.put("op", "add");
            creatingResourcesOp.put("href", "/" + typeName);
            creatingResourcesOp.put("data", creatingResourcesData);
            creatingResourcesOp.put("meta", new LinkedHashMap<>());
            Map<String, Object> creatingResources = Map.of("atomic:operations", List.of(creatingResourcesOp));
            example = creatingResources;
            // Replace generic example
            examples.put("Creating Resources",
                    new Example().value(creatingResources).description(CREATING_RESOURCES_DESCRIPTION));
        }
        Optional<Type<?>> optionalCanUpdateType = this.rootClasses.stream().filter(this::canUpdate).findFirst();
        if (optionalCanUpdateType.isPresent()) {
            Type<?> type = optionalCanUpdateType.get();
            String typeName = dictionary.getJsonAliasFor(type);

            Map<String, Object> attributes = dataAttributes(type);
            Map<String, Object> updatingResourcesData = new LinkedHashMap<>();
            updatingResourcesData.put("type", typeName);
            updatingResourcesData.put("id", "string");
            updatingResourcesData.put("attributes", attributes);
            Map<String, Object> updatingResourcesOp = new LinkedHashMap<>();
            updatingResourcesOp.put("op", "update");
            updatingResourcesOp.put("data", updatingResourcesData);
            updatingResourcesOp.put("meta", new LinkedHashMap<>());
            Map<String, Object> updatingResources = Map.of("atomic:operations", List.of(updatingResourcesOp));
            if (example == null) {
                example = updatingResources;
            }
            // Replace generic example
            examples.put("Updating Resources",
                    new Example().value(updatingResources).description(UPDATING_RESOURCES_DESCRIPTION));
        }
        Optional<Type<?>> optionalCanDeleteType = this.rootClasses.stream().filter(this::canDelete).findFirst();
        if (optionalCanDeleteType.isPresent()) {
            Type<?> type = optionalCanDeleteType.get();
            String typeName = dictionary.getJsonAliasFor(type);

            Map<String, Object> deletingResourcesRef = new LinkedHashMap<>();
            deletingResourcesRef.put("type", typeName);
            deletingResourcesRef.put("id", "string");
            Map<String, Object> deletingResourcesOp = new LinkedHashMap<>();
            deletingResourcesOp.put("op", "remove");
            deletingResourcesOp.put("ref", deletingResourcesRef);
            Map<String, Object> deletingResources = Map.of("atomic:operations", List.of(deletingResourcesOp));
            if (example == null) {
                example = deletingResources;
            }
            // Replace generic example
            examples.put("Deleting Resources",
                    new Example().value(deletingResources).description(DELETING_RESOURCES_DESCRIPTION));
        }
        // Must call setExample() and cannot call example() as it calls toString on the
        // example
        if (example != null) {
            atomicOperations.setExample(example);
        }

        // Create that path for /operations
        PathItem operations = new PathItem();
        operations.post(new Operation().tags(List.of(tagName))
                .description("Perform atomic operations")
                .requestBody(new RequestBody().content(new Content().addMediaType(JsonApi.AtomicOperations.MEDIA_TYPE,
                        new MediaType().schema(atomicOperations).examples(examples))))
                .responses(new ApiResponses()
                        .addApiResponse("200",
                                new ApiResponse().description("Successful response")
                                        .content(new Content().addMediaType(JsonApi.AtomicOperations.MEDIA_TYPE,
                                                new MediaType().schema(new AtomicResults()))))
                        .addApiResponse("400",
                                new ApiResponse().description("Bad request")
                                        .content(new Content().addMediaType(JsonApi.AtomicOperations.MEDIA_TYPE,
                                                new MediaType().schema(new Errors()))))
                        .addApiResponse("423",
                                new ApiResponse().description("Locked")
                                        .content(new Content().addMediaType(JsonApi.AtomicOperations.MEDIA_TYPE,
                                                new MediaType().schema(new Errors()))))));
        openApi.path(pathOf("/operations"), operations);
    }

    protected Map<String, Object> dataAttributes(Type<?> type) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        List<String> attributeNames = dictionary.getAttributes(type);
        for (String attributeName : attributeNames) {
            Type<?> attributeClass = dictionary.getType(type, attributeName);
            if (ClassType.STRING_TYPE.isAssignableFrom(attributeClass)) {
                attributes.put(attributeName, "string");
            } else if (ClassType.NUMBER_TYPE.isAssignableFrom(attributeClass)) {
                attributes.put(attributeName, 0);
            } else if (ClassType.BOOLEAN_TYPE.isAssignableFrom(attributeClass)) {
                attributes.put(attributeName, true);
            }
        }
        return attributes;
    }

    protected String tagNameOf(Type<?> type) {
        String tagName = dictionary.getJsonAliasFor(type);
        return tagNameOf(tagName);
    }

    protected String tagNameOf(String tagName) {
        if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
            tagName = "v" + apiVersion + "/" + tagName;
        }
        return tagName;
    }

    protected String pathOf(String url) {
        if (basePath == null || "/".equals(basePath)) {
            return url;
        }
        return basePath + url;
    }

    public OpenApiBuilder basePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    /**
     * Builds a OpenAPI object.
     * @return the constructed 'OpenAPI' object
     */
    public OpenAPI build() {
        applyTo(this.openApi);
        return this.openApi;
    }

    /**
     * Finds all the paths by navigating the entity relationship graph - starting at
     * the given root entity. Cycles are avoided.
     *
     * @param rootClass the starting node of the graph
     * @return set of discovered paths.
     */
    protected Set<PathMetaData> find(Type<?> rootClass) {
        Queue<PathMetaData> toVisit = new ArrayDeque<>();
        Set<PathMetaData> paths = new LinkedHashSet<>();

        toVisit.add(new PathMetaData(rootClass));

        while (!toVisit.isEmpty()) {
            PathMetaData current = toVisit.remove();

            List<String> relationshipNames;
            try {
                relationshipNames = dictionary.getRelationships(current.getType());

                /* If the entity is not bound in the dictionary, skip it */
            } catch (IllegalArgumentException e) {
                continue;
            }

            for (String relationshipName : relationshipNames) {
                Type<?> relationshipClass = dictionary.getParameterizedType(current.getType(), relationshipName);

                PathMetaData next = new PathMetaData(current.getFullLineage(), relationshipName, relationshipClass);

                /*
                 * We don't allow cycles AND we only record paths that traverse through the
                 * provided subgraph
                 */
                if (current.lineageContainsType(next) || !managedClasses.contains(relationshipClass)) {
                    continue;
                }

                toVisit.add(next);
            }
            paths.add(current);
        }
        return paths;
    }

    protected String getSchemaName(Type<?> type) {
        // Should be the same as JsonApiModelResolver#getSchemaName
        String schemaName = dictionary.getJsonAliasFor(type);
        String apiVersion = EntityDictionary.getModelVersion(type);
        if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
            schemaName = "v" + this.apiVersion + "_" + schemaName;
        }
        return schemaName;
    }
    protected boolean isNone(String permission) {
        return "Prefab.Role.None".equalsIgnoreCase(permission) || Role.NONE_ROLE.equalsIgnoreCase(permission);
    }

    protected boolean canCreate(Type<?> type) {
        return !isNone(getCreatePermission(type));
    }

    protected boolean canRead(Type<?> type) {
        return !isNone(getReadPermission(type));
    }

    protected boolean canUpdate(Type<?> type) {
        return !isNone(getUpdatePermission(type));
    }

    protected boolean canDelete(Type<?> type) {
        return !isNone(getDeletePermission(type));
    }

    protected boolean canReadById(Type<?> type) {
        boolean excluded = dictionary.getIdAnnotation(type, Exclude.class) != null;
        return !(isNone(getReadPermission(type)) || excluded);
    }

    protected boolean canUpdateById(Type<?> type) {
        boolean excluded = dictionary.getIdAnnotation(type, Exclude.class) != null;
        return !(isNone(getUpdatePermission(type)) || excluded);
    }

    protected boolean canDeleteById(Type<?> type) {
        boolean excluded = dictionary.getIdAnnotation(type, Exclude.class) != null;
        return !(isNone(getDeletePermission(type)) || excluded);
    }

    protected boolean canCreate(Type<?> type, String field) {
        return !isNone(getCreatePermission(type, field));
    }

    protected boolean canRead(Type<?> type, String field) {
        return !isNone(getReadPermission(type, field));
    }

    protected boolean canUpdate(Type<?> type, String field) {
        return !isNone(getUpdatePermission(type, field));
    }

    protected boolean canDelete(Type<?> type, String field) {
        return !isNone(getDeletePermission(type, field));
    }

    /**
     * Get the calculated {@link CreatePermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the create permissions for an entity
     */
    protected String getCreatePermission(Type<?> clazz) {
        return getPermission(clazz, CreatePermission.class);
    }

    /**
     * Get the calculated {@link ReadPermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the read permissions for an entity
     */
    protected String getReadPermission(Type<?> clazz) {
        return getPermission(clazz, ReadPermission.class);
    }

    /**
     * Get the calculated {@link UpdatePermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the update permissions for an entity
     */
    protected String getUpdatePermission(Type<?> clazz) {
        return getPermission(clazz, UpdatePermission.class);
    }

    /**
     * Get the calculated {@link DeletePermission} value for the entity.
     *
     * @param clazz the entity class
     * @return the delete permissions for an entity
     */
    protected String getDeletePermission(Type<?> clazz) {
        return getPermission(clazz, DeletePermission.class);
    }

    /**
     * Get the calculated {@link CreatePermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the create permissions for the relationship
     */
    protected String getCreatePermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, CreatePermission.class);
    }

    /**
     * Get the calculated {@link ReadPermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the read permissions for the relationship
     */
    protected String getReadPermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, ReadPermission.class);
    }

    /**
     * Get the calculated {@link UpdatePermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the update permissions for the relationship
     */
    protected String getUpdatePermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, UpdatePermission.class);
    }

    /**
     * Get the calculated {@link DeletePermission} value for the relationship.
     *
     * @param clazz the entity class
     * @param field the field to inspect
     * @return the delete permissions for the relationship
     */
    protected String getDeletePermission(Type<?> clazz, String field) {
        return getPermission(clazz, field, DeletePermission.class);
    }

    protected String getPermission(Type<?> clazz, Class<? extends Annotation> permission) {
        ParseTree parseTree = dictionary.getPermissionsForClass(clazz, permission);
        if (parseTree != null) {
            return parseTree.getText();
        }
        return null;
    }

    protected String getPermission(Type<?> clazz, String field, Class<? extends Annotation> permission) {
        ParseTree parseTree = dictionary.getPermissionsForField(clazz, field, permission);
        if (parseTree != null) {
            return parseTree.getText();
        }
        return null;
    }

    protected Map<String, Example> atomicOperationsExamples() {
        Map<String, Example> examples = new LinkedHashMap<>();

        // Creating Resources
        Map<String, Object> creatingResources = exampleCreatingResources();
        examples.put("Creating Resources",
                new Example().value(creatingResources).description(CREATING_RESOURCES_DESCRIPTION));

        // Updating Resources
        Map<String, Object> updatingResources = exampleUpdatingResources();
        examples.put("Updating Resources",
                new Example().value(updatingResources).description(UPDATING_RESOURCES_DESCRIPTION));

        // Deleting Resources
        Map<String, Object> deletingResources = exampleDeletingResources();
        examples.put("Deleting Resources",
                new Example().value(deletingResources).description(DELETING_RESOURCES_DESCRIPTION));

        // Updating To-One Relationships
        Map<String, Object> updatingToOneRelationships = exampleUpdatingToOneRelationships();
        examples.put("Updating To-One Relationships",
                new Example().value(updatingToOneRelationships).description(UPDATING_TO_ONE_RELATIONSHIPS_DESCRIPTION));

        // Deleting To-One Relationships
        Map<String, Object> deletingToOneRelationships = exampleDeletingToOneRelationships();
        examples.put("Deleting To-One Relationships",
                new Example().value(deletingToOneRelationships).description(DELETING_TO_ONE_RELATIONSHIPS_DESCRIPTION));

        // Creating To-Many Relationships
        Map<String, Object> creatingToManyRelationships = exampleCreatingToManyRelationships();
        examples.put("Creating To-Many Relationships", new Example().value(creatingToManyRelationships)
                .description(CREATING_TO_MANY_RELATIONSHIPS_DESCRIPTION));

        // Updating To-Many Relationships
        Map<String, Object> updatingToManyRelationships = exampleUpdatingToManyRelationships();
        examples.put("Updating To-Many Relationships", new Example().value(updatingToManyRelationships)
                .description(UPDATING_TO_MANY_RELATIONSHIPS_DESCRIPTION));

        // Deleting To-Many Relationships
        Map<String, Object> deletingToManyRelationships = exampleDeletingToManyRelationships();
        examples.put("Deleting To-Many Relationships", new Example().value(deletingToManyRelationships)
                .description(DELETING_TO_MANY_RELATIONSHIPS_DESCRIPTION));

        return examples;
    }

    protected Map<String, Object> exampleDeletingToManyRelationships() {
        Map<String, Object> deletingToManyRelationshipsOp = new LinkedHashMap<>();
        Map<String, Object> deletingToManyRelationshipsRef = new LinkedHashMap<>();
        deletingToManyRelationshipsRef.put("type", "articles");
        deletingToManyRelationshipsRef.put("id", "1");
        deletingToManyRelationshipsRef.put("relationship", "comments");
        Map<String, Object> deletingToManyRelationshipsData1 = new LinkedHashMap<>();
        deletingToManyRelationshipsData1.put("type", "comments");
        deletingToManyRelationshipsData1.put("id", "12");
        Map<String, Object> deletingToManyRelationshipsData2 = new LinkedHashMap<>();
        deletingToManyRelationshipsData2.put("type", "comments");
        deletingToManyRelationshipsData2.put("id", "13");
        deletingToManyRelationshipsOp.put("op", "remove");
        deletingToManyRelationshipsOp.put("ref", deletingToManyRelationshipsRef);
        deletingToManyRelationshipsOp.put("data",
                List.of(deletingToManyRelationshipsData1, deletingToManyRelationshipsData2));
        Map<String, Object> deletingToManyRelationships = Map.of("atomic:operations",
                List.of(deletingToManyRelationshipsOp));
        return deletingToManyRelationships;
    }

    protected Map<String, Object> exampleUpdatingToManyRelationships() {
        Map<String, Object> updatingToManyRelationshipsOp = new LinkedHashMap<>();
        Map<String, Object> updatingToManyRelationshipsRef = new LinkedHashMap<>();
        updatingToManyRelationshipsRef.put("type", "articles");
        updatingToManyRelationshipsRef.put("id", "1");
        updatingToManyRelationshipsRef.put("relationship", "tags");
        Map<String, Object> updatingToManyRelationshipsData1 = new LinkedHashMap<>();
        updatingToManyRelationshipsData1.put("type", "tags");
        updatingToManyRelationshipsData1.put("id", "2");
        Map<String, Object> updatingToManyRelationshipsData2 = new LinkedHashMap<>();
        updatingToManyRelationshipsData2.put("type", "tags");
        updatingToManyRelationshipsData2.put("id", "3");
        updatingToManyRelationshipsOp.put("op", "update");
        updatingToManyRelationshipsOp.put("ref", updatingToManyRelationshipsRef);
        updatingToManyRelationshipsOp.put("data",
                List.of(updatingToManyRelationshipsData1, updatingToManyRelationshipsData2));
        Map<String, Object> updatingToManyRelationships = Map.of("atomic:operations",
                List.of(updatingToManyRelationshipsOp));
        return updatingToManyRelationships;
    }

    protected Map<String, Object> exampleCreatingToManyRelationships() {
        Map<String, Object> creatingToManyRelationshipsOp = new LinkedHashMap<>();
        Map<String, Object> creatingToManyRelationshipsRef = new LinkedHashMap<>();
        creatingToManyRelationshipsRef.put("type", "articles");
        creatingToManyRelationshipsRef.put("id", "1");
        creatingToManyRelationshipsRef.put("relationship", "comments");
        Map<String, Object> creatingToManyRelationshipsData = new LinkedHashMap<>();
        creatingToManyRelationshipsData.put("type", "comments");
        creatingToManyRelationshipsData.put("id", "1");
        creatingToManyRelationshipsOp.put("op", "add");
        creatingToManyRelationshipsOp.put("ref", creatingToManyRelationshipsRef);
        creatingToManyRelationshipsOp.put("data", List.of(creatingToManyRelationshipsData));
        Map<String, Object> creatingToManyRelationships = Map.of("atomic:operations",
                List.of(creatingToManyRelationshipsOp));
        return creatingToManyRelationships;
    }

    protected Map<String, Object> exampleDeletingToOneRelationships() {
        Map<String, Object> deletingToOneRelationshipsOp = new LinkedHashMap<>();
        Map<String, Object> deletingToOneRelationshipsRef = new LinkedHashMap<>();
        deletingToOneRelationshipsRef.put("type", "articles");
        deletingToOneRelationshipsRef.put("id", "13");
        deletingToOneRelationshipsRef.put("relationship", "author");
        deletingToOneRelationshipsOp.put("op", "update");
        deletingToOneRelationshipsOp.put("ref", deletingToOneRelationshipsRef);
        deletingToOneRelationshipsOp.put("data", null);
        Map<String, Object> deletingToOneRelationships = Map.of("atomic:operations",
                List.of(deletingToOneRelationshipsOp));
        return deletingToOneRelationships;
    }

    protected Map<String, Object> exampleUpdatingToOneRelationships() {
        Map<String, Object> updatingToOneRelationshipsOp = new LinkedHashMap<>();
        Map<String, Object> updatingToOneRelationshipsRef = new LinkedHashMap<>();
        updatingToOneRelationshipsRef.put("type", "articles");
        updatingToOneRelationshipsRef.put("id", "13");
        updatingToOneRelationshipsRef.put("relationship", "author");
        Map<String, Object> updatingToOneRelationshipsData = new LinkedHashMap<>();
        updatingToOneRelationshipsData.put("type", "people");
        updatingToOneRelationshipsData.put("id", "9");
        updatingToOneRelationshipsOp.put("op", "update");
        updatingToOneRelationshipsOp.put("ref", updatingToOneRelationshipsRef);
        updatingToOneRelationshipsOp.put("data", updatingToOneRelationshipsData);
        Map<String, Object> updatingToOneRelationships = Map.of("atomic:operations",
                List.of(updatingToOneRelationshipsOp));
        return updatingToOneRelationships;
    }

    protected Map<String, Object> exampleDeletingResources() {
        Map<String, Object> deletingResourcesOp = new LinkedHashMap<>();
        Map<String, Object> deletingResourcesRef = new LinkedHashMap<>();
        deletingResourcesRef.put("type", "articles");
        deletingResourcesRef.put("id", "13");
        deletingResourcesOp.put("op", "remove");
        deletingResourcesOp.put("ref", deletingResourcesRef);
        Map<String, Object> deletingResources = Map.of("atomic:operations",
                List.of(deletingResourcesOp));
        return deletingResources;
    }

    protected Map<String, Object> exampleUpdatingResources() {
        Map<String, Object> updatingResourcesOp = new LinkedHashMap<>();
        Map<String, Object> updatingResourcesData = new LinkedHashMap<>();
        updatingResourcesData.put("type", "articles");
        updatingResourcesData.put("id", "13");
        updatingResourcesData.put("attributes", Map.of("title", "Title"));
        updatingResourcesOp.put("op", "update");
        updatingResourcesOp.put("data", updatingResourcesData);
        Map<String, Object> updatingResources = Map.of("atomic:operations",
                List.of(updatingResourcesOp));
        return updatingResources;
    }

    protected Map<String, Object> exampleCreatingResources() {
        Map<String, Object> creatingResourcesOp = new LinkedHashMap<>();
        Map<String, Object> creatingResourcesData = new LinkedHashMap<>();
        creatingResourcesData.put("type", "articles");
        creatingResourcesData.put("attributes", Map.of("title", "Title"));
        creatingResourcesOp.put("op", "add");
        creatingResourcesOp.put("href", "/blogPosts");
        creatingResourcesOp.put("data", creatingResourcesData);
        Map<String, Object> creatingResources = Map.of("atomic:operations",
                List.of(creatingResourcesOp));
        return creatingResources;
    }

    private static final String CREATING_RESOURCES_DESCRIPTION = ""
            + "To create a resource, the operation MUST include an op code of \"add\" "
            + "as well as a resource object as data. "
            + "The resource object MUST contain at least a type member. An operation that creates a "
            + "resource MAY target a resource collection through the operation's href member.";
    private static final String UPDATING_RESOURCES_DESCRIPTION = ""
            + "To update a resource, the operation MUST include an op code of \"update\" "
            + "as well as a resource object as data. "
            + "An operation that updates a resource MAY target that resource through the "
            + "operation's ref or href members, but not both.";
    private static final String DELETING_RESOURCES_DESCRIPTION = ""
            + "To delete a resource, the operation MUST include an op code of \"remove\". "
            + "An operation that deletes a resource MUST target that resource through the "
            + "operation's ref or href members, but not both.";
    private static final String UPDATING_TO_ONE_RELATIONSHIPS_DESCRIPTION = ""
            + "To assign a to-one relationship, the operation MUST include an op code of \"update\" "
            + "as well as a resource identifier object as data. "
            + "An operation that updates a resource's to-one relationship "
            + "MUST target that relationship through the operation's ref or href members, but not both.";
    private static final String DELETING_TO_ONE_RELATIONSHIPS_DESCRIPTION = ""
            + "To clear a to-one relationship, the operation MUST include an op code of \"update\" "
            + "as well as setting the data to \"null\". "
            + "An operation that updates a resource's to-one relationship "
            + "MUST target that relationship through the operation's ref or href members, but not both.";
    private static final String CREATING_TO_MANY_RELATIONSHIPS_DESCRIPTION = ""
            + "To add members to a to-many relationship, the operation MUST include an op code of \"add\" "
            + "as well as an array of resource identifier objects as data." + " "
            + "An operation that updates a resource's to-many relationship MUST target that "
            + "relationship through the operation's ref or href members, but not both.";
    private static final String UPDATING_TO_MANY_RELATIONSHIPS_DESCRIPTION = ""
            + "To replace all the members of a to-many relationship, "
            + "the operation MUST include an op code of \"update\" "
            + "as well as an array of resource identifier objects as data. "
            + "An operation that updates a resource's to-many relationship MUST target that "
            + "relationship through the operation's ref or href members, but not both.";
    private static final String DELETING_TO_MANY_RELATIONSHIPS_DESCRIPTION = ""
            + "To remove members from a to-many relationship, the operation MUST include an op code of \"remove\" "
            + "as well as an array of resource identifier objects as data. "
            + "An operation that updates a resource's to-many relationship MUST target that "
            + "relationship through the operation's ref or href members, but not both.";
}
