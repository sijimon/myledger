package com.myledger.repository;

import com.myledger.dto.StatusCount;
import com.myledger.entity.FundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FundRequestRepository extends JpaRepository<FundRequest, Long> {

    boolean existsByRequesterId(Long userId);

    /** Per-project fund-request totals by status. requester = null → all; else scope to that user. */
    @Query("""
            select new com.myledger.dto.StatusCount(fr.status, count(fr), coalesce(sum(fr.total), 0))
            from FundRequest fr
            where fr.project.id = :pid
              and (:requester is null or fr.requester.id = :requester)
              and fr.createdAt >= :start and fr.createdAt < :end
            group by fr.status
            """)
    List<StatusCount> reportByStatus(@Param("pid") Long pid, @Param("requester") Long requester,
                                     @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);


    @Query("""
            select fr from FundRequest fr
            join fetch fr.project
            join fetch fr.requester
            where fr.requester.id = :userId
            order by fr.id desc
            """)
    List<FundRequest> findMine(@Param("userId") Long userId);

    @Query("""
            select fr from FundRequest fr
            join fetch fr.project
            join fetch fr.requester
            order by fr.id desc
            """)
    List<FundRequest> findAllDetailed();

    @Query("""
            select fr from FundRequest fr
            join fetch fr.project
            join fetch fr.requester
            where fr.id = :id
            """)
    Optional<FundRequest> findByIdDetailed(@Param("id") Long id);
}
