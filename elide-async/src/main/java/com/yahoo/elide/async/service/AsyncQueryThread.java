package com.yahoo.elide.async.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.request.EntityProjection;

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable thread for executing the query provided in Async Query.
 * It will also update the query status and result object at different
 * stages of execution.
 */
@Slf4j
public class AsyncQueryThread implements Runnable {

	private String query;
	private QueryType queryType;
	private RequestScope scope;
	private Elide elide;
	private QueryRunner runner;
	private UUID id;

    public AsyncQueryThread(String query, QueryType queryType, RequestScope scope, Elide elide, QueryRunner runner, UUID id){
        log.debug("New Async Query thread created");
        this.query = query;
        this.queryType = queryType;
        this.scope = scope;
        this.elide = elide;
        this.runner = runner;
        this.id = id;
    }

    @Override
    public void run() {
        processQuery();
    }

    /**
     * This is the main method which processes the Async Query request, executes the query and updates
     * values for AsyncQuery and AsyncQueryResult models accordingly.
     */
    protected void processQuery() {
        try {
            // Change async query to processing
            updateAsyncQueryStatus(QueryStatus.PROCESSING, id);
            //Just doing sleep for async testing
            Thread.sleep(60000);
            ElideResponse response = null;
            log.debug("query: {}", query);
            log.debug("queryType: {}", queryType);
            Principal principal = ((Principal) scope.getUser().getOpaqueUser());
            log.debug("Principal name: {}", principal.getName());
            AsyncQuery asyncQuery;
            AsyncQueryResult asyncQueryResult;
            if (queryType.equals(QueryType.JSONAPI_V1_0)) {
                MultivaluedMap<String, String> queryParams = getQueryParams(query);
                response = elide.get(getPath(query), queryParams, scope.getUser().getOpaqueUser());
                log.debug("JSONAPI_V1_0 getResponseCode: {}", response.getResponseCode());
                log.debug("JSONAPI_V1_0 getBody: {}", response.getBody());
            }
            else if (queryType.equals(QueryType.GRAPHQL_V1_0)) {
                response = runner.run(query, principal);
                log.debug("GRAPHQL_V1_0 getResponseCode: {}", response.getResponseCode());
                log.debug("GRAPHQL_V1_0 getBody: {}", response.getBody());
            }
            // if 200 - response code then Change async query to complete else change to Failure
            if (response.getResponseCode() == 200) {
                asyncQuery = updateAsyncQueryStatus(QueryStatus.COMPLETE, id);
            } else {
                asyncQuery = updateAsyncQueryStatus(QueryStatus.FAILURE, id);
            }

            // Create AsyncQueryResult entry for AsyncQuery
            asyncQueryResult = createAsyncQueryResult(response.getResponseCode(), response.getBody(), asyncQuery, id);

            // Add queryResult object to query object
            updateAsyncQueryStatus(asyncQueryResult, id);

        } catch (IOException e) {
            log.error("IOException: {}", e.getMessage());
        } catch (URISyntaxException e) {
            log.error("URISyntaxException: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("InterruptedException: {}", e.getMessage());
        }  catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
        }
    }

    /**
     * This method parses the url and gets the query params and adds them into a MultivaluedMap
     * to be used by underlying Elide.get method
     * @param query query from the Async request
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return MultivaluedMap with query parameters
     */
    protected MultivaluedMap<String, String> getQueryParams(String query) throws URISyntaxException {
        URIBuilder uri;
        uri = new URIBuilder(query);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
        for (NameValuePair queryParam : uri.getQueryParams()) {
            queryParams.add(queryParam.getName(), queryParam.getValue());
        }
        log.debug("QueryParams: {}", queryParams);
        return queryParams;
    }

    /**
     * This method parses the url and gets the query params and retrieves path
     * to be used by underlying Elide.get method
     * @param query query from the Async request
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return Path extracted from URI
     */
    protected String getPath(String query) throws URISyntaxException {
        URIBuilder uri;
        uri = new URIBuilder(query);
        log.debug("Retrieving path from query");
        return uri.getPath();
    }

    /**
     * This method updates the model for AsyncQuery with passed status value.
     * @param status new status based on the enum QueryStatus
     * @param asyncQueryId queryId from asyncQuery request
     * @throws IOException IOException from DataStoreTransaction
     * @return AsyncQuery Object
     */
    protected AsyncQuery updateAsyncQueryStatus(QueryStatus status, UUID asyncQueryId) throws IOException {
        log.debug("Updating AsyncQuery status to {}", status);
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();
        EntityProjection asyncQueryCollection = EntityProjection.builder()
            .type(AsyncQuery.class)
            .build();
        AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
        query.setQueryStatus(status);
        tx.save(query, scope);
        tx.commit(scope);
        tx.flush(scope);
        tx.close();
        return query;
    }

    /**
     * This method updates the model for AsyncQuery with result object,
     * @param asyncQueryResult AsyncQueryResult object to be associated with the AsyncQuery object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @throws IOException IOException from DataStoreTransaction
     */
    protected void updateAsyncQueryStatus(AsyncQueryResult asyncQueryResult, UUID asyncQueryId) throws IOException {
        log.debug("Updating AsyncQueryResult to {}", asyncQueryResult);
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();
        EntityProjection asyncQueryCollection = EntityProjection.builder()
            .type(AsyncQuery.class)
            .build();
        AsyncQuery query = (AsyncQuery) tx.loadObject(asyncQueryCollection, asyncQueryId, scope);
        query.setResult(asyncQueryResult);
        tx.save(query, scope);
        tx.commit(scope);
        tx.flush(scope);
        tx.close();
    }

    /**
     * This method persists the model for AsyncQueryResult
     * @param status ElideResponse status from AsyncQuery
     * @param responseBody ElideResponse responseBody from AsyncQuery
     * @param asyncQuery AsyncQuery object to be associated with the AsyncQueryResult object
     * @param asyncQueryId UUID of the AsyncQuery to be associated with the AsyncQueryResult object
     * @throws IOException IOException from DataStoreTransaction
     * @return AsyncQueryResult Object
     */
    protected AsyncQueryResult createAsyncQueryResult(Integer status, String responseBody, AsyncQuery asyncQuery, UUID asyncQueryId) throws IOException {
		log.debug("Adding AsyncQueryResult entry");
        DataStoreTransaction tx = elide.getDataStore().beginTransaction();
        AsyncQueryResult asyncQueryResult = new AsyncQueryResult();
        asyncQueryResult.setStatus(status);
        asyncQueryResult.setResponseBody(responseBody);
        asyncQueryResult.setContentLength(responseBody.length());
        asyncQueryResult.setId(asyncQueryId);
        asyncQueryResult.setQuery(asyncQuery);
        tx.createObject(asyncQueryResult, scope);
        tx.save(asyncQueryResult, scope);
        tx.commit(scope);
        tx.flush(scope);
        tx.close();
        return asyncQueryResult;
    }
}
