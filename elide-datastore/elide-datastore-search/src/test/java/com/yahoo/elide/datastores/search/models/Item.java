/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.models;

import com.yahoo.elide.annotation.Include;

import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;

import lombok.Data;
import org.hibernate.search.annotations.TokenizerDef;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include
@Indexed
@Data
@AnalyzerDef(name = "whitespace",
        tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class)
)
public class Item {
    @Id
    private long id;

    @Fields({
            @Field(name = "name", index = Index.YES,
                    analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "whitespace")),
            @Field(name = "sortName", analyze = Analyze.NO, store = Store.NO, index = Index.YES)
    })
    @SortableField(forField = "sortName")
    private String name;

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "whitespace"))
    private String description;
}
