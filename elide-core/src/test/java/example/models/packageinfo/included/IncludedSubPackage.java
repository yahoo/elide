/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.packageinfo.included;

import example.models.BaseId;

import jakarta.persistence.Entity;

/**
 * Helper class to test Include on parent package level
 */
@Entity
public class IncludedSubPackage extends BaseId {
}
