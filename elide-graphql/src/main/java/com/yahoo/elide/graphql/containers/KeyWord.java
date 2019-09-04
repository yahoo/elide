package com.yahoo.elide.graphql.containers;

import lombok.Getter;

public enum KeyWord {
    NODE_KEYWORD("node"),
    EDGES_KEYWORD("edges"),
    PAGE_INFO_KEYWORD("pageInfo"),
    PAGE_INFO_HAS_NEXT_PAGE_KEYWORD("hasNextPage"),
    PAGE_INFO_START_CURSOR_KEYWORD("startCursor"),
    PAGE_INFO_END_CURSOR_KEYWORD("endCursor"),
    PAGE_INFO_TOTAL_RECORDS_KEYWORD("totalRecords"),
    UNKNOWN("unknown");

    @Getter
    private String name;

    KeyWord(String name) {
        this.name = name;
    }

    public boolean equals(String name) {
        return this.name.equals(name);
    }

    public static KeyWord byName(String value) {
        for (KeyWord keyWord : KeyWord.values()) {
            if (keyWord.equals(value)) {
                return keyWord;
            }
        }

        return UNKNOWN;
    }
}
