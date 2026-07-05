package com.myledger.repository;

import com.myledger.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    boolean existsByUploadedBy(Long userId);
}
