package com.yahoo.elide.spring.controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.config.JsonApiControllerProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JsonApiControllerTest {

    protected Elide elide = mock(Elide.class);
    protected ElideSettings settings = mock(ElideSettings.class);
    protected ElideResponse response = mock(ElideResponse.class);
    protected Authentication authentication = mock(Authentication.class);
    protected RefreshableElide refreshableElide = mock(RefreshableElide.class);
    protected HttpServletRequest request = mock(HttpServletRequest.class);
    protected JsonApiController testController;

    @BeforeAll
    public void init() {
        when(elide.getElideSettings()).thenReturn(settings);
        when(settings.getBaseUrl()).thenReturn("/json");
        when(response.getBody()).thenReturn("");
        when(response.getResponseCode()).thenReturn(200);
        when(refreshableElide.getElide()).thenReturn(elide);
        when(request.getAttribute(any())).thenReturn("/foo");

        ElideConfigProperties properties = new ElideConfigProperties();
        properties.setJsonApi(new JsonApiControllerProperties());
        testController = new JsonApiController(refreshableElide, properties);
    }

    @Test
    public void testPost() throws Exception {
        when(elide.post(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "foo");
        headers.add("proxy-authorization", "foo");

        MultiValueMap requestParams = new LinkedMultiValueMap();

        Callable<ResponseEntity<String>> asyncHandler = testController.elidePost(
                headers, requestParams, "", request, authentication);

        asyncHandler.call();

        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide).post(any(), any(), any(), any(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void testDelete() throws Exception {
        when(elide.delete(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "foo");
        headers.add("proxy-authorization", "foo");

        MultiValueMap requestParams = new LinkedMultiValueMap();

        Callable<ResponseEntity<String>> asyncHandler = testController.elideDelete(
                headers, requestParams, request, authentication);

        asyncHandler.call();

        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide).delete(any(), any(), any(), any(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void testPatch() throws Exception {
        when(elide.patch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "foo");
        headers.add("proxy-authorization", "foo");

        MultiValueMap requestParams = new LinkedMultiValueMap();

        Callable<ResponseEntity<String>> asyncHandler = testController.elidePatch(
                headers, requestParams, "", request, authentication);

        asyncHandler.call();

        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide).patch(any(), any(), any(), any(), any(), any(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void testGet() throws Exception {
        when(elide.get(any(), any(), any(), any(), any(), any(), any())).thenReturn(response);

        HttpHeaders headers = new HttpHeaders();
        headers.add("authorization", "foo");
        headers.add("proxy-authorization", "foo");

        MultiValueMap requestParams = new LinkedMultiValueMap();

        Callable<ResponseEntity<String>> asyncHandler = testController.elideGet(
                headers, requestParams, request, authentication);

        asyncHandler.call();

        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide).get(any(), any(), any(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }
}
