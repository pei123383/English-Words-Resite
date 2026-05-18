package com.englishwords;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EnglishWordsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void supportsRegisterBookImportQuizAndProgressFlow() throws Exception {
        String token = registerAndGetToken("tester");
        long bookId = createBook(token, "CET-4");
        importWords(token, bookId, """
            word,translation,phonetic,example,tags
            apple,apple,/apl/,An apple.,food
            apple,red apple,/apl/,A red apple.,food
            book,book,,,study
            cat,cat,,,animal
            dog,dog,,,animal
            """);

        mockMvc.perform(get("/api/words")
                .param("bookId", String.valueOf(bookId))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(4));

        String quizJson = mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(bookId))
                .param("count", "1")
                .param("mode", "EN_TO_CN")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].options", hasSize(4)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode question = objectMapper.readTree(quizJson).path("data").get(0);
        long wordId = question.path("wordId").asLong();
        String answer = question.path("answer").asText();
        Set<String> options = new HashSet<>();
        question.path("options").forEach(option -> options.add(option.asText()));
        org.assertj.core.api.Assertions.assertThat(options)
            .hasSize(4)
            .contains(answer);

        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"wordId":%d,"mode":"EN_TO_CN","selectedAnswer":"%s","responseTimeMs":4000}
                    """.formatted(wordId, answer))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.correct").value(true))
            .andExpect(jsonPath("$.data.quality").value(5))
            .andExpect(jsonPath("$.data.correctAnswer").value(answer))
            .andExpect(jsonPath("$.data.progress.reviewCount").value(1))
            .andExpect(jsonPath("$.data.progress.masteryLevel").value(1));

        mockMvc.perform(get("/api/progress/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalWords").value(4))
            .andExpect(jsonPath("$.data.totalReviews").value(1));
    }

    @Test
    void randomQuestionFallsBackToOtherBooksForDistractors() throws Exception {
        String token = registerAndGetToken("fallback-tester");
        long smallBookId = createBook(token, "Small");
        long otherBookId = createBook(token, "Other");
        importWords(token, smallBookId, """
            word,translation
            solo,solo
            paired,paired
            """);
        importWords(token, otherBookId, """
            word,translation
            river,river
            mountain,mountain
            forest,forest
            """);

        mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(smallBookId))
                .param("count", "1")
                .param("mode", "EN_TO_CN")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].options", hasSize(4)));
    }

    @Test
    void doesNotReturnQuestionWithFewerThanTwoOptions() throws Exception {
        String token = registerAndGetToken("single-word-tester");
        long bookId = createBook(token, "Single");
        importWords(token, bookId, """
            word,translation
            solo,solo
            """);

        mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(bookId))
                .param("count", "1")
                .param("mode", "EN_TO_CN")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void autoScoresCorrectAndWrongAnswersByResponseTime() throws Exception {
        String token = registerAndGetToken("score-tester");
        long bookId = createBook(token, "Scoring");
        importWords(token, bookId, """
            word,translation
            fast,fast
            medium,medium
            slow,slow
            wrongfast,wrongfast
            wrongmedium,wrongmedium
            wrongslow,wrongslow
            distractor,distractor
            """);

        assertSubmitQuality(token, bookId, "fast", "fast", 4_000, 5, true);
        assertSubmitQuality(token, bookId, "medium", "medium", 10_000, 4, true);
        assertSubmitQuality(token, bookId, "slow", "slow", 16_000, 3, true);
        assertSubmitQuality(token, bookId, "wrongfast", "nope", 4_000, 2, false);
        assertSubmitQuality(token, bookId, "wrongmedium", "nope", 10_000, 1, false);
        assertSubmitQuality(token, bookId, "wrongslow", "nope", 16_000, 0, false);
    }

    @Test
    void reviewOnlyIncludesPreviouslyReviewedWordsEvenBeforeDueTime() throws Exception {
        String token = registerAndGetToken("due-review-tester");
        long bookId = createBook(token, "Due Review");
        importWords(token, bookId, """
            word,translation
            fresh,fresh
            learned,learned
            helper,helper
            spare,spare
            """);

        mockMvc.perform(get("/api/progress/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalWords").value(4))
            .andExpect(jsonPath("$.data.dueWords").value(0));

        mockMvc.perform(get("/api/progress/due")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(bookId))
                .param("count", "10")
                .param("mode", "EN_TO_CN")
                .param("onlyDue", "true")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));

        assertSubmitQuality(token, bookId, "learned", "learned", 4_000, 5, true);

        mockMvc.perform(get("/api/progress/overview")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalReviews").value(1))
            .andExpect(jsonPath("$.data.dueWords").value(1));

        mockMvc.perform(get("/api/progress/due")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].word").value("learned"));

        mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(bookId))
                .param("count", "10")
                .param("mode", "EN_TO_CN")
                .param("onlyDue", "true")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].word").value("learned"));
    }

    @Test
    void comparesEnglishAnswersCaseInsensitively() throws Exception {
        String token = registerAndGetToken("case-tester");
        long bookId = createBook(token, "Case");
        importWords(token, bookId, """
            word,translation
            Apple,fruit
            Book,object
            Cat,animal
            Dog,animal
            """);

        long wordId = findWordIdByPrompt(token, bookId, "fruit", "CN_TO_EN");
        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"wordId":%d,"mode":"CN_TO_EN","selectedAnswer":"apple","responseTimeMs":4000}
                    """.formatted(wordId))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.correct").value(true))
            .andExpect(jsonPath("$.data.quality").value(5));
    }

    @Test
    void rejectsAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/word-books"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void importsCsvWithUtf8BomHeader() throws Exception {
        String token = registerAndGetToken("bom-tester");
        long bookId = createBook(token, "BOM CSV");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "bom-words.csv",
            "text/csv",
            "\uFEFFword,translation,phonetic,example,tags\nmemory,memory,/memory/,Memory improves with sleep.,CET4\n"
                .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/words/import")
                .file(file)
                .param("bookId", String.valueOf(bookId))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalRows").value(1))
            .andExpect(jsonPath("$.data.createdCount").value(1))
            .andExpect(jsonPath("$.data.updatedCount").value(0))
            .andExpect(jsonPath("$.data.skippedCount").value(0));
    }

    @Test
    void searchesBuiltInDictionary() throws Exception {
        String token = registerAndGetToken("dictionary-tester");

        mockMvc.perform(get("/api/dictionary")
                .param("keyword", "app")
                .param("size", "10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.items[0].word").value("apple"))
            .andExpect(jsonPath("$.data.items[1].word").value("application"));

        mockMvc.perform(get("/api/dictionary")
                .param("keyword", "网络")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].word").value("network"));
    }

    @Test
    void rejectsCsvMissingRequiredHeaders() throws Exception {
        String token = registerAndGetToken("bad-header-tester");
        long bookId = createBook(token, "Bad Header CSV");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "bad-words.csv",
            "text/csv",
            "term,meaning\nmemory,memory\n".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/words/import")
                .file(file)
                .param("bookId", String.valueOf(bookId))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    private void assertSubmitQuality(
        String token,
        long bookId,
        String prompt,
        String selectedAnswer,
        long responseTimeMs,
        int expectedQuality,
        boolean expectedCorrect
    ) throws Exception {
        long wordId = findWordIdByPrompt(token, bookId, prompt, "EN_TO_CN");
        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"wordId":%d,"mode":"EN_TO_CN","selectedAnswer":"%s","responseTimeMs":%d}
                    """.formatted(wordId, selectedAnswer, responseTimeMs))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.correct").value(expectedCorrect))
            .andExpect(jsonPath("$.data.quality").value(expectedQuality));
    }

    private long findWordIdByPrompt(String token, long bookId, String prompt, String mode) throws Exception {
        String quizJson = mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(bookId))
                .param("count", "100")
                .param("mode", mode)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].prompt", hasItem(prompt)))
            .andReturn()
            .getResponse()
            .getContentAsString();

        for (JsonNode question : objectMapper.readTree(quizJson).path("data")) {
            if (prompt.equals(question.path("prompt").asText())) {
                return question.path("wordId").asLong();
            }
        }
        throw new AssertionError("Question not found: " + prompt);
    }

    private void importWords(String token, long bookId, String csv) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "words.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/words/import")
                .file(file)
                .param("bookId", String.valueOf(bookId))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    private String registerAndGetToken(String username) throws Exception {
        String json = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"secret123","nickname":"Tester"}
                    """.formatted(username)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(json).path("data").path("token").asText();
    }

    private long createBook(String token, String name) throws Exception {
        String json = mockMvc.perform(post("/api/word-books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s","description":"test book"}
                    """.formatted(name))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path("id").asLong();
    }
}
