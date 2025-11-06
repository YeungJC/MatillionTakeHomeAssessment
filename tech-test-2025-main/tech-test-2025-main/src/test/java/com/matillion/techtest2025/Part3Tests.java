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
 * This test suite covers the following extended functionality:
 * <ul>
 *   <li>ID field functionality added to POST and GET responses</li>
 *   <li>Optional name parameter support in POST endpoint</li>
 * </ul>
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

    // ==================== NAME PARAMETER TESTS ====================

    /**
     * Tests that the POST endpoint accepts an optional name parameter.
     * <p>
     * The CSV contains Formula 1 driver data:
     * - 3 rows of F1 drivers
     * - 3 columns: driver, number, team
     * <p>
     * Expected behavior:
     * - POST endpoint should accept name as a query parameter
     * - Response should include the provided name
     * - Name should be stored in the database
     */
    @Test
    void shouldAcceptNameParameterInPostEndpoint(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);
        String analysisName = "F1 Drivers 2024";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", analysisName)
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.name()).isEqualTo(analysisName);
        assertThat(response.id()).isNotNull();
    }

    /**
     * Tests that the POST endpoint works without providing a name parameter.
     * <p>
     * Expected behavior:
     * - POST endpoint should work when name is not provided
     * - Response should have null name
     * - All other fields should be populated correctly
     */
    @Test
    void shouldWorkWithoutNameParameter(
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

        assertThat(response.name()).isNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.numberOfRows()).isEqualTo(3);
        assertThat(response.numberOfColumns()).isEqualTo(3);
    }

    /**
     * Tests that names with special characters are handled correctly.
     * <p>
     * Expected behavior:
     * - Names with spaces, numbers, and special characters should be accepted
     * - Name should be stored and retrieved exactly as provided
     */
    @Test
    void shouldHandleNamesWithSpecialCharacters(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);
        String complexName = "Sales Data Q4 2024 - Region A&B";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", complexName)
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.name()).isEqualTo(complexName);
    }

    /**
     * Tests that the GET endpoint includes the name in the response.
     * <p>
     * Expected behavior:
     * - GET endpoint should return the name that was provided during ingestion
     * - Name should match exactly what was saved
     */
    @Test
    void shouldReturnNameInGetEndpoint(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);
        String analysisName = "Driver Analysis 2024";

        var postResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", analysisName)
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

        assertThat(getResponse.name()).isEqualTo(analysisName);
        assertThat(getResponse.id()).isEqualTo(analysisId);
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
