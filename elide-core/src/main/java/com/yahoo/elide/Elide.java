/*
 * Copyright 2018, Yahoo Inc.
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
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.exceptions.ErrorObjects;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
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
import com.fasterxml.jackson.core.JacksonException;
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
    @Getter private final ErrorMapper errorMapper;
    @Getter private final TransactionRegistry transactionRegistry;
    @Getter private final ClassScanner scanner;
    private boolean initialized = false;

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     */
    public Elide(
            ElideSettings elideSettings
    ) {
        this(elideSettings, new TransactionRegistry(), elideSettings.getDictionary().getScanner(), false);
    }

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     * @param transactionRegistry Global transaction state.
     */
    public Elide(
            ElideSettings elideSettings,
            TransactionRegistry transactionRegistry
    ) {
        this(elideSettings, transactionRegistry, elideSettings.getDictionary().getScanner(), false);
    }

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     * @param transactionRegistry Global transaction state.
     * @param scanner Scans classes for Elide annotations.
     * @param doScans Perform scans now.
     */
    public Elide(
            ElideSettings elideSettings,
            TransactionRegistry transactionRegistry,
            ClassScanner scanner,
            boolean doScans
    ) {
        this.elideSettings = elideSettings;
        this.scanner = scanner;
        this.auditLogger = elideSettings.getAuditLogger();
        this.dataStore = new InMemoryDataStore(elideSettings.getDataStore());
        this.mapper = elideSettings.getMapper();
        this.errorMapper = elideSettings.getErrorMapper();
        this.transactionRegistry = transactionRegistry;

        if (doScans) {
            doScans();
        }
    }

    /**
     * Scans & binds Elide models, scans for security check definitions, serde definitions, life cycle hooks
     * and more.  Any dependency injection required by objects found from scans must be performed prior to this call.
     */
    public void doScans() {
        if (! initialized) {
            elideSettings.getSerdes().forEach((type, serde) -> registerCustomSerde(type, serde, type.getSimpleName()));
            registerCustomSerde();

            //Scan for security checks prior to populating data stores in case they need them.
            elideSettings.getDictionary().scanForSecurityChecks();

            this.dataStore.populateEntityDictionary(elideSettings.getDictionary());
            initialized = true;
        }
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
        return scanner.getAnnotatedClasses(ElideTypeConverter.class);
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
                        user, requestId, queryParams, requestHeaders, elideSettings);
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
            Supplier<Pair<Integer, JsonApiDocument>> responder = visitor.visit(JsonApiParser.parse(path));
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
     * @param <T> The response type (JsonNode or JsonApiDocument)
     * @return the response
     */
    protected <T> ElideResponse handleRequest(boolean isReadOnly, User user,
                                          Supplier<DataStoreTransaction> transaction, UUID requestId,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            HandlerResult result = handler.handle(tx, user);
            RequestScope requestScope = result.getRequestScope();
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            Supplier<Pair<Integer, T>> responder = result.getResponder();
            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.runQueuedPreFlushTriggers();
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
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return response;
        } catch (IOException e) {
            return handleNonRuntimeException(e, isVerbose);
        } catch (RuntimeException e) {
            return handleRuntimeException(e, isVerbose);
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

    private ElideResponse handleNonRuntimeException(Exception error, boolean isVerbose) {
        CustomErrorException mappedException = mapError(error);
        if (mappedException != null) {
            return buildErrorResponse(mappedException, isVerbose);
        }

        if (error instanceof JacksonException) {
            JacksonException jacksonException = (JacksonException) error;
            String message = (jacksonException.getLocation() != null
                    && jacksonException.getLocation().getSourceRef() != null)
                    ? error.getMessage() //This will leak Java class info if the location isn't known.
                    : jacksonException.getOriginalMessage();

            return buildErrorResponse(new BadRequestException(message), isVerbose);
        }

        if (error instanceof IOException) {
            log.error("IO Exception uncaught by Elide", error);
            return buildErrorResponse(new TransactionException(error), isVerbose);
        }

        log.error("Error or exception uncaught by Elide", error);
        throw new RuntimeException(error);
    }

    private ElideResponse handleRuntimeException(RuntimeException error, boolean isVerbose) {
        CustomErrorException mappedException = mapError(error);

        if (mappedException != null) {
            return buildErrorResponse(mappedException, isVerbose);
        }

        if (error instanceof WebApplicationException) {
            throw error;
        }

        if (error instanceof ForbiddenAccessException) {
            ForbiddenAccessException e = (ForbiddenAccessException) error;
            if (log.isDebugEnabled()) {
                log.debug("{}", e.getLoggedMessage());
            }
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof JsonPatchExtensionException) {
            JsonPatchExtensionException e = (JsonPatchExtensionException) error;
            log.debug("JSON patch extension exception caught", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof HttpStatusException) {
            HttpStatusException e = (HttpStatusException) error;
            log.debug("Caught HTTP status exception", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof ParseCancellationException) {
            ParseCancellationException e = (ParseCancellationException) error;
            log.debug("Parse cancellation exception uncaught by Elide (i.e. invalid URL)", e);
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        }

        if (error instanceof ConstraintViolationException) {
            ConstraintViolationException e = (ConstraintViolationException) error;
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
        }

        log.error("Error or exception uncaught by Elide", error);
        throw new RuntimeException(error);
    }

    public CustomErrorException mapError(Exception error) {
        if (errorMapper != null) {
            log.trace("Attempting to map unknown exception of type {}", error.getClass());
            CustomErrorException customizedError = errorMapper.map(error);

            if (customizedError != null) {
                log.debug("Successfully mapped exception from type {} to {}",
                        error.getClass(), customizedError.getClass());
                return customizedError;
            } else {
                log.debug("No error mapping present for {}", error.getClass());
            }
        }

        return null;
    }

    protected <T> ElideResponse buildResponse(Pair<Integer, T> response) {
        try {
            T responseNode = response.getRight();
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
     * @param <T> Response type.
     */
    protected static class HandlerResult<T> {
        protected RequestScope requestScope;
        protected Supplier<Pair<Integer, T>> result;
        protected RuntimeException cause;

        protected HandlerResult(RequestScope requestScope, Supplier<Pair<Integer, T>> result) {
            this.requestScope = requestScope;
            this.result = result;
        }

        public HandlerResult(RequestScope requestScope, RuntimeException cause) {
            this.requestScope = requestScope;
            this.cause = cause;
        }

        public Supplier<Pair<Integer, T>> getResponder() {
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
