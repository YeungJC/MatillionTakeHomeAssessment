package com.matillion.techtest2025;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 3: Additional Features Implementation Tests
 * <p>
 * This test suite covers the ID field functionality added to POST and GET responses.
 * <p>
 * <b>Prerequisites:</b> Part 1 and Part 2 must be completed before Part 3 can be implemented.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Part3Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataAnalysisRepository dataAnalysisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        dataAnalysisRepository.deleteAll();
    }

    // ==================== ID FIELD TESTS ====================

    /**
     * Tests that the POST endpoint returns an ID in the response.
     * <p>
     * Expected behavior:
     * - Response should include a non-null ID
     * - ID should be a positive Long value
     * - ID should be unique for each analysis
     */
    @Test
    void shouldReturnIdInPostResponse(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.id()).isPositive();
    }

    /**
     * Tests that multiple analyses receive different IDs.
     * <p>
     * Expected behavior:
     * - Each analysis should receive a unique ID
     * - IDs should be auto-incremented
     */
    @Test
    void shouldAssignUniqueIdsToMultipleAnalyses(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result1 = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        var result2 = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        DataAnalysisResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response1.id()).isNotNull();
        assertThat(response2.id()).isNotNull();
        assertThat(response1.id()).isNotEqualTo(response2.id());
    }

    /**
     * Tests that the GET endpoint returns the same ID that was assigned during POST.
     * <p>
     * Expected behavior:
     * - GET endpoint should return the ID that was assigned during POST
     * - ID should match exactly between POST and GET
     */
    @Test
    void shouldReturnSameIdInGetEndpoint(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var postResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        Long analysisId = postResponse.id();

        var getResult = mockMvc.perform(get("/api/analysis/" + analysisId))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse getResponse = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(getResponse.id()).isEqualTo(analysisId);
    }
}
