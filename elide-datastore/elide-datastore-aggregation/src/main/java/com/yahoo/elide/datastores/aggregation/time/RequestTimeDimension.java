package com.yahoo.elide.datastores.aggregation.time;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import lombok.Getter;

public class RequestTimeDimension {
    @Getter
    private TimeDimension timeDimension;

    @Getter
    private TimeGrain requestedGrain;

    public RequestTimeDimension(TimeDimension timeDimension, TimeGrain requestedGrain) {
        if (timeDimension.getSupportedGrains().stream()
                .noneMatch(grain -> grain.getGrain().equals(requestedGrain))) {
            throw new InvalidValueException(
                    "Unsupported time grain " + requestedGrain + " on " + timeDimension.getId());
        }

        this.timeDimension = timeDimension;
        this.requestedGrain = requestedGrain;
    }
}
