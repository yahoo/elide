/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.extensions;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.exceptions.HttpStatusException;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.JsonPatchExtensionException;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.models.Data;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.Patch;
import com.paiondata.elide.jsonapi.models.Resource;
import com.paiondata.elide.jsonapi.parser.DeleteVisitor;
import com.paiondata.elide.jsonapi.parser.JsonApiParser;
import com.paiondata.elide.jsonapi.parser.PatchVisitor;
import com.paiondata.elide.jsonapi.parser.PostVisitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JSON API JSON patch extension.
 * @see <a href="http://jsonapi.org/extensions/jsonpatch/">JSON Patch Extension</a>
 */
public class JsonApiJsonPatch {
    public static final String EXTENSION = "jsonpatch";

    private static class PatchAction {
        public final Patch patch;

        // Failure
        public HttpStatusException cause;

        // Post Processing
        public boolean isPostProcessing;
        public JsonApiDocument doc;
        public String path;

        public PatchAction(Patch patch) {
            this.patch = patch;
            this.cause = null;
        }

        public void postProcess(JsonApiJsonPatchRequestScope requestScope) {
            if (isPostProcessing) {
                try {
                    // Only update relationships
                    clearAllExceptRelationships(doc);
                    PatchVisitor visitor = new PatchVisitor(new JsonApiJsonPatchRequestScope(path, doc, requestScope));
                    visitor.visit(JsonApiParser.parse(path));
                } catch (HttpStatusException e) {
                    cause = e;
                    throw e;
                }
            }
        }
    }

    private final List<PatchAction> actions;
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
     * Process json patch.
     *
     * @param dataStore the dataStore
     * @param uri the uri
     * @param patchDoc the patch doc
     * @param requestScope request scope
     * @return pair
     */
    public static Supplier<Pair<Integer, JsonNode>> processJsonPatch(DataStore dataStore,
            String uri,
            String patchDoc,
            JsonApiJsonPatchRequestScope requestScope) {
        List<Patch> actions;
        try {
            actions = requestScope.getMapper().forJsonPatch().readDoc(patchDoc);
        } catch (IOException e) {
            throw new InvalidEntityBodyException(patchDoc);
        }
        JsonApiJsonPatch processor = new JsonApiJsonPatch(dataStore, actions, uri, requestScope);
        return processor.processActions(requestScope);
    }

    /**
     * Constructor.
     *
     * @param dataStore Data Store
     * @param actions List of patch actions
     * @param rootUri root URI
     */
    private JsonApiJsonPatch(DataStore dataStore,
            List<Patch> actions,
            String rootUri,
            RequestScope requestScope) {
        this.actions = actions.stream().map(PatchAction::new).collect(Collectors.toList());
        this.rootUri = rootUri;
    }

    /**
     * Process json patch actions.
     *
     * @return Pair (return code, JsonNode)
     */
    private Supplier<Pair<Integer, JsonNode>> processActions(JsonApiJsonPatchRequestScope requestScope) {
        try {
            List<Supplier<Pair<Integer, JsonApiDocument>>> results = handleActions(requestScope);

            postProcessRelationships(requestScope);

            return () -> {
                try {
                    return Pair.of(HttpStatus.SC_OK, mergeResponse(results, requestScope.getMapper()));
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

    /**
     * Handle a patch action.
     *
     * @param requestScope outer request scope
     * @return List of responders
     */
    private List<Supplier<Pair<Integer, JsonApiDocument>>> handleActions(JsonApiJsonPatchRequestScope requestScope) {
        return actions.stream().map(action -> {
            Supplier<Pair<Integer, JsonApiDocument>> result;
            try {
                String path = action.patch.getPath();
                if (path == null) {
                    throw new InvalidEntityBodyException("Patch extension requires all objects "
                            + "to have an assigned path");
                }
                String[] combined = ArrayUtils.addAll(rootUri.split("/"), action.patch.getPath().split("/"));
                String fullPath = String.join("/", combined).replace("/-", "");

                Patch.Operation operation = action.patch.getOperation();
                if (operation == null) {
                    throw new InvalidEntityBodyException("Patch extension operation cannot be null.");
                }
                switch (operation) {
                    case ADD:
                        result = handleAddOp(fullPath, action.patch.getValue(), requestScope, action);
                        break;
                    case REPLACE:
                        result = handleReplaceOp(fullPath, action.patch.getValue(), requestScope, action);
                        break;
                    case REMOVE:
                        result = handleRemoveOp(fullPath, action.patch.getValue(), requestScope);
                        break;
                    default:
                        throw new InvalidEntityBodyException(
                            "Could not parse patch extension operation:" + action.patch.getOperation());
                }
                return result;
            } catch (HttpStatusException e) {
                action.cause = e;
                throw e;
            }
        }).collect(Collectors.toList());
    }

    /**
     * Add a document via patch extension.
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleAddOp(
            String path, JsonNode patchValue, JsonApiJsonPatchRequestScope requestScope, PatchAction action) {
        try {
            JsonApiDocument value = requestScope.getMapper().forJsonPatch().readValue(patchValue);
            Data<Resource> data = value.getData();
            if (data == null || data.get() == null) {
                throw new InvalidEntityBodyException("Expected an entity body but received none.");
            }

            Collection<Resource> resources = data.get();
            if (!path.contains("relationships")) { // Reserved key for relationships
                String id = getSingleResource(resources).getId();

                if (StringUtils.isEmpty(id)) {
                    throw new InvalidEntityBodyException("Patch extension requires all objects to have an assigned "
                            + "ID (temporary or permanent) when assigning relationships.");
                }
                String fullPath = path + "/" + id;
                // Defer relationship updating until the end
                getSingleResource(resources).setRelationships(null);
                // Reparse since we mangle it first
                action.doc = requestScope.getMapper().forJsonPatch().readValue(patchValue);
                action.path = fullPath;
                action.isPostProcessing = true;
            }
            PostVisitor visitor = new PostVisitor(new JsonApiJsonPatchRequestScope(path, value, requestScope));
            return visitor.visit(JsonApiParser.parse(path));
        } catch (HttpStatusException e) {
            action.cause = e;
            throw e;
        } catch (IOException e) {
            throw new InvalidEntityBodyException("Could not parse patch extension value: " + patchValue);
        }
    }

    /**
     * Replace data via patch extension.
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleReplaceOp(
            String path, JsonNode patchVal, JsonApiJsonPatchRequestScope requestScope, PatchAction action) {
        try {
            JsonApiDocument value = requestScope.getMapper().forJsonPatch().readValue(patchVal);

            if (!path.contains("relationships")) { // Reserved
                Data<Resource> data = value.getData();
                Collection<Resource> resources = data.get();
                // Defer relationship updating until the end
                getSingleResource(resources).setRelationships(null);
                // Reparse since we mangle it first
                action.doc = requestScope.getMapper().forJsonPatch().readValue(patchVal);
                action.path = path;
                action.isPostProcessing = true;
            }
            // Defer relationship updating until the end
            PatchVisitor visitor = new PatchVisitor(new JsonApiJsonPatchRequestScope(path, value, requestScope));
            return visitor.visit(JsonApiParser.parse(path));
        } catch (IOException e) {
            throw new InvalidEntityBodyException("Could not parse patch extension value: " + patchVal);
        }
    }

    /**
     * Remove data via patch extension.
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleRemoveOp(String path,
                                                             JsonNode patchValue,
                                                             JsonApiJsonPatchRequestScope requestScope) {
        try {
            JsonApiDocument value = requestScope.getMapper().forJsonPatch().readValue(patchValue);
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
                new JsonApiJsonPatchRequestScope(path, value, requestScope));
            return visitor.visit(JsonApiParser.parse(fullPath));
        } catch (IOException e) {
            throw new InvalidEntityBodyException("Could not parse patch extension value: " + patchValue);
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
    private void postProcessRelationships(JsonApiJsonPatchRequestScope requestScope) {
        actions.forEach(action -> action.postProcess(requestScope));
    }

    /**
     * Turn an exception into a proper error response from patch extension.
     */
    private void throwErrorResponse() {
        ArrayNode errorContainer = getErrorContainer();

        boolean failed = false;
        for (PatchAction action : actions) {
            failed = processAction(errorContainer, failed, action);
        }

        JsonPatchExtensionException failure =
                new JsonPatchExtensionException(HttpStatus.SC_BAD_REQUEST, errorContainer);

        // attach error causes to exception
        for (PatchAction action : actions) {
            if (action.cause != null) {
                failure.addSuppressed(action.cause);
            }
        }

        throw failure;
    }

    private ArrayNode getErrorContainer() {
        return JsonNodeFactory.instance.arrayNode();
    }

    private boolean processAction(ArrayNode errorList, boolean failed, PatchAction action) {
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

    /**
     * Clear all relationships for all resources in document.
     */
    private static void clearAllExceptRelationships(JsonApiDocument doc) {
        Data<Resource> data = doc.getData();
        if (data == null || data.get() == null) {
            return;
        }
        data.get().forEach(JsonApiJsonPatch::clearAllExceptRelationships);
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
        ArrayNode list = JsonNodeFactory.instance.arrayNode();
        for (Supplier<Pair<Integer, JsonApiDocument>> result : results) {
            JsonNode node;
            JsonApiDocument document = result.get().getRight();
            if (document == null) {
                node = JsonNodeFactory.instance.objectNode().set("data", null);
            } else {
                node = mapper.toJsonObject(document);
            }

            list.add(node);
        }
        return list;
    }

    /**
     * Determine whether or not ext = jsonpatch is present in header.
     *
     * @param header the header
     * @return True if Json patch, false otherwise
     */
    public static boolean isPatchExtension(String header) {
        if (header == null) {
            return false;
        }

        // Find ext=jsonpatch
        return Arrays.stream(header.split(";"))
            .map(key -> key.split("="))
            .filter(value -> value.length == 2)
            .anyMatch(value -> value[0].trim().equals("ext") && parameterValues(value[1]).contains(EXTENSION));
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
