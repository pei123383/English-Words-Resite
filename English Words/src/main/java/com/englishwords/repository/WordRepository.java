package com.englishwords.repository;

import com.englishwords.entity.Word;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WordRepository extends JpaRepository<Word, Long>, JpaSpecificationExecutor<Word> {

    Optional<Word> findByIdAndUserId(Long id, Long userId);

    Optional<Word> findByUserIdAndWordBookIdAndTermIgnoreCase(Long userId, Long wordBookId, String term);

    long countByUserId(Long userId);

    List<Word> findByUserIdAndWordBookIdAndIdNot(Long userId, Long wordBookId, Long id);

    List<Word> findByUserIdAndWordBookIdNotAndIdNot(Long userId, Long wordBookId, Long id);

    @Query("select lower(w.term) from Word w where w.user.id = :userId and w.wordBook.id = :wordBookId")
    List<String> findLowerTermsByUserIdAndWordBookId(@Param("userId") Long userId, @Param("wordBookId") Long wordBookId);

    @Query("""
        select w from Word w
        join w.progress p
        where w.user.id = :userId
          and (:bookId is null or w.wordBook.id = :bookId)
          and (:onlyDue = false or p.reviewCount > 0)
        order by function('RAND')
        """)
    List<Word> findRandomWords(
        @Param("userId") Long userId,
        @Param("bookId") Long bookId,
        @Param("onlyDue") boolean onlyDue,
        Pageable pageable
    );
}
