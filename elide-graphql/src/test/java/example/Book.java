/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Singular;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model for books.
 * <p>
 * <b>CAUTION: DO NOT DECORATE IT WITH {@link Builder}, which hides its no-args constructor. This will result in
 * runtime error at places such as {@code entityClass.newInstance();}</b>
 */
@Entity
@Table(name = "book")
@Include(description = "A GraphQL Book")
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${book.title}"})
@Subscription(operations = { Subscription.Operation.CREATE, Subscription.Operation.UPDATE})
public class Book {

    private long id;
    private String title;
    private String genre;
    private String language;
    @JsonIgnore
    private long publishDate = 0;
    @Singular private Collection<Author> authors = new ArrayList<>();
    private Publisher publisher = null;
    private Date publicationDate = null;

    private Date lastPurchasedDate = null;
    private Author.AuthorType authorTypeAtTimeOfPublication;
    private Set<PublicationFormat> publicationFormats = new HashSet<>();
    private Set<Preview> previews = new HashSet<>();
    private BigDecimal weightLbs;
    private Price price;
    private List<Price> priceHistory;
    private Map<Date, Price> priceRevisions;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @SubscriptionField
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @SubscriptionField
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Price getPrice() {
        return price;
    }

    public void setPriceHistory(List<Price> priceHistory) {
        this.priceHistory = priceHistory;
    }

    public List<Price> getPriceHistory() {
        return priceHistory;
    }

    public void setPriceRevisions(Map<Date, Price> priceRevisions) {
        this.priceRevisions = priceRevisions;
    }

    public Map<Date, Price> getPriceRevisions() {
        return priceRevisions;
    }

    public void setPrice(Price price) {
        this.price = price;
    }


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setPublishDate(final long publishDate) {
        this.publishDate = publishDate;
    }

    public long getPublishDate() {
        return this.publishDate;
    }

    public void setWeightLbs(BigDecimal weight) {
        this.weightLbs = weight;
    }

    public BigDecimal getWeightLbs() {
        return this.weightLbs;
    }

    @SubscriptionField
    @ManyToMany
    public Collection<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Collection<Author> authors) {
        this.authors = authors;
    }

    @SubscriptionField
    @OneToMany(mappedBy = "book")
    public Collection<Preview> getPreviews() {
        return previews;
    }

    public void setPreviews(Set<Preview> previews) {
        this.previews = previews;
    }

    @OneToOne
    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Date getLastPurchasedDate() {
        return lastPurchasedDate;
    }

    public void setLastPurchasedDate(Date lastPurchasedDate) {
        this.lastPurchasedDate = lastPurchasedDate;
    }

    public Author.AuthorType getAuthorTypeAtTimeOfPublication() {
        return authorTypeAtTimeOfPublication;
    }

    public void setAuthorTypeAtTimeOfPublication(Author.AuthorType authorTypeAtTimeOfPublication) {
        this.authorTypeAtTimeOfPublication = authorTypeAtTimeOfPublication;
    }

    public Set<PublicationFormat> getPublicationFormats() {
        return publicationFormats;
    }

    public void setPublicationFormats(Set<PublicationFormat> publicationFormats) {
        this.publicationFormats = publicationFormats;
    }
}
