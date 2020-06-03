/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

import org.hibernate.Session;

/**
 * Cancel Session implementation.
 */

public abstract class CancelSession {
   private final Session session;
   
   protected CancelSession(Session session) {
       this.session = session;
   }

   public void cancel() {
       session.cancelQuery();
   }
}
