package com.myledger.repository;

import com.myledger.entity.FundRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FundRequestItemRepository extends JpaRepository<FundRequestItem, Long> {

    List<FundRequestItem> findByRequestIdOrderByIdAsc(Long requestId);

    @Query("select coalesce(sum(i.qty * i.unitPrice), 0) from FundRequestItem i where i.requestId = :requestId")
    BigDecimal sumAmount(@Param("requestId") Long requestId);
}
