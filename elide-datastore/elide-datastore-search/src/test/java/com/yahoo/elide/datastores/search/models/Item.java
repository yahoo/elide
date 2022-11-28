/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.models;

import com.yahoo.elide.annotation.Include;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Include
@Indexed
@Data
@AnalyzerDef(name = "case_insensitive",
        tokenizer = @TokenizerDef(factory = NGramTokenizerFactory.class, params = {
            @Parameter(name = "minGramSize", value = "3"),
            @Parameter(name = "maxGramSize", value = "50")
        }),
        filters = {
                @TokenFilterDef(factory = LowerCaseFilterFactory.class)
        }
)
public class Item {
    @Id
    private long id;

    @Fields({
            @Field(name = "name", index = Index.YES,
                    analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "case_insensitive")),
            @Field(name = "sortName", analyze = Analyze.NO, store = Store.NO, index = Index.YES)
    })
    @SortableField(forField = "sortName")
    private String name;

    @Field(index = Index.YES, analyze = Analyze.YES,
            store = Store.NO, analyzer = @Analyzer(definition = "case_insensitive"))
    private String description;

    @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @SortableField
    private Date modifiedDate;

    private BigDecimal price;
}
