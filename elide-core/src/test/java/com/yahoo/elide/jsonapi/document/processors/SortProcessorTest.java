/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.document.processors;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.security.User;
import example.Child;
import example.Parent;
import example.Post;
import example.TestCheckMappings;
import org.mockito.Answers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class SortProcessorTest {
    private static final String SORT = "sort";

    private SortProcessor sortProcessor;

    private PersistentResource<Parent> parentRecord1;
    private PersistentResource<Parent> parentRecord2;
    private PersistentResource<Parent> parentRecord3;

    private EntityDictionary dictionary;
    private RequestScope goodUserScope;

    @BeforeMethod
    public void setUp() throws Exception {
        dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);

        sortProcessor = new SortProcessor();
        goodUserScope = new RequestScope(null, new JsonApiDocument(),
                mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS),
                new User(1), null,
                new ElideSettingsBuilder(null)
                        .withAuditLogger(new TestAuditLogger())
                        .withEntityDictionary(dictionary)
                        .withDefaultMaxPageSize(10)
                        .withDefaultPageSize(10)
                        .build());

        //Create objects
        Parent parent1 = newParent(1);
        parent1.setFirstName("Alex");

        Parent parent2 = newParent(2);
        parent2.setFirstName("Zack");

        Parent parent3 = newParent(3);
        parent3.setFirstName("Mike");

        Parent parent4 = newParent(4);
        parent4.setFirstName("Alex");

        //Create Persistent Resources
        parentRecord1 = new PersistentResource<>(parent1, goodUserScope);
        parentRecord2 = new PersistentResource<>(parent2, goodUserScope);
        parentRecord3 = new PersistentResource<>(parent3, goodUserScope);
    }

    @Test
    public void testExecuteSingleSortField() throws Exception {

        // Mock parents
        Set<PersistentResource> parents = new HashSet<>();
        parents.add(parentRecord1);
        parents.add(parentRecord2);
        parents.add(parentRecord3);

        // Mock query params
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(SORT, Collections.singletonList("firstName"));

        // Assert sort order
        List<Resource> givenOrder = Arrays.asList(parentRecord1.toResource(), parentRecord2.toResource(), parentRecord3.toResource());
        List<Resource> expectedOrder = Arrays.asList(parentRecord1.toResource(), parentRecord3.toResource(), parentRecord2.toResource());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(givenOrder));
        sortProcessor.execute(jsonApiDocument, parents, Optional.of(queryParams));
        Collection<Resource> actualOrder = jsonApiDocument.getData().get();

        Assert.assertEquals(actualOrder, expectedOrder,
                "Sort Processor sorted parents in ascending order by name");
    }

    @Test
    public void testExecuteDescendingSortField() throws Exception {

        // Mock parents
        Set<PersistentResource> parents = new HashSet<>();
        parents.add(parentRecord1);
        parents.add(parentRecord2);
        parents.add(parentRecord3);

        // Mock query params
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(SORT, Collections.singletonList("-firstName"));

        // Assert sort order
        List<Resource> givenOrder = Arrays.asList(parentRecord1.toResource(), parentRecord2.toResource(), parentRecord3.toResource());
        List<Resource> expectedOrder = Arrays.asList(parentRecord2.toResource(), parentRecord3.toResource(), parentRecord1.toResource());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(givenOrder));
        sortProcessor.execute(jsonApiDocument, parents, Optional.of(queryParams));
        Collection<Resource> actualOrder = jsonApiDocument.getData().get();

        Assert.assertEquals(actualOrder, expectedOrder,
                "Sort Processor sorted parents in descending order by name when field was preceded by '-'");
    }

    @Test
    public void testExecuteMultipleSortField() throws Exception {
        // Mock posts
        dictionary.bindEntity(Post.class);

        Post post1 = new Post();
        post1.setId(1);
        post1.setTitle("Hello world!");
        post1.setCreated(10);

        Post post2 = new Post();
        post2.setId(2);
        post2.setTitle("Hello world!");
        post2.setCreated(20);

        Post post3 = new Post();
        post3.setId(3);
        post3.setTitle("Goodbye world!");
        post3.setCreated(10);

        //Create Persistent Resources
        PersistentResource<Post> postRecord1 = new PersistentResource<>(post1, goodUserScope);
        PersistentResource<Post> postRecord2 = new PersistentResource<>(post2, goodUserScope);
        PersistentResource<Post> postRecord3 = new PersistentResource<>(post3, goodUserScope);

        // Mock parents
        Set<PersistentResource> posts = new HashSet<>();
        posts.add(postRecord1);
        posts.add(postRecord2);
        posts.add(postRecord3);

        // Mock query params
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(SORT, Arrays.asList("title", "-created"));

        // Assert sort order
        List<Resource> givenOrder = Arrays.asList(postRecord1.toResource(), postRecord2.toResource(), postRecord3.toResource());
        List<Resource> expectedOrder = Arrays.asList(postRecord3.toResource(), postRecord2.toResource(), postRecord1.toResource());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(givenOrder));
        sortProcessor.execute(jsonApiDocument, posts, Optional.of(queryParams));
        Collection<Resource> actualOrder = jsonApiDocument.getData().get();

        Assert.assertEquals(actualOrder, expectedOrder,
                "Sort Processor sorted posts in ascending order by title and resolved ties in descending order by created");
    }

    @Test
    public void testExecuteNonExistentField() throws Exception {

        // Mock parents
        Set<PersistentResource> parents = new HashSet<>();
        parents.add(parentRecord1);
        parents.add(parentRecord2);
        parents.add(parentRecord3);

        // Mock query params
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put(SORT, Collections.singletonList("doesNotExist"));

        // Assert sort order
        List<Resource> givenOrder = Arrays.asList(parentRecord1.toResource(), parentRecord2.toResource(), parentRecord3.toResource());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(givenOrder));
        sortProcessor.execute(jsonApiDocument, parents, Optional.of(queryParams));
        Collection<Resource> actualOrder = jsonApiDocument.getData().get();

        Assert.assertEquals(actualOrder, givenOrder,
                "Sort Processor ignored sort field that didn't exist");
    }

    @Test
    public void testExecuteNoSortParam() throws Exception {

        // Mock parents
        Set<PersistentResource> parents = new HashSet<>();
        parents.add(parentRecord1);
        parents.add(parentRecord2);
        parents.add(parentRecord3);

        // Mock query params
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("notSort", Collections.singletonList("doesNotExist"));

        // Assert sort order
        List<Resource> givenOrder = Arrays.asList(parentRecord1.toResource(), parentRecord2.toResource(), parentRecord3.toResource());

        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        jsonApiDocument.setData(new Data<>(givenOrder));
        sortProcessor.execute(jsonApiDocument, parents, Optional.of(queryParams));
        Collection<Resource> actualOrder = jsonApiDocument.getData().get();

        Assert.assertEquals(actualOrder, givenOrder,
                "Sort Processor did nothing when 'sort' param was not given");
    }

    private static Parent newParent(int id) {
        Parent parent = new Parent();
        parent.setId(id);
        parent.setChildren(new HashSet<>());
        parent.setSpouses(new HashSet<>());
        return parent;
    }

    private static Child newChild(int id) {
        Child child = new Child();
        child.setId(id);
        child.setParents(new HashSet<>());
        return child;
    }
}
