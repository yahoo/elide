package com.yahoo.elide.extension.runtime;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.ClassScanner;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class ElideBeans {
    @Produces
    public EntityDictionary produceDictionary(ClassScanner scanner) {
        return EntityDictionary.builder().scanner(scanner).build();
    }
}
