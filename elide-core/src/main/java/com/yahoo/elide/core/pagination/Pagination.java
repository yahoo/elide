package com.yahoo.elide.core.pagination;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the pagination strategy
 */

@AllArgsConstructor
@ToString
public class Pagination {

    public static final int DEFAULT_PAGE_SIZE = 500;
    // todo do we ever want to return more than 10000 items? Set Upper Bounds

    private static final Pagination DEFAULT_PAGINATION = new Pagination(0,0);

    public static final Map<String,String> pageKeys = new HashMap<>();
    static {
        pageKeys.put("page[size]","pageSize");
        pageKeys.put("page[limit]","pageSize");
        pageKeys.put("page[number]","page");
        pageKeys.put("page[offset]","page");
    }

    @Getter
    private int page;

    @Getter
    private int pageSize;

    /**
     * Know if this is the default instance
     * @return The default pagination values
     */
    public boolean isDefault() {
        return this.page == 0 && this.pageSize == 0;
    }

    /**
     * Given json-api paging params, generate page and pageSize values from query params
     * @param queryParams The page queryParams (ImmuatableMultiValueMap)
     * @return The new Page object
     */
    public static Pagination parseQueryParams(final MultivaluedMap<String, String> queryParams) {
        final Map<String, Integer> pageData = new HashMap<>();
        queryParams.entrySet()
                .forEach(paramEntry -> {
                    // looking for page[size], page[limit]
                    // looking for page[offset], page[number]
                    final String queryParamKey = paramEntry.getKey();
                    if (pageKeys.containsKey(queryParamKey)) {
                        final String type = pageKeys.get(queryParamKey);
                        final String value = paramEntry.getValue().get(0); // is this correct...
                        try {
                            pageData.put(type, Integer.parseInt(value, 10));
                        } catch (ClassCastException e) {
                            // todo - should we log here?
                        }
                    }
                });

        if (pageData.isEmpty()) {
            return DEFAULT_PAGINATION;
        }
        if (!pageData.containsKey("page")) {
            pageData.put("page", 1);
        }
        if (!pageData.containsKey("pageSize")) {
            pageData.put("pageSize", DEFAULT_PAGE_SIZE);
        }
        return new Pagination(pageData.get("page"), pageData.get("pageSize"));
    }

    /**
     * Default Instance
     * @return The default instance
     */
    public static Pagination getDefaultPagination() {
        return DEFAULT_PAGINATION;
    }
}
