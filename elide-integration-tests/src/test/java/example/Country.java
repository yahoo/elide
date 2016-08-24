/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;


import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.checks.prefab.Role;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.Collection;

@Entity
@Include(rootLevel = true)
@SharePermission(all = {Role.ALL.class})
@Getter
@Setter
public class Country {
    private long id;
    private String name;
    private HeadOfGovernment headOfGovernment;
    private Collection<Territory> territories;
    private Collection<OlympicGames> olympicGames;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    @OneToOne
    @JoinColumn(name = "head_of_government_id")
    public HeadOfGovernment getHeadOfGovernment() {
        return headOfGovernment;
    }

    @OneToMany(mappedBy = "country")
    public Collection<Territory> getTerritories() {
        return territories;
    }

    @ManyToMany
    @JoinTable(
            name = "CountryOlympicGames",
            joinColumns = @JoinColumn(name = "country_id"),
            inverseJoinColumns = @JoinColumn(name = "olympic_games_id")
    )
    public Collection<OlympicGames> getOlympicGames() {
        return olympicGames;
    }
}
