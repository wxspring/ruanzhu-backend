package com.company.ruanzhu.common.util;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageRequest {
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int size = 10;

    private String sortBy;
    private boolean ascending = false;

    public long getOffset() {
        return (long) (page - 1) * size;
    }
}
