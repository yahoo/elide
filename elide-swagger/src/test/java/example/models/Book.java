/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Include
@ReadPermission(expression = "Principal is author OR Principal is publisher")
@CreatePermission(expression = "Principal is author")
@DeletePermission(expression = "Prefab.Role.None")
@ApiModel(description = "A book")
public class Book {
    @OneToMany
    @Size(max = 10)
    @UpdatePermission(expression = "Principal is author")
    @ApiModelProperty(value = "Writers", required = false, readOnly = true,
        example = "[\"author1\", \"author2\", \"author3\"]")
    public Set<Author> getAuthors() {
        return null;
    }

    @OneToOne
    @UpdatePermission(expression = "Principal is publisher")
    public Publisher getPublisher() {
        return null;
    }

    @NotNull
    @ApiModelProperty(required = true)
    public String title;

    @ApiModelProperty(value = "Year published", example = "1999", readOnly = true)
    public String year;
}
