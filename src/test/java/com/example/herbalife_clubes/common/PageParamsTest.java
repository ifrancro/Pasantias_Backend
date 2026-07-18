package com.example.herbalife_clubes.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;

class PageParamsTest {

    @Test
    void defaultSizeWhenZeroOrNegative() {
        Pageable p = PageParams.of(0, 0);
        assertEquals(PageParams.DEFAULT_SIZE, p.getPageSize());
        Pageable p2 = PageParams.of(0, -5);
        assertEquals(PageParams.DEFAULT_SIZE, p2.getPageSize());
    }

    @Test
    void capsExcessiveSize() {
        Pageable p = PageParams.of(0, 500);
        assertEquals(PageParams.MAX_SIZE, p.getPageSize());
    }

    @Test
    void rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> PageParams.of(-1, 20));
    }

    @Test
    void acceptsValidPage() {
        Pageable p = PageParams.of(2, 10);
        assertEquals(2, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }
}
