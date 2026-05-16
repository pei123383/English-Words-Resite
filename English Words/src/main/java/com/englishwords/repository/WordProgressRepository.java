package com.englishwords.repository;

import com.englishwords.entity.WordProgress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WordProgressRepository extends JpaRepository<WordProgress, Long> {

    Optional<WordProgress> findByUserIdAndWordId(Long userId, Long wordId);

    @Query("""
        select p from WordProgress p
        where p.user.id = :userId
          and (p.nextReviewAt is null or p.nextReviewAt <= :now)
        order by p.nextReviewAt asc
        """)
    List<WordProgress> findDue(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
        select count(p) from WordProgress p
        where p.user.id = :userId
          and (p.nextReviewAt is null or p.nextReviewAt <= :now)
        """)
    long countDue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("select count(p) from WordProgress p where p.user.id = :userId and p.masteryLevel >= :level")
    long countMastered(@Param("userId") Long userId, @Param("level") int level);

    @Query("select coalesce(sum(p.reviewCount), 0) from WordProgress p where p.user.id = :userId")
    long sumReviewCount(@Param("userId") Long userId);
}
