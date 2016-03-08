/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.security.User;
import example.blog.Blogger;
import example.blog.Comment;
import example.blog.Post;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class RelationshipTerminalStateTest {
    public abstract class RelationshipTerminalStateBaseTest {
        EntityDictionary dictionary = new EntityDictionary();
        JsonApiMapper mapper = new JsonApiMapper(dictionary);
        AuditLogger logger = mock(AuditLogger.class);
        DataStoreTransaction tx;

        Post post;
        PersistentResource<Post> resource;
        StateContext state;

        @BeforeMethod
        public void setupDictionary() {
            dictionary.bindEntity(Blogger.class);
            dictionary.bindEntity(Post.class);
            dictionary.bindEntity(Comment.class);

            tx = mock(DataStoreTransaction.class);
        }

        protected User getSecurityUser(Blogger blogger) {
            return new User(blogger);
        }

        protected void mockPostInTX(Post p) {
            when(tx.loadObject(Post.class, p.getId())).thenReturn(p);
        }

        protected void mockCommentInTx(Comment c) {
            when(tx.loadObject(Comment.class, c.getId())).thenReturn(c);
        }

        protected void mockBloggerInTx(Blogger b) {
            when(tx.loadObject(Blogger.class, b.getId())).thenReturn(b);
        }

        protected Blogger getBlogger(long id) {
            Blogger blogger = new Blogger();
            blogger.setId(id);
            return blogger;
        }

        protected Post createPost(long id, Blogger blogger) {
            Post post = new Post();
            post.setId(id);
            post.setAuthor(blogger);
            post.setPublished(true);
            return post;
        }

        protected Comment createComment(long id, Post post, Blogger author, String text) {
            Comment comment = new Comment();
            comment.setId(id);
            comment.setPost(post);
            comment.setAuthor(author);
            comment.setText(text);
            return comment;
        }

        protected void setRequestDatumForUser(long userId, String type, String id) {
            Data<Resource> data = null;
            if (type != null) {
                data = createDataForToOne(type, id);
            }
            JsonApiDocument document = new JsonApiDocument(data);
            User securityUser = getSecurityUser(getBlogger(userId));
            RequestScope scope = new RequestScope(document, tx, securityUser, dictionary, mapper, logger);

            resource = new PersistentResource<>(post, scope);
            state = new StateContext(null, scope);
        }

        protected void setRequestDataForUser(long userId, String type, String... ids) {
            Data<Resource> data = createDataForToMany(type, ids);

            JsonApiDocument document = new JsonApiDocument(data);
            User securityUser = getSecurityUser(getBlogger(userId));
            RequestScope scope = new RequestScope(document, tx, securityUser, dictionary, mapper, logger);

            resource = new PersistentResource<>(post, scope);
            state = new StateContext(null, scope);
        }

        protected Data<Resource> createDataForToOne(String entity, String id) {
            return new Data<>(new Resource(entity, id));
        }

        protected Data<Resource> createDataForToMany(String type, String[] ids) {
            Set<Resource> resources = new HashSet<>();
            for (String id : ids) {
                resources.add(new Resource(type, id));
            }
            return new Data<>(resources);
        }

    }

    /**************************************
     * Operations on toOne relationships. *
     **************************************/
    // Relationship visible + Entity visible
    public class ToOneVisibleRelationshipVisibleEntityTest extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Post first = createPost(1, author);
            first.setTitle("First Draft");

            Post second = createPost(2, author);
            second.setTitle("Second Draft");

            first.setNextRevision(second);
            second.setPreviousRevision(first);

            mockPostInTX(first);
            mockPostInTX(second);
            post = first;
        }
        @Test
        public void testReadRelationship() {
            // user#1 GET /posts/1/relationships/nextRevision
            setRequestDatumForUser(1, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "nextRevision");
            Pair<Integer, JsonNode> response = terminal.handleGet(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 200);
        }
        @Test
        public void testSetRelationship() {
            // user#1 PATCH /posts/1/relationships/nextRevision
            setRequestDatumForUser(1, "post", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "nextRevision");
            Pair<Integer, JsonNode> response = terminal.handlePatch(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
        }
        @Test
        public void testClearRelationship() {
            // user#1 PATCH /posts/1/relationships/nextRevision
            setRequestDatumForUser(1, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "nextRevision");
            Pair<Integer, JsonNode> response = terminal.handlePatch(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
        }
    }

    // Relationship visible + Entity not visible
    public class ToOneVisibleRelationshipHiddenEntityTest extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Post first = createPost(1, author);
            first.setTitle("First Draft");

            Post second = createPost(2, author);
            second.setTitle("Second Draft");

            Blogger friend = getBlogger(2);
            Post prequel = createPost(3, friend);
            prequel.setTitle("Prequel");

            first.setNextRevision(second);
            second.setPreviousRevision(first);

            prequel.setNextRevision(first);
            first.setPreviousRevision(prequel);


            mockPostInTX(first);
            mockPostInTX(second);
            mockPostInTX(prequel);
            post = first;
        }
        @Test
        public void testReadRelationship() {
            // user#2 GET /posts/1/relationships/nextRevision
            setRequestDatumForUser(2, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "nextRevision");
            Pair<Integer, JsonNode> response = terminal.handleGet(state).get();

            Assert.assertEquals(response.getLeft().intValue(), 200);

        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testSetRelationship() {
            // TODO: a test that won't throw ForbiddenAccess
            // user#2 PATCH /posts/1/relationships/nextRevision
            setRequestDatumForUser(2, "post", "3");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "nextRevision");
            terminal.handlePatch(state).get(); // not permitted because we can't modify post#2
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testFailSetSameValueToRelationship() {
            // user#2 PATCH /posts/1/relationships/nextRevision
            setRequestDatumForUser(2, "post", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "nextRevision");
            terminal.handlePatch(state).get();
        }
        @Test
        public void testClearRelationship() {
            // user#2 PATCH /posts/1/relationships/previousRevision
            setRequestDatumForUser(2, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "previousRevision");
            Pair<Integer, JsonNode> response = terminal.handlePatch(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertNull(post.getPreviousRevision());
        }
    }

    // Relationship not visible + Entity visible
    public class ToOneHiddenRelationshipVisibleEntityTest extends  RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Post first = createPost(1, author);
            first.setTitle("First Draft");

            Blogger friend = getBlogger(2);
            Post second = createPost(2, friend);
            second.setTitle("Superawesome post");

            first.setImpetus(second);

            mockPostInTX(first);
            mockPostInTX(second);
            post = first;
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testReadRelationship() {
            // user#2 GET /posts/1/relationships/impetus
            setRequestDatumForUser(2, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            Pair<Integer, JsonNode> response = terminal.handleGet(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testSetRelationship() {
            // user#2 PATCH /posts/1/relationships/impetus
            setRequestDatumForUser(2, "post", "1");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            terminal.handlePatch(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testFailResetRelationship() {
            // user#2 PATCH /posts/1/relationships/impetus
            setRequestDatumForUser(2, "post", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            terminal.handlePatch(state).get();

        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testClearRelationship() {
            // user#2 PATCH /posts/1/relationships/impetus
            setRequestDatumForUser(2, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            terminal.handlePatch(state).get();
        }
    }

    // Relationship not visible + Entity not visible
    public class ToOneHiddenRelationshipHiddenEntity extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Post first = createPost(1, author);
            first.setTitle("Required introduction");

            Blogger friend = getBlogger(2);
            Post second = createPost(2, author);
            second.setTitle("Superawesome post");
            second.setPublished(false);

            first.setImpetus(second);

            mockPostInTX(first);
            mockPostInTX(second);
            post = first;
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testReadRelationship() {
            // user#2 GET /posts/1/relationships/impetus
            setRequestDatumForUser(2, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            terminal.handleGet(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testSetRelationship() {
            // user#2 PATCH /posts/1/relationships/impetus
            setRequestDatumForUser(2, "post", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            terminal.handlePatch(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testClearRelationship() {
            // user#2 PATCH /posts/1/relationships/impetus
            setRequestDatumForUser(2, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "impetus");
            terminal.handlePatch(state).get();
        }
    }

    /***************************************
     * Operations on toMany relationships. *
     ***************************************/
    // Relationship visible + Entity visible
    public class ToManyVisibleRelationshipVisibleEntity extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Post post = createPost(1, author);
            post.setTitle("Required introduction");

            Comment postScript = createComment(1, post, author, "Look for my next post soon!");
            Comment expectant = createComment(2, post, getBlogger(2), "Can't wait for your next post!");
            Comment secondUpdate = createComment(3, post, author, "My next post is now out!");

            post.getComments().add(postScript);
            post.getComments().add(expectant);

            mockPostInTX(post);
            mockCommentInTx(postScript);
            mockCommentInTx(expectant);
            mockCommentInTx(secondUpdate);

            this.post = post;
        }
        @Test
        public void testReadRelationship() {
            // user#1 GET /posts/1/relationships/comments
            setRequestDatumForUser(1, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handleGet(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 200);
        }
        @Test
        public void testAddToRelationship() {
            // user#1 POST /posts/1/relationships/comments
            setRequestDatumForUser(1, "comment", "3");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handlePost(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertEquals(post.getComments().size(), 3);
        }
        @Test
        public void testSetRelationship() {
            // user#1 PATCH /posts/1/relationships/comments
            setRequestDataForUser(1, "comment", "2", "3");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handlePatch(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertEquals(post.getComments().size(), 2);
        }
        @Test
        public void testRemoveFromRelationship() {
            // user#1 DELETE /posts/1/relationships/comments
            setRequestDatumForUser(1, "comment", "1");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handleDelete(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertEquals(post.getComments().size(), 1);
        }
    }

    // Relationship visible + Entity not visible
    public class ToManyVisibleRelationshipHiddenEntity extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Post post = createPost(1, author);
            post.setTitle("Required introduction");

            Comment postScript = createComment(1, post, author, "Look for my next post soon!");
            Comment expectant = createComment(2, post, getBlogger(2), "Can't wait for your next post!");
            Comment secondUpdate = createComment(3, post, author, "My next post is now out!");

            post.getComments().add(postScript);
            post.getComments().add(expectant);

            mockPostInTX(post);
            mockCommentInTx(postScript);
            mockCommentInTx(expectant);
            mockCommentInTx(secondUpdate);

            this.post = post;
        }
        @Test
        public void testReadRelationship() {
            // user#3 GET /posts/1/relationships/comments
            setRequestDatumForUser(3, null, null);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handleGet(state).get();
            JsonNode data = response.getRight().get("data");
            Assert.assertEquals(response.getLeft().intValue(), 200);
            Assert.assertTrue(data.isArray());
            Assert.assertEquals(data.size(), 0);
        }
        @Test
        public void testAddToRelationship () {
            // user#3 POST /posts/1/relationships/comments
            setRequestDatumForUser(3, "comment", "4");
            Comment newComment = createComment(4, post, getBlogger(3), "First!!");
            newComment.setPost(null);
            mockCommentInTx(newComment);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handlePost(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertEquals(post.getComments().size(), 3);
        }
        @Test
        public void testSetRelationship() {
            // user#3 PATCH /posts/1/relationships/comments
            setRequestDataForUser(3, "comment", "4");
            Comment newComment = createComment(4, post, getBlogger(3), "First!!");
            newComment.setPost(null);
            mockCommentInTx(newComment);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handlePatch(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertEquals(post.getComments().size(), 3);
        }
        @Test
        public void testRemoveFromRelationship() {
            // user#3 DELETE /posts/1/relationships/comments
            setRequestDataForUser(3, "comment", "4");
            Comment newComment = createComment(4, post, getBlogger(3), "First!!");
            mockCommentInTx(newComment);

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "comments");
            Pair<Integer, JsonNode> response = terminal.handleDelete(state).get();
            Assert.assertEquals(response.getLeft().intValue(), 204);
            Assert.assertEquals(post.getComments().size(), 2);
        }
    }

    // Relationship not visible + Entity visible
    public class ToManyHiddenRelationshipEntityVisible extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Blogger user2 = getBlogger(2);
            Blogger user3 = getBlogger(3);
            Blogger user4 = getBlogger(4);

            Post post = createPost(1, author);
            post.setTitle("My post");
            post.getModerators().add(user2);
            post.getModerators().add(user3);

            mockPostInTX(post);
            mockBloggerInTx(user2);
            mockBloggerInTx(user3);
            mockBloggerInTx(user4);

            this.post = post;

        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testReadRelationship() {
            // user#2 GET /posts/1/relationships/moderators
            setRequestDatumForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handleGet(state).get();

        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testAddToRelationship() {
            // user#2 POST /posts/1/relationships/moderators
            setRequestDatumForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handlePost(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testSetRelationship() {
            // user#2 PATCH /posts/1/relationships/moderators
            setRequestDataForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handlePatch(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testRemoveFromRelationship() {
            // user#2 DELETE /posts/1/relationships/moderators
            setRequestDatumForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handleGet(state).get();
        }
    }

    // Relationship not visible + Entity not visible
    public class ToManyHiddenRelationshipHiddenEntity extends RelationshipTerminalStateBaseTest {
        @BeforeMethod
        public void setupTestData() {
            Blogger author = getBlogger(1);
            Blogger user2 = getBlogger(2);
            Blogger user3 = getBlogger(3);
            Blogger user4 = getBlogger(4);
            author.setActive(false);
            user2.setActive(false);
            user3.setActive(false);
            user4.setActive(false);

            Post post = createPost(1, author);
            post.setTitle("My post");
            post.getModerators().add(user2);
            post.getModerators().add(user3);

            mockPostInTX(post);
            mockBloggerInTx(user2);
            mockBloggerInTx(user3);
            mockBloggerInTx(user4);

            this.post = post;
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testReadRelationship() {
            // user#2 GET /posts/1/relationships/moderators
            setRequestDatumForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handleGet(state).get();

        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testAddToRelationship() {
            // user#2 POST /posts/1/relationships/moderators
            setRequestDatumForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handlePost(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testSetRelationship() {
            // user#2 PATCH /posts/1/relationships/moderators
            setRequestDataForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handlePatch(state).get();
        }
        @Test(expectedExceptions = { ForbiddenAccessException.class })
        public void testRemoveFromRelationship() {
            // user#2 DELETE /posts/1/relationships/moderators
            setRequestDatumForUser(4, "blogger", "2");

            RelationshipTerminalState terminal = new RelationshipTerminalState(resource, "moderators");
            terminal.handleGet(state).get();
        }
    }
}
