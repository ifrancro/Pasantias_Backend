package com.example.herbalife_clubes.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagedResponseTest {

    @Test
    void emptyPage() {
        PagedResponse<String> page = PagedResponse.empty(0, 20);
        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
        assertTrue(page.first());
        assertTrue(page.last());
        assertFalse(page.hasNext());
        assertFalse(page.hasPrevious());
    }

    @Test
    void middlePageMetadata() {
        PagedResponse<Integer> page = PagedResponse.of(List.of(1, 2), 1, 2, 5);
        assertEquals(1, page.page());
        assertEquals(2, page.size());
        assertEquals(5, page.totalElements());
        assertEquals(3, page.totalPages());
        assertFalse(page.first());
        assertFalse(page.last());
        assertTrue(page.hasNext());
        assertTrue(page.hasPrevious());
    }

    @Test
    void lastPage() {
        PagedResponse<Integer> page = PagedResponse.of(List.of(5), 2, 2, 5);
        assertTrue(page.last());
        assertFalse(page.hasNext());
        assertTrue(page.hasPrevious());
    }

    @Test
    void outOfRangePageStillEmptyContentWithTotals() {
        PagedResponse<Integer> page = PagedResponse.of(List.of(), 10, 20, 3);
        assertTrue(page.content().isEmpty());
        assertEquals(3, page.totalElements());
        assertFalse(page.hasNext());
    }
}
