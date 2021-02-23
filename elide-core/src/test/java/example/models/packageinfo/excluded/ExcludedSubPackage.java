/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.packageinfo.excluded;

import example.models.BaseId;
import javax.persistence.Entity;

/**
 * Helper class to test Include on parent package level
 */
@Entity
public class ExcludedSubPackage extends BaseId {
}
