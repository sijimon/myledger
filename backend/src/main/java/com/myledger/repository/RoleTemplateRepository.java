package com.myledger.repository;

import com.myledger.entity.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, Long> {

    List<RoleTemplate> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
