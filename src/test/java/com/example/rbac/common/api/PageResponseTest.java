package com.example.rbac.common.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResponseTest {

    @Test
    void computesPaginationMetadata() {
        PageResponse<String> page = PageResponse.of(
                Arrays.asList("a", "b"), 0, 2, 5);

        assertEquals(2, page.getItems().size());
        assertEquals(0, page.getPageNumber());
        assertEquals(2, page.getPageSize());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertTrue(page.isFirst());
        assertFalse(page.isLast());
        assertFalse(page.isEmpty());
    }

    @Test
    void handlesEmptyPage() {
        PageResponse<String> page = PageResponse.of(
                Collections.emptyList(), 0, 10, 0);

        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalPages());
        assertTrue(page.isLast());
    }
}
