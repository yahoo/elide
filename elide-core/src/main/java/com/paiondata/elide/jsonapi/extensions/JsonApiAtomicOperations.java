/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.extensions;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.exceptions.HttpStatusException;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.models.Data;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.Operation;
import com.paiondata.elide.jsonapi.models.Operation.OperationCode;
import com.paiondata.elide.jsonapi.models.Operations;
import com.paiondata.elide.jsonapi.models.Ref;
import com.paiondata.elide.jsonapi.models.Resource;
import com.paiondata.elide.jsonapi.models.Result;
import com.paiondata.elide.jsonapi.models.Results;
import com.paiondata.elide.jsonapi.parser.DeleteVisitor;
import com.paiondata.elide.jsonapi.parser.JsonApiParser;
import com.paiondata.elide.jsonapi.parser.PatchVisitor;
import com.paiondata.elide.jsonapi.parser.PostVisitor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * JSON API Atomic Operations extension.
 * @see <a href="https://jsonapi.org/ext/atomic/">Atomic Operations</a>
 */
public class JsonApiAtomicOperations {
    public static final String EXTENSION = "https://jsonapi.org/ext/atomic";

    private static class OperationAction {
        public final Operation operation;

        // Failure
        public HttpStatusException cause;

        // Post Processing
        public boolean isPostProcessing;
        public JsonApiDocument doc;
        public String path;

        public OperationAction(Operation operation) {
            this.operation = operation;
            this.cause = null;
        }

        public void postProcess(JsonApiAtomicOperationsRequestScope requestScope) {
            if (isPostProcessing) {
                try {
                    // Only update relationships
                    clearAllExceptRelationships(doc);
                    PatchVisitor visitor = new PatchVisitor(
                            new JsonApiAtomicOperationsRequestScope(path, doc, requestScope));
                    visitor.visit(JsonApiParser.parse(path));
                } catch (HttpStatusException e) {
                    cause = e;
                    throw e;
                }
            }
        }
    }

    private final List<OperationAction> actions;
    private final String rootUri;

    private static final ObjectNode ERR_NODE_ERR_IN_SUBSEQUENT_OPERATION;
    private static final ObjectNode ERR_NODE_OPERATION_NOT_RUN;

    static {
        ERR_NODE_OPERATION_NOT_RUN = JsonNodeFactory.instance.objectNode();
        ERR_NODE_OPERATION_NOT_RUN.set("detail",
            JsonNodeFactory.instance.textNode("Operation not executed. Terminated by earlier failure."));

        ERR_NODE_ERR_IN_SUBSEQUENT_OPERATION = JsonNodeFactory.instance.objectNode();
        ERR_NODE_ERR_IN_SUBSEQUENT_OPERATION.set("detail",
                JsonNodeFactory.instance.textNode("Subsequent operation failed."));
    }

    /**
     * Process JSON Atomic Operations.
     *
     * @param dataStore the dataStore
     * @param uri the uri
     * @param operationsDoc the operations doc
     * @param requestScope request scope
     * @return pair
     */
    public static Supplier<Pair<Integer, JsonNode>> processAtomicOperations(DataStore dataStore,
            String uri,
            String operationsDoc,
            JsonApiAtomicOperationsRequestScope requestScope) {
        List<Operation> actions;
        try {
            Operations operations = requestScope.getMapper().forAtomicOperations().readDoc(operationsDoc);
            actions = operations.getOperations();
        } catch (InvalidFormatException e) {
            if (OperationCode.class.equals(e.getTargetType())) {
                // Invalid op code results in a format error as it is an enum
                throw new InvalidEntityBodyException(
                        "Invalid Atomic Operations extension operation code:"
                                + e.getValue());
            } else {
                throw new InvalidEntityBodyException(operationsDoc);
            }
        } catch (IOException e) {
            throw new InvalidEntityBodyException(operationsDoc);
        }
        JsonApiAtomicOperations processor = new JsonApiAtomicOperations(dataStore, actions, uri, requestScope);
        return processor.processActions(requestScope);
    }

    /**
     * Constructor.
     *
     * @param dataStore Data Store
     * @param actions List of patch actions
     * @param rootUri root URI
     */
    private JsonApiAtomicOperations(DataStore dataStore,
            List<Operation> actions,
            String rootUri,
            RequestScope requestScope) {
        this.actions = actions.stream().map(OperationAction::new).toList();
        this.rootUri = rootUri;
    }

    /**
     * Process atomic operations actions.
     *
     * @return Pair (return code, JsonNode)
     */
    private Supplier<Pair<Integer, JsonNode>> processActions(JsonApiAtomicOperationsRequestScope requestScope) {
        try {
            List<Supplier<Pair<Integer, JsonApiDocument>>> results = handleActions(requestScope);

            postProcessRelationships(requestScope);

            return () -> {
                try {
                    return Pair.of(HttpStatus.SC_OK,
                            mergeResponse(results, requestScope.getMapper()));
                } catch (HttpStatusException e) {
                    throwErrorResponse();
                    // NOTE: This should never be called. throwErrorResponse should _always_ throw an exception
                    return null;
                }
            };
        } catch (HttpStatusException e) {
            throwErrorResponse();
            // NOTE: This should never be called. throwErrorResponse should _always_ throw an exception
            return () -> null;
        }
    }

    protected String getFullPath(Ref ref, Operation operation) {
        if (ref != null) {
            StringBuilder fullPathBuilder = new StringBuilder();
            if (ref.getType() == null) {
                throw new InvalidEntityBodyException(
                        "Atomic Operations extension ref must specify the type member.");
            }
            fullPathBuilder.append(ref.getType());

            // Only relationship operations or resource update or remove operation should have the id
            if (ref.getRelationship() != null || OperationCode.UPDATE.equals(operation.getOperationCode())
                    || OperationCode.REMOVE.equals(operation.getOperationCode())) {
                if (ref.getId() != null) {
                    fullPathBuilder.append("/");
                    fullPathBuilder.append(ref.getId());
                } else if (ref.getLid() != null) {
                    fullPathBuilder.append("/");
                    fullPathBuilder.append(ref.getLid());
                }
            }
            if (ref.getRelationship() != null) {
                fullPathBuilder.append("/");
                fullPathBuilder.append("relationships");
                fullPathBuilder.append("/");
                fullPathBuilder.append(ref.getRelationship());
            }
            return fullPathBuilder.toString();
        }
        return null;

    }

    /**
     * Performs basic validation that the Operation is specified correctly.
     *
     * @param operation the operation to validate
     */
    private void validateOperation(Operation operation) {
        if (operation == null) {
            throw new InvalidEntityBodyException("Atomic Operations extension operation must be specified.");
        }
        if (operation.getOperationCode() == null) {
            throw new InvalidEntityBodyException(
                    "Atomic Operations extension operation code must be specified.");
        }
        String href = operation.getHref();
        Ref ref = operation.getRef();

        if (href != null && ref != null) {
            throw new InvalidEntityBodyException(
                    "Atomic Operations extension operation cannot contain both ref and href members.");
        }
        if (ref != null && ref.getLid() != null && ref.getId() != null) {
            throw new InvalidEntityBodyException(
                    "Atomic Operations extension ref cannot contain both id and lid members.");
        }
    }

    /**
     * Handle a atomic operations action.
     *
     * @param requestScope outer request scope
     * @return List of responders
     */
    private List<Supplier<Pair<Integer, JsonApiDocument>>> handleActions(
            JsonApiAtomicOperationsRequestScope requestScope) {
        return actions.stream().map(action -> {
            Supplier<Pair<Integer, JsonApiDocument>> result;
            try {
                Operation operation = action.operation;
                validateOperation(operation);
                JsonNode data = operation.getData();
                String href = operation.getHref();
                Ref ref = operation.getRef();
                String fullPath = href;
                boolean refSpecified = ref != null;

                if (fullPath == null) {
                    if (ref == null) {
                        ref = inferRef(requestScope.getMapper(), operation);
                    }
                    fullPath = getFullPath(ref, operation);
                }
                if (fullPath == null) {
                    throw new InvalidEntityBodyException(
                            "Atomic Operations extension operation requires either ref or href members to be specified."
                            );
                } else if (refSpecified && Operation.OperationCode.ADD.equals(operation.getOperationCode())
                        && isResourceOperation(fullPath)) {
                    throw new InvalidEntityBodyException(
                            "Atomic Operations extension add resource operation may only specify the href member.");
                }

                switch (operation.getOperationCode()) {
                    case ADD:
                        result = handleAddOp(fullPath, data, requestScope, action);
                        break;
                    case UPDATE:
                        result = handleUpdateOp(fullPath, data, requestScope, action);
                        break;
                    case REMOVE:
                        result = handleRemoveOp(fullPath, data, requestScope);
                        break;
                    default:
                        throw new InvalidEntityBodyException(
                            "Invalid Atomic Operations extension operation code:"
                                    + operation.getOperationCode());
                }
                return result;
            } catch (HttpStatusException e) {
                action.cause = e;
                throw e;
            }
        }).toList();
    }

    /**
     * Infer ref using the data for operations on add and update resources. The ref cannot be
     * inferred for remove resource and for operations on relationships.
     *
     * @param mapper the json api mapper
     * @param operation the operation
     * @return the ref
     */
    private Ref inferRef(JsonApiMapper mapper, Operation operation) {
        // Attempt to infer the ref from the data
        if (operation.getData() != null && !operation.getData().isArray()) {
            try {
                Resource resource = mapper.forAtomicOperations()
                        .readResource(operation.getData());
                if (resource.getType() != null && isResourceOperation(resource)) {
                    if (OperationCode.ADD.equals(operation.getOperationCode())) {
                        return new Ref(resource.getType(), null, null, null);
                    } else if (OperationCode.UPDATE.equals(operation.getOperationCode())) {
                        if (resource.getLid() != null) {
                            return new Ref(resource.getType(), null, resource.getLid(), null);
                        } else if (resource.getId() != null) {
                            return new Ref(resource.getType(), resource.getId(), null, null);
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                // Do nothing as it will fall back on InvalidEntityBodyException
            }
        }
        return null;
    }

    /**
     * Determines if the operation is on a resource.
     * <p>
     * If there are attributes or relationships present then it is an operation on a
     * resource and not a relationship.
     *
     * @param resource the resource
     * @return true if it is a resource operation and not a relationship operation
     */
    private boolean isResourceOperation(Resource resource) {
        return (resource.getAttributes() != null && !resource.getAttributes().isEmpty())
                || (resource.getRelationships() != null && !resource.getRelationships().isEmpty());
    }

    /**
     * Determines if the operation is on a resource.
     * <p>
     * If the href contains /relationships/ then it is not an operation on a resource.
     *
     * @param href the path
     * @return true if it is a resource operation and not a relationship operation
     */
    private boolean isResourceOperation(String href) {
        return !href.contains("/relationships/");
    }

    /**
     * Add a document via atomic operations extension.
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleAddOp(
            String path, JsonNode dataValue, JsonApiAtomicOperationsRequestScope requestScope, OperationAction action) {
        try {
            JsonApiDocument value = requestScope.getMapper().forAtomicOperations().readData(dataValue);
            Data<Resource> data = value.getData();
            if (data == null || data.get() == null) {
                throw new InvalidEntityBodyException("Expected an entity body but received none.");
            }

            Collection<Resource> resources = data.get();
            if (!path.contains("relationships")) { // Reserved key for relationships
                String id = getSingleResource(resources).getId();

                if (StringUtils.isEmpty(id)) {
                    throw new InvalidEntityBodyException(
                            "Atomic Operations extension requires all objects to have an assigned "
                                    + "ID (temporary or permanent) when assigning relationships.");
                }
                String fullPath = path + "/" + id;
                // Defer relationship updating until the end
                getSingleResource(resources).setRelationships(null);
                // Reparse since we mangle it first
                action.doc = requestScope.getMapper().forAtomicOperations().readData(dataValue);
                action.path = fullPath;
                action.isPostProcessing = true;
            }
            PostVisitor visitor = new PostVisitor(new JsonApiAtomicOperationsRequestScope(path, value, requestScope));
            return visitor.visit(JsonApiParser.parse(path));
        } catch (HttpStatusException e) {
            action.cause = e;
            throw e;
        } catch (IOException e) {
            throw new InvalidEntityBodyException("Could not parse Atomic Operations extension value: " + dataValue);
        }
    }

    /**
     * Update data via atomic operations extension.
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleUpdateOp(
            String path, JsonNode dataValue, JsonApiAtomicOperationsRequestScope requestScope, OperationAction action) {
        try {
            JsonApiDocument value = requestScope.getMapper().forAtomicOperations().readData(dataValue);

            if (!path.contains("relationships")) { // Reserved
                Data<Resource> data = value.getData();
                Collection<Resource> resources = data.get();
                // Defer relationship updating until the end
                getSingleResource(resources).setRelationships(null);
                // Reparse since we mangle it first
                action.doc = requestScope.getMapper().forAtomicOperations().readData(dataValue);
                action.path = path;
                action.isPostProcessing = true;
            }
            // Defer relationship updating until the end
            PatchVisitor visitor = new PatchVisitor(new JsonApiAtomicOperationsRequestScope(path, value, requestScope));
            return visitor.visit(JsonApiParser.parse(path));
        } catch (IOException e) {
            throw new InvalidEntityBodyException("Could not parse Atomic Operations extension value: " + dataValue);
        }
    }

    /**
     * Remove data via atomic operations extension.
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleRemoveOp(String path,
                                                             JsonNode dataValue,
                                                             JsonApiAtomicOperationsRequestScope requestScope) {
        try {
            JsonApiDocument value = requestScope.getMapper().forAtomicOperations().readData(dataValue);
            String fullPath;
            if (path.contains("relationships")) { // Reserved keyword for relationships
                fullPath = path;
            } else {
                Data<Resource> data = value.getData();
                if (data == null || data.get() == null) {
                    fullPath = path;
                } else {
                    Collection<Resource> resources = data.get();
                    String id = getSingleResource(resources).getId();
                    fullPath = path + "/" + id;
                }
            }
            DeleteVisitor visitor = new DeleteVisitor(
                new JsonApiAtomicOperationsRequestScope(path, value, requestScope));
            return visitor.visit(JsonApiParser.parse(fullPath));
        } catch (IOException e) {
            throw new InvalidEntityBodyException("Could not parse Atomic Operations extension value: " + dataValue);
        }
    }

    /**
     * Post-process relationships after all objects for request have been created.
     *
     * This is required since we have no way of determining which object should be created first. That is,
     * in the case of a cyclic relationship between 2 or more newly created objects, some object needs to be created
     * first. In our case, we will create all objects and then add the relationships in memory. Finally, at the end, we
     * rely on the commit of DataStoreTransaction to handle the creation properly.
     *
     * @param requestScope request scope
     */
    private void postProcessRelationships(JsonApiAtomicOperationsRequestScope requestScope) {
        actions.forEach(action -> action.postProcess(requestScope));
    }

    /**
     * Turn an exception into a proper error response from Atomic Operations extension.
     */
    private void throwErrorResponse() {
        ArrayNode errorContainer = getErrorContainer();

        boolean failed = false;
        for (OperationAction action : actions) {
            failed = processAction(errorContainer, failed, action);
        }

        JsonApiAtomicOperationsException failure =
                new JsonApiAtomicOperationsException(HttpStatus.SC_BAD_REQUEST, errorContainer);

        // attach error causes to exception
        for (OperationAction action : actions) {
            if (action.cause != null) {
                failure.addSuppressed(action.cause);
            }
        }

        throw failure;
    }

    private ArrayNode getErrorContainer() {
        return JsonNodeFactory.instance.arrayNode();
    }

    private boolean processAction(ArrayNode errorList, boolean failed, OperationAction action) {
        ObjectNode container = JsonNodeFactory.instance.objectNode();
        ArrayNode errors = JsonNodeFactory.instance.arrayNode();
        container.set("errors", errors);
        errorList.add(container);

        if (action.cause != null) {
            // this is the failed operation
            errors.add(toErrorNode(action.cause.getMessage(), action.cause.getStatus()));
            failed = true;
        } else if (!failed) {
            // this operation succeeded
            errors.add(ERR_NODE_ERR_IN_SUBSEQUENT_OPERATION);
        } else {
            // this operation never ran
            errors.add(ERR_NODE_OPERATION_NOT_RUN);
        }
        return failed;
    }

    protected String getRootUri() {
        return this.rootUri;
    }

    /**
     * Clear all relationships for all resources in document.
     */
    private static void clearAllExceptRelationships(JsonApiDocument doc) {
        Data<Resource> data = doc.getData();
        if (data == null || data.get() == null) {
            return;
        }
        data.get().forEach(JsonApiAtomicOperations::clearAllExceptRelationships);
    }

    /**
     * Clear all properties except the relationships.
     */
    private static void clearAllExceptRelationships(Resource resource) {
        resource.setAttributes(null);
        resource.setLinks(null);
        resource.setMeta(null);
    }

    /**
     * Convert a message and status to an error node.
     *
     */
    private static JsonNode toErrorNode(String detail, Integer status) {
        ObjectNode formattedError = JsonNodeFactory.instance.objectNode();
        formattedError.set("detail", JsonNodeFactory.instance.textNode(Encode.forHtml(detail)));
        if (status != null) {
            formattedError.set("status", JsonNodeFactory.instance.textNode(status.toString()));
        }
        return formattedError;
    }

    /**
     * Merge response documents to create final response.
     */
    private static JsonNode mergeResponse(
            List<Supplier<Pair<Integer, JsonApiDocument>>> results,
            JsonApiMapper mapper
    ) {
        List<Result> list = new ArrayList<>();
        for (Supplier<Pair<Integer, JsonApiDocument>> result : results) {
            JsonApiDocument document = result.get().getRight();
            if (document != null) {
                document.getData().get().stream().map(resource -> new Result(resource, document.getMeta()))
                        .forEach(list::add);
            } else {
                list.add(new Result(null));
            }
        }
        return mapper.getObjectMapper().valueToTree(new Results(list));
    }

    /**
     * Determine whether or not ext = "https://jsonapi.org/ext/atomic" is present in header.
     *
     * @param header the header
     * @return true if it is Atomic Operations
     */
    public static boolean isAtomicOperationsExtension(String header) {
        if (header == null) {
            return false;
        }

        // Find ext="https://jsonapi.org/ext/atomic"
        return Arrays.stream(header.split(";"))
            .map(key -> key.split("="))
            .filter(value -> value.length == 2)
                .anyMatch(value -> value[0].trim().equals("ext")
                        && parameterValues(value[1]).contains(EXTENSION));
    }

    private static Set<String> parameterValues(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 1 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            String unquoted = trimmed.substring(1, trimmed.length() - 1);
            Set<String> result = new HashSet<>();
            Collections.addAll(result, unquoted.split(" "));
            return result;
        }
        return Collections.singleton(trimmed);
    }

    private static Resource getSingleResource(Collection<Resource> resources) {
        if (resources == null || resources.size() != 1) {
            throw new InvalidEntityBodyException("Expected single resource.");
        }
        return IterableUtils.first(resources);
    }
}
