package com.englishwords;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.preset-word-book.enabled=true")
class PresetWordBookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsPresetBookForNewUser() throws Exception {
        String token = registerAndGetToken("preset-user");

        String booksJson = mockMvc.perform(get("/api/word-books")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].name").value("六级核心词汇"))
            .andExpect(jsonPath("$.data[0].wordCount").value(2219))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long bookId = objectMapper.readTree(booksJson).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/words")
                .param("bookId", String.valueOf(bookId))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2219))
            .andExpect(jsonPath("$.data.items[*].word", hasItem("zinc")));

        mockMvc.perform(get("/api/quizzes/random")
                .param("bookId", String.valueOf(bookId))
                .param("count", "1")
                .param("mode", "EN_TO_CN")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].options", hasSize(4)));
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
        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path("token").asText();
    }
}
