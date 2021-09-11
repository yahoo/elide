package com.yahoo.elide.extension.runtime;

import org.jboss.resteasy.spi.metadata.ResourceBuilder;

public class ElideResourceBuilder extends ResourceBuilder {
    public ElideResourceBuilder() {
        System.out.println("foo");
    }

    @Override
    protected ResourceClassBuilder createResourceClassBuilder(Class<?> clazz) {
        //return super.createResourceClassBuilder(clazz);
        return buildRootResource(clazz, "/foo");
    }
}
