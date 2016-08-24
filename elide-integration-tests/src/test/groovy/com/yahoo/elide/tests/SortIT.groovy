/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.restassured.RestAssured
import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer
import org.testng.Assert
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Tests for sorting, including sorting rules that involve related entities and their attributes.
 */
class SortIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper()

    private Set<Integer> cityIds = new HashSet<>()
    private Set<Integer> countryIds = new HashSet<>()
    private Set<Integer> headOfGovernmentIds = new HashSet<>()
    private Set<Integer> territoryIds = new HashSet<>()
    private Set<Integer> olympicGamesIds = new HashSet<>()

    @BeforeClass
    public void setup() {

        // Add some Olympic Games
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
            {"op": "add", "path": "/olympicGames",
                "value": {"type": "olympicGames", "id": 1,"attributes": {"location": "Athens", "year": 1896}}
            },
            {"op": "add", "path": "/olympicGames",
                "value": {"type": "olympicGames", "id": 2,"attributes": {"location": "Paris", "year": 1900}}
            },
            {"op": "add", "path": "/olympicGames",
                "value": {"type": "olympicGames", "id": 3,"attributes": {"location": "Sydney", "year": 2000}}
            },
            {"op": "add", "path": "/olympicGames",
                "value": {"type": "olympicGames", "id": 4,"attributes": {"location": "Rio", "year": 2016}}
            }
            ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        // Add heads of government
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 1,"attributes": {"name": "Malcolm Turnbull"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 2,"attributes": {"name": "Mike Baird"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 3,"attributes": {"name": "Daniel Andrews"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 4,"attributes": {"name": "Annastacia Palaszczuk"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 5,"attributes": {"name": "Colin Barnett"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 6,"attributes": {"name": "Jay Weatherill"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 7,"attributes": {"name": "Will Hodgman"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 8,"attributes": {"name": "Adam Giles"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 9,"attributes": {"name": "Andrew Barr"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 10,"attributes": {"name": "Justin Trudeau"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 11,"attributes": {"name": "Kathleen Wynne"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 12,"attributes": {"name": "Philippe Couillard"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 13,"attributes": {"name": "Stephen McNeil"}}
            },
            {"op": "add", "path": "/headOfGovernment",
                "value": {"type": "headOfGovernment", "id": 14,"attributes": {"name": "Rachel Notley"}}
            }
            ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        // Add countries
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
            {"op": "add", "path": "/country", "value": {
                "type": "country", "id": 1, "attributes": {"name": "Australia"},
                "relationships": {
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 1}},
                    "olympicGames": {"data": [
                        {"type": "olympicGames", "id": 1},
                        {"type": "olympicGames", "id": 2},
                        {"type": "olympicGames", "id": 3},
                        {"type": "olympicGames", "id": 4}
                    ]}
                }
            }},
            {"op": "add", "path": "/country", "value": {
                "type": "country", "id": 2,
                "attributes": {"name": "Canada"},
                "relationships": {
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 10}},
                    "olympicGames": {"data": [
                        {"type": "olympicGames", "id": 2},
                        {"type": "olympicGames", "id": 3},
                        {"type": "olympicGames", "id": 4}
                    ]}
                }
            }}
            ]''')
            .patch("/").then().statusCode(HttpStatus.SC_OK)

        // Add territories
        RestAssured
            .given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body('''[
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 1,
                "attributes": {"name": "New South Wales"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 2}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 2,
                "attributes": {"name": "Victoria"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 3}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 3,
                "attributes": {"name": "Queensland"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 4}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 4,
                "attributes": {"name": "Western Australia"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 5}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 5,
                "attributes": {"name": "South Australia"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 6}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 6,
                "attributes": {"name": "Tasmania"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 7}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 7,
                "attributes": {"name": "Australian Capital Territory"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 8}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 8,
                "attributes": {"name": "Northern Territory"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 1}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 9}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 9,
                "attributes": {"name": "Ontario"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 2}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 11}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 10,
                "attributes": {"name": "Quebec"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 2}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 12}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 11,
                "attributes": {"name": "Nova Scotia"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 2}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 13}}
                }
            }},
            {"op": "add", "path": "/territory", "value": {
                "type": "territory", "id": 12,
                "attributes": {"name": "Alberta"},
                "relationships": {
                    "country": {"data": {"type": "country", "id": 2}},
                    "headOfGovernment": {"data": {"type": "headOfGovernment", "id": 14}}
                }
            }}
            ]''')
            .patch("/").then().log().all().statusCode(HttpStatus.SC_OK)

        // Add cities
        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''[
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 1,
                "attributes": {"name": "Sydney"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 1}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 2,
                "attributes": {"name": "Newcastle"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 1}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 3,
                "attributes": {"name": "Wollongong"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 1}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 4,
                "attributes": {"name": "Melbourne"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 2}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 5,
                "attributes": {"name": "Geelong"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 2}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 6,
                "attributes": {"name": "Brisbane"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 3}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 7,
                "attributes": {"name": "Cairns"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 3}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 8,
                "attributes": {"name": "Perth"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 4}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 9,
                "attributes": {"name": "Adelaide"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 5}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 10,
                "attributes": {"name": "Hobart"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 6}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 11,
                "attributes": {"name": "Launceston"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 6}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 12,
                "attributes": {"name": "Canberra"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 7}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 13,
                "attributes": {"name": "Darwin"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 8}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 14,
                "attributes": {"name": "Toronto"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 9}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 15,
                "attributes": {"name": "Ottawa"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 9}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 16,
                "attributes": {"name": "Quebec"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 10}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 17,
                "attributes": {"name": "Montreal"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 10}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 18,
                "attributes": {"name": "Halifax"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 11}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 19,
                "attributes": {"name": "Edmonton"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 12}}}
            }},
            {"op": "add", "path": "/city", "value": {
                "type": "city", "id": 20,
                "attributes": {"name": "Calgary"},
                "relationships": {"territory": {"data": {"type": "territory", "id": 12}}}
            }}
            ]''')
                .patch("/").then().log().all().statusCode(HttpStatus.SC_OK)

        for (JsonNode author : mapper.readTree(RestAssured.get("/city").asString()).get("data")) {
            cityIds.add(author.get("id").asInt())
        }
        for (JsonNode country : mapper.readTree(RestAssured.get("/country").asString()).get("data")) {
            countryIds.add(country.get("id").asInt())
        }
        for (JsonNode headOfGovernment : mapper.readTree(RestAssured.get("/headOfGovernment").asString()).get("data")) {
            headOfGovernmentIds.add(headOfGovernment.get("id").asInt())
        }
        for (JsonNode olympicGames : mapper.readTree(RestAssured.get("/olympicGames").asString()).get("data")) {
            olympicGamesIds.add(olympicGames.get("id").asInt())
        }
        for (JsonNode territory : mapper.readTree(RestAssured.get("/territory").asString()).get("data")) {
            territoryIds.add(territory.get("id").asInt())
        }
    }

    @Test
    public void testSortOnSimpleAttribute() {
        def result = mapper.readTree(RestAssured.get("/city?sort=name").asString())
        def cities = result["data"]
        Assert.assertEquals(cities.size(), 20)
        final String firstCityName = cities.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCityName, "Adelaide")
    }

    @Test
    public void testSortOnOneToOneJoinEntityAttribute() {
        def result = mapper.readTree(RestAssured.get("/territory?sort=headOfGovernment.name").asString())
        def territories = result["data"]
        Assert.assertEquals(territories.size(), 12)
        final String firstTerritoryName = territories.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstTerritoryName, "Australian Capital Territory")
    }

    @Test
    public void testSortOnManyToOneJoinEntityAttribute() {
        def result = mapper.readTree(RestAssured.get("/city?sort=territory.country.name,territory.name,name").asString())
        def cities = result["data"]
        Assert.assertEquals(cities.size(), 20)
        final String firstCityName = cities.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCityName, "Canberra")
    }

    @Test
    public void testSortOnManyToOneJoinEntityAttributeWithPagination() {
        def result = mapper.readTree(RestAssured.get("/city?sort=territory.country.name,territory.name,name&page[size]=4&page[number]=3").asString())
        def cities = result["data"]
        Assert.assertEquals(cities.size(), 4)
        final String firstCityName = cities.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCityName, "Hobart")

        def metaPage = result["meta"]["page"]
        Assert.assertEquals(metaPage.get("number").asText(), "3")
        Assert.assertEquals(metaPage["limit"].asText(), "4")
    }

    @Test
    public void testSortOnFilteredManyToOneJoinEntityAttribute() {
        def result = mapper.readTree(RestAssured.get("/city?filter[city.territory.country.name]=Canada&sort=territory.name,name").asString())
        def cities = result["data"]
        Assert.assertEquals(cities.size(), 7)
        final String firstCityName = cities.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCityName, "Calgary")
    }

    @Test
    public void testSortOnOneToManyJoinEntityAttributes() {
        def result = mapper.readTree(RestAssured.get("/country?sort=territories.name").asString())
        def countries = result["data"]
        Assert.assertEquals(countries.size(), 2)
        def firstCountryName = countries.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCountryName, "Canada")
    }

    @Test
    public void testSortOnManyToManyJoinEntityAttributes() {
        def result = mapper.readTree(RestAssured.get("/country?sort=olympicGames.year").asString())
        def countries = result["data"]
        Assert.assertEquals(countries.size(), 2)
        def firstCountryName = countries.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCountryName, "Australia")
    }

    @Test
    public void testSortOnManyToManyJoinEntityAttributes2() {
        def result = mapper.readTree(RestAssured.get("/country?sort=-olympicGames.year,-name").asString())
        def countries = result["data"]
        Assert.assertEquals(countries.size(), 2)
        def firstCountryName = countries.get(0)["attributes"]["name"].asText()
        Assert.assertEquals(firstCountryName, "Canada")
    }

    @AfterTest
    public void cleanUp() {
        for (int id : cityIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/city/"+id)
        }
        for (int id : territoryIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/territory/"+id)
        }
        for (int id : countryIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/country/"+id)
        }
        for (int id : headOfGovernmentIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/headOfGovernment/"+id)
        }
        for (int id : olympicGamesIds) {
            RestAssured
                    .given()
                    .accept("application/vnd.api+json; ext=jsonpatch")
                    .delete("/olympicGames/"+id)
        }
    }
}
