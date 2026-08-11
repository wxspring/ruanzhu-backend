package com.company.ruanzhu.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success("test");
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("test", result.getData());
    }

    @Test
    void testError() {
        Result<?> result = Result.error(400, "bad request");
        assertEquals(400, result.getCode());
        assertEquals("bad request", result.getMessage());
        assertNull(result.getData());
    }
}
