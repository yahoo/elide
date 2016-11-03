/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import com.yahoo.elide.beans.NoopBean;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.*;

public class NoopTransactionTest {
    DataStoreTransaction tx = new NoopTransaction();
    NoopBean bean = new NoopBean();

    @Test
    public void testSave() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.save(bean, null);
    }

    @Test
    public void testDelete() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.delete(bean, null);
    }

    @Test
    public void testFlush() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.flush(null);
    }

    @Test
    public void testCommit() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.commit(null);
    }

    @Test
    public void testCreateObject() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.createObject(bean, null);
    }

    @Test(expectedExceptions = InvalidOperationException.class)
    public void testLoadObject() throws Exception {
        // Should throw
        tx.loadObject(NoopBean.class, 1, Optional.empty(), null);
    }

    @Test(expectedExceptions = InvalidOperationException.class)
    public void testLoadObjects() throws Exception {
        // Should throw
        tx.loadObjects(NoopBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null);
    }

    @Test
    public void testClose() throws Exception {
        tx.close();
    }
}
