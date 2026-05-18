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
          and p.reviewCount > 0
        order by
          case when p.nextReviewAt is null then 1 else 0 end,
          p.nextReviewAt asc,
          p.updatedAt desc
        """)
    List<WordProgress> findDue(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        select count(p) from WordProgress p
        where p.user.id = :userId
          and p.reviewCount > 0
        """)
    long countDue(@Param("userId") Long userId);

    @Query("select count(p) from WordProgress p where p.user.id = :userId and p.masteryLevel >= :level")
    long countMastered(@Param("userId") Long userId, @Param("level") int level);

    @Query("select coalesce(sum(p.reviewCount), 0) from WordProgress p where p.user.id = :userId")
    long sumReviewCount(@Param("userId") Long userId);
}
