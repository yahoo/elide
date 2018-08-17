package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.hibernate5.HibernateRevisionsDataStore;
import com.yahoo.elide.utils.ClassScanner;
import example.AddressFragment;
import example.House;
import example.Parent;
import example.Person;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
import example.Author;
import example.Book;
import example.Chapter;
import example.Filtered;
import example.TestCheckMappings;
import org.hibernate.cfg.Environment;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class RevisionStoreIT extends AbstractIntegrationTestInitializer {

    private static final String CLASH_OF_KINGS = "A Clash of Kings";
    private static final String STORM_OF_SWORDS = "A Storm of Swords";
    private static final String SONG_OF_ICE_AND_FIRE = "A Song of Ice and Fire";
    private static final String DATA = "data";
    private static final String ATTRIBUTES = "attributes";
    private static final String NAME = "name";
    private static final String ADDRESS = "address";
    private static final String HOUSE = "house";
    private static final String RELATIONS = "relationships";
    private static final String CHAPTER_COUNT = "chapterCount";

    private final JsonParser jsonParser;
    private final ObjectMapper mapper;
    private final Elide elide;

    public RevisionStoreIT() {
        jsonParser = new JsonParser();
        mapper = new ObjectMapper();
        elide = new Elide(new ElideSettingsBuilder(AbstractIntegrationTestInitializer.getDatabaseManager())
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(new EntityDictionary(TestCheckMappings.MAPPINGS))
                .build());

    }

    @BeforeClass
    public static void setup() throws IOException {
        RequestScope scope = Mockito.mock(RequestScope.class);
        Person person;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {

            //tx.save(tx.createNewObject(Filtered.class), null);
            //tx.save(tx.createNewObject(Filtered.class), null);
            //tx.save(tx.createNewObject(Filtered.class), null);

            person = new Person();
            person.setName("person");
            AddressFragment af = new AddressFragment();
            af.state = "IL";
            af.street = "Illinois St";
            person.setAddress(af);
            tx.save(person, scope);
            tx.commit(scope);
        }

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            //Person person = (Person) tx.loadObject(Person.class, 1, Optional.empty(), scope);
            person.setName("new name");

            tx.save(person, scope);
            tx.commit(scope);
        }

        House house1, house2;
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            //Person person = (Person) tx.loadObject(Person.class, 1, Optional.empty(), scope);
            house1 = addHouse(1, person, tx);

            //tx.save(person, scope);
            //tx.commit(scope);
        }

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            //Person person = (Person) tx.loadObject(Person.class, 1, Optional.empty(), scope);
            house2 = addHouse(2, person, tx);

            //tx.save(person, scope);
            //tx.commit(scope);
        }

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            AddressFragment af = new AddressFragment();
            af.street = "changes address" + "Street";
            house2.setAddress(af);
            tx.save(house2, scope);
            tx.commit(scope);
        }
    }

    private static House addHouse(int number, Person person, DataStoreTransaction tx) {
        House house = new House();
        AddressFragment af = new AddressFragment();
        af.street = number + "Street";
        house.setAddress(af);
        house.setOwner(person);
        Set<House> houses = person.getHouse() == null
                ? new HashSet<>()
                : person.getHouse();
        houses.add(house);
        person.setHouse(houses);
        tx.save(house, null);
        tx.save(person, null);
        tx.commit(null);
        return house;
    }

    @Test
    public void testRootEntityFormulaFetch() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[person]", Arrays.asList("name,address"));
        ElideResponse response = elide.get("/person", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), 1);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(NAME).asText(), "person");
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(ADDRESS).toString(), "{\"street\":\"Illinois St\",\"state\":\"IL\",\"zip\":null}");
    }

    @Test
    public void testSubCollectionEntityFormulaFetch() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("fields[house]", Arrays.asList("address"));
        ElideResponse response = elide.get("/person/1/house", queryParams, 1);
        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), 2);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(ADDRESS).toString(),
                "{\"street\":\"0Street\",\"state\":null,\"zip\":null}");
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(ADDRESS).toString(),
                "{\"street\":\"1Street\",\"state\":null,\"zip\":null}");
    }

    @Test
    public void testRootEntityVersion() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.put("__historicalversion", Arrays.asList(Long.toString(1)));
        ElideResponse response = elide.get("/person/1", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).get(ATTRIBUTES).get(NAME).asText(), "person");
        assertEquals(result.get(DATA).get(ATTRIBUTES).get(ADDRESS).toString(),
                "{\"street\":\"Illinois St\",\"state\":\"IL\",\"zip\":null}");

        queryParams.put("__historicalversion", Arrays.asList(Long.toString(2)));
        response = elide.get("/person/1", queryParams, 1);

        result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).get(ATTRIBUTES).get(NAME).asText(), "new name");
        assertEquals(result.get(DATA).get(ATTRIBUTES).get(ADDRESS).toString(),
                "{\"street\":\"Illinois St\",\"state\":\"IL\",\"zip\":null}");

        queryParams.put("__historicalversion", Arrays.asList(Long.toString(3)));
        response = elide.get("/person/1", queryParams, 1);

        result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).get(RELATIONS).get(HOUSE).toString(),
                "{\"data\":[{\"type\":\"house\",\"id\":\"1\"}]}");

        queryParams.put("__historicalversion", Arrays.asList(Long.toString(4)));
        response = elide.get("/person/1", queryParams, 1);

        result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).get(RELATIONS).get(HOUSE).toString(),
                "{\"data\":[{\"type\":\"house\",\"id\":\"1\"}," +
                        "{\"type\":\"house\",\"id\":\"2\"}]}");
    }

    @Test
    public void testSubCollectionEntityVersion() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.put("__historicalversion", Arrays.asList(Long.toString(4)));
        ElideResponse response = elide.get("/person/1/house/1", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).get(ATTRIBUTES).get(ADDRESS).toString(),
                "{\"street\":\"1Street\",\"state\":null,\"zip\":null}");

        queryParams.put("__historicalversion", Arrays.asList(Long.toString(5)));
        response = elide.get("/person/1/house/2", queryParams, 1);

        result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).get(ATTRIBUTES).get(ADDRESS).toString(),
                "{\"street\":\"changes addressStreet\",\"state\":null,\"zip\":null}");
    }
}
