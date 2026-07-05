package com.myledger.repository;

import com.myledger.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @Query("select m.projectId from ProjectMember m where m.userId = :userId")
    List<Long> findProjectIdsByUserId(@Param("userId") Long userId);
}
