package com.mcafee.csi.thirdparty.com.yahoo.elide.utils.coerce.converters;

import com.yahoo.elide.utils.coerce.converters.Serde;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ISO8601InstantSerdeTest {

    private final Serde<String, Instant> serde = new ISO8601InstantSerde();

    @Test
    public void can_deserialize_utc_iso_string() {

        final Instant instant = serde.deserialize("2019-06-01T09:42:55Z");

        assertEquals(1559382175, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

    }

    @Test
    public void can_deserialize_offset_iso_string() {

        final Instant instant = serde.deserialize("2019-06-01T10:42:55+01:00");

        assertEquals(1559382175, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

    }

    @Test
    public void can_deserialize_sub_second_precision_utc_iso_string() {

        final Instant instant = serde.deserialize("2019-06-01T09:42:55.123Z");

        assertEquals(1559382175, instant.getEpochSecond());
        assertEquals(123000000, instant.getNano());

    }

    @Test
    public void can_serialize() {

        assertEquals(
            "2019-06-01T09:42:55Z",
            serde.serialize(Instant.ofEpochSecond(1559382175))
        );

    }

    @Test
    public void can_serialize_sub_second_precision() {

        assertEquals(
            "2019-06-01T09:42:55.123Z",
            serde.serialize(Instant.ofEpochMilli(1559382175123L))
        );

    }

}
