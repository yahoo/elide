/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Date;
import java.util.Set;

@Entity
@Include(friendlyName = "Book")
@ReadPermission(expression = "Principal is author OR Principal is publisher")
@CreatePermission(expression = "Principal is author")
@Schema(title = "Override Include Title", description = "A book")
public class Book {
    @OneToMany
    @Size(max = 10)
    @UpdatePermission(expression = "Principal is author")
    @Schema(description = "Writers", requiredMode = RequiredMode.REQUIRED, accessMode = AccessMode.READ_ONLY,
        example = "[\"author1\", \"author2\", \"author3\"]")
    public Set<Author> getAuthors() {
        return null;
    }

    @OneToOne
    @UpdatePermission(expression = "Principal is publisher")
    @Schema(requiredMode = RequiredMode.REQUIRED, extensions = {
            @Extension(properties = {
                    @ExtensionProperty(name = "relationType", value = "oneToOne")
            })
    })
    public Publisher getPublisher() {
        return null;
    }

    @NotNull
    @Schema(requiredMode = RequiredMode.REQUIRED, extensions = {
            @Extension(properties = {
                    @ExtensionProperty(name = "isLocalized", value = "false")
            })
    })
    public String title;

    @Schema(description = "Year published", example = "1999", accessMode = AccessMode.READ_ONLY)
    public String year;

    public Date publishedOn;
}
