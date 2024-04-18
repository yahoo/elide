/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.paiondata.elide.annotation.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Entity
@Include(rootLevel = false, description = "The Agent", friendlyName = "Agent")
public class Agent {
    @OneToMany
    @Size(max = 10)
    @Schema(description = "Writers", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY,
            example = "[\"author1\", \"author2\", \"author3\"]")
    public Set<Author> getAuthors() {
        return null;
    }

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;
}
