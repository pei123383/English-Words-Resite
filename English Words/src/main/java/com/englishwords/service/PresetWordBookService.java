package com.englishwords.service;

import com.englishwords.config.PresetWordBookProperties;
import com.englishwords.entity.User;
import com.englishwords.entity.Word;
import com.englishwords.entity.WordBook;
import com.englishwords.entity.WordProgress;
import com.englishwords.repository.WordBookRepository;
import com.englishwords.repository.WordProgressRepository;
import com.englishwords.repository.WordRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresetWordBookService {

    private final PresetWordBookProperties properties;
    private final ResourceLoader resourceLoader;
    private final WordBookRepository wordBookRepository;
    private final WordRepository wordRepository;
    private final WordProgressRepository progressRepository;

    public PresetWordBookService(
        PresetWordBookProperties properties,
        ResourceLoader resourceLoader,
        WordBookRepository wordBookRepository,
        WordRepository wordRepository,
        WordProgressRepository progressRepository
    ) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.wordBookRepository = wordBookRepository;
        this.wordRepository = wordRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional
    public void ensureForUser(User user) {
        if (!properties.enabled()) {
            return;
        }

        WordBook wordBook = wordBookRepository.findByUserIdAndPresetKey(user.getId(), properties.key())
            .orElseGet(() -> createWordBook(user));
        importMissingWords(user, wordBook);
    }

    private WordBook createWordBook(User user) {
        WordBook wordBook = new WordBook();
        wordBook.setUser(user);
        wordBook.setName(properties.name());
        wordBook.setDescription(properties.description());
        wordBook.setPresetKey(properties.key());
        return wordBookRepository.save(wordBook);
    }

    private void importMissingWords(User user, WordBook wordBook) {
        List<PresetWord> presetWords = loadPresetWords();
        if (wordBookRepository.countWords(wordBook.getId(), user.getId()) >= presetWords.size()) {
            return;
        }

        Set<String> existingTerms = new HashSet<>(
            wordRepository.findLowerTermsByUserIdAndWordBookId(user.getId(), wordBook.getId())
        );
        for (PresetWord presetWord : presetWords) {
            if (!existingTerms.add(presetWord.term().toLowerCase(Locale.ROOT))) {
                continue;
            }
            Word word = new Word();
            word.setUser(user);
            word.setWordBook(wordBook);
            word.setTerm(presetWord.term());
            word.setTranslation(presetWord.translation());
            wordRepository.save(word);
            createProgress(user, word);
        }
    }

    private List<PresetWord> loadPresetWords() {
        Resource resource = resourceLoader.getResource(properties.resource());
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader("word", "translation")
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(reader)
        ) {
            List<PresetWord> presetWords = new ArrayList<>();
            for (CSVRecord record : parser) {
                String term = get(record, "word");
                String translation = get(record, "translation");
                if (term == null || translation == null) {
                    continue;
                }
                presetWords.add(new PresetWord(term, translation));
            }
            return presetWords;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to import preset word book", ex);
        }
    }

    private void createProgress(User user, Word word) {
        WordProgress progress = new WordProgress();
        progress.setUser(user);
        progress.setWord(word);
        progress.setEaseFactor(2.5);
        progress.setMasteryLevel(0);
        progressRepository.save(progress);
        word.setProgress(progress);
    }

    private String get(CSVRecord record, String name) {
        if (!record.isMapped(name)) {
            return null;
        }
        String value = record.get(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            value = value.substring(1).trim();
        }
        return value.isEmpty() ? null : value;
    }

    private record PresetWord(String term, String translation) {
    }
}
