package com.englishwords.service;

import com.englishwords.entity.WordProgress;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class Sm2Service {

    public WordProgress applyReview(WordProgress progress, int quality, LocalDateTime reviewedAt) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("quality must be between 0 and 5");
        }

        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setLastReviewedAt(reviewedAt);

        boolean remembered = quality >= 3;
        if (remembered) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);
            int nextRepetition = progress.getRepetition() + 1;
            progress.setRepetition(nextRepetition);
            if (nextRepetition == 1) {
                progress.setIntervalDays(1);
            } else if (nextRepetition == 2) {
                progress.setIntervalDays(6);
            } else {
                progress.setIntervalDays(Math.max(1, (int) Math.round(progress.getIntervalDays() * progress.getEaseFactor())));
            }
        } else {
            progress.setWrongCount(progress.getWrongCount() + 1);
            progress.setRepetition(0);
            progress.setIntervalDays(1);
        }

        double qualityGap = 5 - quality;
        double nextEaseFactor = progress.getEaseFactor()
            + (0.1 - qualityGap * (0.08 + qualityGap * 0.02));
        progress.setEaseFactor(Math.max(1.3, round(nextEaseFactor)));
        progress.setMasteryLevel(calculateMasteryLevel(progress));
        progress.setNextReviewAt(reviewedAt.plusDays(progress.getIntervalDays()));
        return progress;
    }

    public WordProgress reset(WordProgress progress) {
        progress.setReviewCount(0);
        progress.setCorrectCount(0);
        progress.setWrongCount(0);
        progress.setEaseFactor(2.5);
        progress.setIntervalDays(0);
        progress.setRepetition(0);
        progress.setMasteryLevel(0);
        progress.setNextReviewAt(null);
        progress.setLastReviewedAt(null);
        return progress;
    }

    public int calculateMasteryLevel(WordProgress progress) {
        if (progress.getReviewCount() == 0) {
            return 0;
        }
        double accuracy = (double) progress.getCorrectCount() / progress.getReviewCount();
        int score = 0;
        if (progress.getRepetition() >= 1 && accuracy >= 0.5) {
            score = 1;
        }
        if (progress.getRepetition() >= 2 && accuracy >= 0.6) {
            score = 2;
        }
        if (progress.getRepetition() >= 3 && accuracy >= 0.7 && progress.getIntervalDays() >= 7) {
            score = 3;
        }
        if (progress.getRepetition() >= 4 && accuracy >= 0.8 && progress.getIntervalDays() >= 14) {
            score = 4;
        }
        if (progress.getRepetition() >= 5 && accuracy >= 0.85 && progress.getIntervalDays() >= 30) {
            score = 5;
        }
        return score;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
