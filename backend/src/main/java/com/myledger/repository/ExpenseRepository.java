package com.myledger.repository;

import com.myledger.dto.NameAmount;
import com.myledger.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    boolean existsByProjectId(Long projectId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByPhaseId(Long phaseId);

    boolean existsByCreatedBy(Long userId);

    /**
     * List within [start, end) with related entities fetched to avoid N+1 and lazy
     * loading in the response. Pass a wide range for "all time".
     */
    @Query("""
            select e from Expense e
            join fetch e.category
            join fetch e.project
            left join fetch e.phase
            left join fetch e.receipt
            where e.expenseDate >= :start and e.expenseDate < :end
            order by e.expenseDate desc, e.id desc
            """)
    List<Expense> findForListing(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select e from Expense e
            join fetch e.category
            join fetch e.project
            left join fetch e.phase
            left join fetch e.receipt
            where e.expenseDate >= :start and e.expenseDate < :end and e.createdBy = :userId
            order by e.expenseDate desc, e.id desc
            """)
    List<Expense> findForListingByCreator(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                          @Param("userId") Long userId);

    @Query("""
            select e from Expense e
            join fetch e.category
            join fetch e.project
            left join fetch e.phase
            left join fetch e.receipt
            where e.id = :id
            """)
    Optional<Expense> findByIdWithRelations(@Param("id") Long id);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.expenseDate >= :start and e.expenseDate < :end
            """)
    BigDecimal totalSpend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select count(e) from Expense e
            where e.expenseDate >= :start and e.expenseDate < :end
            """)
    long countInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select new com.myledger.dto.NameAmount(e.category.name, sum(e.amount))
            from Expense e
            where e.expenseDate >= :start and e.expenseDate < :end
            group by e.category.name
            order by sum(e.amount) desc
            """)
    List<NameAmount> totalByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select new com.myledger.dto.NameAmount(e.project.name, sum(e.amount))
            from Expense e
            where e.expenseDate >= :start and e.expenseDate < :end
            group by e.project.name
            order by sum(e.amount) desc
            """)
    List<NameAmount> totalByProject(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** By phase (only expenses that have a phase), labelled "Project · Phase". */
    @Query("""
            select new com.myledger.dto.NameAmount(
                concat(e.project.name, ' · ', e.phase.name), sum(e.amount))
            from Expense e
            where e.expenseDate >= :start and e.expenseDate < :end and e.phase is not null
            group by e.project.name, e.phase.name
            order by sum(e.amount) desc
            """)
    List<NameAmount> totalByPhase(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Monthly totals as 'YYYY-MM' -> sum, most recent first. Native for date_trunc. */
    @Query(value = """
            select to_char(date_trunc('month', expense_date), 'YYYY-MM') as month,
                   sum(amount) as total
            from expenses
            where expense_date >= :start and expense_date < :end
            group by date_trunc('month', expense_date)
            order by date_trunc('month', expense_date) desc
            """, nativeQuery = true)
    List<MonthlyTotalRow> totalByMonth(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // ---- Per-project report aggregates. creator = null → all; else scope to that user. ----

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.project.id = :pid and e.expenseDate >= :start and e.expenseDate < :end
              and (:creator is null or e.createdBy = :creator)
            """)
    BigDecimal reportTotal(@Param("pid") Long pid, @Param("start") LocalDate start,
                           @Param("end") LocalDate end, @Param("creator") Long creator);

    @Query("""
            select count(e) from Expense e
            where e.project.id = :pid and e.expenseDate >= :start and e.expenseDate < :end
              and (:creator is null or e.createdBy = :creator)
            """)
    long reportCount(@Param("pid") Long pid, @Param("start") LocalDate start,
                     @Param("end") LocalDate end, @Param("creator") Long creator);

    @Query("""
            select new com.myledger.dto.NameAmount(e.category.name, sum(e.amount)) from Expense e
            where e.project.id = :pid and e.expenseDate >= :start and e.expenseDate < :end
              and (:creator is null or e.createdBy = :creator)
            group by e.category.name order by sum(e.amount) desc
            """)
    List<NameAmount> reportByCategory(@Param("pid") Long pid, @Param("start") LocalDate start,
                                      @Param("end") LocalDate end, @Param("creator") Long creator);

    @Query("""
            select new com.myledger.dto.NameAmount(e.phase.name, sum(e.amount)) from Expense e
            where e.project.id = :pid and e.expenseDate >= :start and e.expenseDate < :end
              and e.phase is not null and (:creator is null or e.createdBy = :creator)
            group by e.phase.name order by sum(e.amount) desc
            """)
    List<NameAmount> reportByPhase(@Param("pid") Long pid, @Param("start") LocalDate start,
                                   @Param("end") LocalDate end, @Param("creator") Long creator);

    /** Distinct financial-year start years present in the data, most recent first. */
    @Query(value = """
            select distinct cast(
                case when extract(month from expense_date) >= :startMonth
                     then extract(year from expense_date)
                     else extract(year from expense_date) - 1 end as integer) as fy
            from expenses
            order by fy desc
            """, nativeQuery = true)
    List<Integer> distinctFinancialYears(@Param("startMonth") int startMonth);

    /** Projection for the native monthly-total query. */
    interface MonthlyTotalRow {
        String getMonth();

        BigDecimal getTotal();
    }
}
