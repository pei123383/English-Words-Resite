package com.englishwords.repository;

import com.englishwords.entity.WordBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WordBookRepository extends JpaRepository<WordBook, Long> {

    List<WordBook> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<WordBook> findByIdAndUserId(Long id, Long userId);

    Optional<WordBook> findByUserIdAndPresetKey(Long userId, String presetKey);

    @Query("select count(w) from Word w where w.wordBook.id = :bookId and w.user.id = :userId")
    long countWords(@Param("bookId") Long bookId, @Param("userId") Long userId);
}
