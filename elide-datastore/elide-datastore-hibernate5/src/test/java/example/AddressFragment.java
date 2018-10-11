/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.datastores.hibernate5.usertypes.JsonType;

import org.hibernate.annotations.TypeDef;

@TypeDef(typeClass = JsonType.class, name = "json")
public class AddressFragment {
   public String street;
   public String state;
   public ZipCode zip;

   public class ZipCode {
       public String zip;
       public String plusFour;
   }
}
