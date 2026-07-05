package com.myledger.dto;

import com.myledger.entity.FundRequestItem;

import java.math.BigDecimal;

public record FundRequestItemResponse(Long id, String description, BigDecimal qty, BigDecimal unitPrice, BigDecimal amount) {
    public static FundRequestItemResponse from(FundRequestItem i) {
        return new FundRequestItemResponse(i.getId(), i.getDescription(), i.getQty(), i.getUnitPrice(), i.amount());
    }
}
