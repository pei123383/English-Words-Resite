package com.englishwords.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishwords.entity.WordProgress;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class Sm2ServiceTest {

    private final Sm2Service sm2Service = new Sm2Service();

    @Test
    void appliesFirstSuccessfulReview() {
        WordProgress progress = new WordProgress();
        progress.setEaseFactor(2.5);

        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 12, 0);
        sm2Service.applyReview(progress, 5, now);

        assertThat(progress.getReviewCount()).isEqualTo(1);
        assertThat(progress.getCorrectCount()).isEqualTo(1);
        assertThat(progress.getRepetition()).isEqualTo(1);
        assertThat(progress.getIntervalDays()).isEqualTo(1);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(1));
        assertThat(progress.getMasteryLevel()).isEqualTo(1);
    }

    @Test
    void failedReviewResetsRepetitionButKeepsReviewCount() {
        WordProgress progress = new WordProgress();
        progress.setEaseFactor(2.5);
        progress.setRepetition(3);
        progress.setIntervalDays(10);
        progress.setReviewCount(3);
        progress.setCorrectCount(3);

        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 12, 0);
        sm2Service.applyReview(progress, 2, now);

        assertThat(progress.getReviewCount()).isEqualTo(4);
        assertThat(progress.getWrongCount()).isEqualTo(1);
        assertThat(progress.getRepetition()).isZero();
        assertThat(progress.getIntervalDays()).isEqualTo(1);
        assertThat(progress.getEaseFactor()).isGreaterThanOrEqualTo(1.3);
        assertThat(progress.getNextReviewAt()).isEqualTo(now.plusDays(1));
    }
}
