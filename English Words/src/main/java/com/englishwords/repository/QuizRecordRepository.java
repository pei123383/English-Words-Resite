package com.englishwords.repository;

import com.englishwords.entity.QuizRecord;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {

    List<QuizRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
