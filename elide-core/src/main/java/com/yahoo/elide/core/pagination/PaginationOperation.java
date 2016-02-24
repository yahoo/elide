package com.yahoo.elide.core.pagination;

/**
 * Interface for pagination operations.
 * @param <T> the return type for apply
 */
public interface PaginationOperation<T> {

    T apply(Pagination page);

}
