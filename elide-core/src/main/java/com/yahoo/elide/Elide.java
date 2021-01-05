/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.ErrorObjects;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TimeoutException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.extensions.JsonApiPatch;
import com.yahoo.elide.jsonapi.extensions.PatchRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.parser.BaseVisitor;
import com.yahoo.elide.jsonapi.parser.DeleteVisitor;
import com.yahoo.elide.jsonapi.parser.GetVisitor;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.yahoo.elide.jsonapi.parser.PatchVisitor;
import com.yahoo.elide.jsonapi.parser.PostVisitor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

/**
 * REST Entry point handler.
 */
@Slf4j
public class Elide {
    public static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    public static final String JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION =
            "application/vnd.api+json; ext=jsonpatch";

    @Getter private final ElideSettings elideSettings;
    @Getter private final AuditLogger auditLogger;
    @Getter private final DataStore dataStore;
    @Getter private final JsonApiMapper mapper;
    @Getter private final TransactionRegistry transactionRegistry;
    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object
     */
    public Elide(ElideSettings elideSettings) {
        this.elideSettings = elideSettings;
        this.auditLogger = elideSettings.getAuditLogger();
        this.dataStore = new InMemoryDataStore(elideSettings.getDataStore());
        this.dataStore.populateEntityDictionary(elideSettings.getDictionary());
        this.mapper = elideSettings.getMapper();
        this.transactionRegistry = new TransactionRegistry();

        elideSettings.getSerdes().forEach((targetType, serde) -> {
            CoerceUtil.register(targetType, serde);
        });

        registerCustomSerde();
    }

    protected void registerCustomSerde() {
        Injector injector = elideSettings.getDictionary().getInjector();
        Set<Class<?>> classes = registerCustomSerdeScan();

        for (Class<?> clazz : classes) {
            if (!Serde.class.isAssignableFrom(clazz)) {
                log.warn("Skipping Serde registration (not a Serde!): {}", clazz);
                continue;
            }
            Serde serde = (Serde) injector.instantiate(clazz);
            injector.inject(serde);

            ElideTypeConverter converter = clazz.getAnnotation(ElideTypeConverter.class);
            Class baseType = converter.type();
            registerCustomSerde(baseType, serde, converter.name());

            for (Class type : converter.subTypes()) {
                if (!baseType.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Mentioned type " + type
                            + " not subtype of " + baseType);
                }
                registerCustomSerde(type, serde, converter.name());
            }
        }
    }

    protected void registerCustomSerde(Class<?> type, Serde serde, String name) {
        log.info("Registering serde for type : {}", type);
        CoerceUtil.register(type, serde);
        registerCustomSerdeInObjectMapper(type, serde, name);
    }

    protected void registerCustomSerdeInObjectMapper(Class<?> type, Serde serde, String name) {
        ObjectMapper objectMapper = mapper.getObjectMapper();
        objectMapper.registerModule(new SimpleModule(name)
                .addSerializer(type, new JsonSerializer<Object>() {
                    @Override
                    public void serialize(Object obj, JsonGenerator jsonGenerator,
                                          SerializerProvider serializerProvider)
                            throws IOException, JsonProcessingException {
                        jsonGenerator.writeObject(serde.serialize(obj));
                    }
                }));
    }

    protected Set<Class<?>> registerCustomSerdeScan() {
        return ClassScanner.getAnnotatedClasses(ElideTypeConverter.class);
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse get(String baseUrlEndPoint, String path, MultivaluedMap<String, String> queryParams,
                             User opaqueUser, String apiVersion) {
        return get(baseUrlEndPoint, path, queryParams, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse get(String baseUrlEndPoint, String path, MultivaluedMap<String, String> queryParams,
                             User opaqueUser, String apiVersion, UUID requestId) {
        return get(baseUrlEndPoint, path, queryParams, Collections.emptyMap(), opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse get(String baseUrlEndPoint, String path, MultivaluedMap<String, String> queryParams,
                             Map<String, List<String>> requestHeaders, User opaqueUser, String apiVersion,
                             UUID requestId) {
        if (elideSettings.isStrictQueryParams()) {
            try {
                verifyQueryParams(queryParams);
            } catch (BadRequestException e) {
                return buildErrorResponse(e, false);
            }
        }
        return handleRequest(true, opaqueUser, dataStore::beginReadTransaction, requestId, (tx, user) -> {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion, jsonApiDoc,
                    tx, user, queryParams, requestHeaders, requestId, elideSettings);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(path));
            BaseVisitor visitor = new GetVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse post(String baseUrlEndPoint, String path, String jsonApiDocument,
                              User opaqueUser, String apiVersion) {
        return post(baseUrlEndPoint, path, jsonApiDocument, null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse post(String baseUrlEndPoint, String path, String jsonApiDocument,
                              MultivaluedMap<String, String> queryParams,
                              User opaqueUser, String apiVersion, UUID requestId) {
        return post(baseUrlEndPoint, path, jsonApiDocument, queryParams, Collections.emptyMap(),
                    opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse post(String baseUrlEndPoint, String path, String jsonApiDocument,
                              MultivaluedMap<String, String> queryParams, Map<String, List<String>> requestHeaders,
                              User opaqueUser, String apiVersion, UUID requestId) {
        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, (tx, user) -> {
            JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion,
                    jsonApiDoc, tx, user, queryParams, requestHeaders, requestId, elideSettings);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(path));
            BaseVisitor visitor = new PostVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse patch(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument,
                               User opaqueUser, String apiVersion) {
        return patch(baseUrlEndPoint, contentType, accept, path, jsonApiDocument,
                     null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse patch(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument, MultivaluedMap<String, String> queryParams,
                               User opaqueUser, String apiVersion, UUID requestId) {

        return patch(baseUrlEndPoint, contentType, accept, path, jsonApiDocument, queryParams,
                null, opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse patch(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument, MultivaluedMap<String, String> queryParams,
                               Map<String, List<String>> requestHeaders, User opaqueUser,
                               String apiVersion, UUID requestId) {

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiPatch.isPatchExtension(contentType) && JsonApiPatch.isPatchExtension(accept)) {
            handler = (tx, user) -> {
                PatchRequestScope requestScope = new PatchRequestScope(baseUrlEndPoint, path, apiVersion, tx,
                        user, requestId, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder =
                            JsonApiPatch.processJsonPatch(dataStore, path, jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            handler = (tx, user) -> {
                JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);

                RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion, jsonApiDoc,
                        tx, user, queryParams, requestHeaders, requestId, elideSettings);
                requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                        requestScope).parsePath(path));
                BaseVisitor visitor = new PatchVisitor(requestScope);
                return visit(path, requestScope, visitor);
            };
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, handler);
    }

    /**
     * Handle DELETE.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse delete(String baseUrlEndPoint, String path, String jsonApiDocument,
                                User opaqueUser, String apiVersion) {
        return delete(baseUrlEndPoint, path, jsonApiDocument, null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle DELETE.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse delete(String baseUrlEndPoint, String path, String jsonApiDocument,
                                MultivaluedMap<String, String> queryParams,
                                User opaqueUser, String apiVersion, UUID requestId) {
        return delete(baseUrlEndPoint, path, jsonApiDocument, queryParams, Collections.emptyMap(),
                      opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle DELETE.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse delete(String baseUrlEndPoint, String path, String jsonApiDocument,
                                MultivaluedMap<String, String> queryParams,
                                Map<String, List<String>> requestHeaders,
                                User opaqueUser, String apiVersion, UUID requestId) {
        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, (tx, user) -> {
            JsonApiDocument jsonApiDoc = StringUtils.isEmpty(jsonApiDocument)
                    ? new JsonApiDocument()
                    : mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion, jsonApiDoc,
                    tx, user, queryParams, requestHeaders, requestId, elideSettings);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(path));
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    public HandlerResult visit(String path, RequestScope requestScope, BaseVisitor visitor) {
        try {
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(JsonApiParser.parse(path));
            return new HandlerResult(requestScope, responder);
        } catch (RuntimeException e) {
            return new HandlerResult(requestScope, e);
        }
    }

    /**
     * Handle JSON API requests.
     *
     * @param isReadOnly if the transaction is read only
     * @param user the user object from the container
     * @param transaction a transaction supplier
     * @param requestId the Request ID
     * @param handler a function that creates the request scope and request handler
     * @return the response
     */
    protected ElideResponse handleRequest(boolean isReadOnly, User user,
                                          Supplier<DataStoreTransaction> transaction, UUID requestId,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            HandlerResult result = handler.handle(tx, user);
            RequestScope requestScope = result.getRequestScope();
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            Supplier<Pair<Integer, JsonNode>> responder = result.getResponder();
            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (!isReadOnly) {
                requestScope.saveOrCreateObjects();
            }
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            ElideResponse response = buildResponse(responder.get());

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }

            return response;

        } catch (WebApplicationException e) {
            throw e;
        } catch (ForbiddenAccessException e) {
            if (log.isDebugEnabled()) {
                log.debug("{}", e.getLoggedMessage());
            }
            return buildErrorResponse(e, isVerbose);
        } catch (JsonPatchExtensionException e) {
            log.debug("JSON patch extension exception caught", e);
            return buildErrorResponse(e, isVerbose);
        } catch (HttpStatusException e) {
            log.debug("Caught HTTP status exception", e);
            return buildErrorResponse(e, isVerbose);
        } catch (IOException e) {
            log.error("IO Exception uncaught by Elide", e);
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (ParseCancellationException e) {
            log.debug("Parse cancellation exception uncaught by Elide (i.e. invalid URL)", e);
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        } catch (ConstraintViolationException e) {
            log.debug("Constraint violation exception caught", e);
            String message = "Constraint violation";
            final ErrorObjects.ErrorObjectsBuilder errorObjectsBuilder = ErrorObjects.builder();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errorObjectsBuilder.addError()
                    .withDetail(constraintViolation.getMessage());
                final String propertyPathString = constraintViolation.getPropertyPath().toString();
                if (!propertyPathString.isEmpty()) {
                    Map<String, Object> source = new HashMap<>(1);
                    source.put("property", propertyPathString);
                    errorObjectsBuilder.with("source", source);
                }
            }
            return buildErrorResponse(
                new CustomErrorException(HttpStatus.SC_BAD_REQUEST, message, errorObjectsBuilder.build()),
                isVerbose
            );
        } catch (Exception | Error e) {
            if (e instanceof InterruptedException) {
                log.debug("Request Thread interrupted.", e);
                return buildErrorResponse(new TimeoutException(e), isVerbose);
            }
            log.error("Error or exception uncaught by Elide", e);
            throw e;
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected ElideResponse buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        if (error instanceof InternalServerErrorException) {
            log.error("Internal Server Error", error);
        }

        return buildResponse(isVerbose ? error.getVerboseErrorResponse()
                : error.getErrorResponse());
    }

    protected ElideResponse buildResponse(Pair<Integer, JsonNode> response) {
        try {
            JsonNode responseNode = response.getRight();
            Integer responseCode = response.getLeft();
            String body = responseNode == null ? null : mapper.writeJsonApiDocument(responseNode);
            return new ElideResponse(responseCode, body);
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    private void verifyQueryParams(MultivaluedMap<String, String> queryParams) {
        String undefinedKeys = queryParams.keySet()
                        .stream()
                        .filter(Elide::notAValidKey)
                        .collect(Collectors.joining(", "));

        if (!undefinedKeys.isEmpty()) {
            throw new BadRequestException("Found undefined keys in request: " + undefinedKeys);
        }
    }

    private static boolean notAValidKey(String key) {
        boolean validKey = key.equals("sort")
                        || key.startsWith("filter")
                        || (key.startsWith("fields[") && key.endsWith("]"))
                        || key.startsWith("page[")
                        || key.equals(EntityProjectionMaker.INCLUDE);
        return !validKey;
    }

    /**
     * A function that sets up the request handling objects.
     *
     * @param <DataStoreTransaction> the request's transaction
     * @param <User> the request's user
     * @param <HandlerResult> the request handling objects
     */
    @FunctionalInterface
    public interface Handler<DataStoreTransaction, User, HandlerResult> {
        HandlerResult handle(DataStoreTransaction a, User b) throws IOException;
    }

    /**
     * A wrapper to return multiple values, less verbose than Pair.
     */
    protected static class HandlerResult {
        protected RequestScope requestScope;
        protected Supplier<Pair<Integer, JsonNode>> result;
        protected RuntimeException cause;

        protected HandlerResult(RequestScope requestScope, Supplier<Pair<Integer, JsonNode>> result) {
            this.requestScope = requestScope;
            this.result = result;
        }

        public HandlerResult(RequestScope requestScope, RuntimeException cause) {
            this.requestScope = requestScope;
            this.cause = cause;
        }

        public Supplier<Pair<Integer, JsonNode>> getResponder() {
            if (cause != null) {
                throw cause;
            }
            return result;
        }

        public RequestScope getRequestScope() {
            return requestScope;
        }
    }
}
