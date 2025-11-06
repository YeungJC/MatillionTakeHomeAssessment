package com.matillion.techtest2025;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import com.matillion.techtest2025.service.DataAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Part 3: Additional Features Implementation Tests
 * <p>
 * This test suite covers the following extended functionality:
 * <ul>
 *   <li>ID field functionality added to POST and GET responses</li>
 *   <li>Optional name parameter support in POST endpoint</li>
 *   <li>List all analyses endpoint</li>
 *   <li>Markdown conversion utility for exporting CSV data as GitHub-flavored Markdown tables</li>
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

    // ==================== LIST ALL ANALYSES TESTS ====================

    /**
     * Tests the GET /api/analysis endpoint that lists all analyses.
     * <p>
     * Expected behavior:
     * - GET /api/analysis should return a list of all analyses
     * - Each item should contain full analysis details
     * - Empty database should return empty list
     */
    @Test
    void shouldListAllAnalyses(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        // Initially, list should be empty
        var emptyResult = mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andReturn();

        List<DataAnalysisResponse> emptyList = objectMapper.readValue(
                emptyResult.getResponse().getContentAsString(),
                new TypeReference<List<DataAnalysisResponse>>() {}
        );

        assertThat(emptyList).isEmpty();

        // Add three analyses
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Analysis A")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Analysis B")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Analysis C")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        // Now list should contain three analyses
        var listResult = mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andReturn();

        List<DataAnalysisResponse> analyses = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                new TypeReference<List<DataAnalysisResponse>>() {}
        );

        assertThat(analyses).hasSize(3);
        assertThat(analyses)
                .extracting(DataAnalysisResponse::name)
                .containsExactlyInAnyOrder("Analysis A", "Analysis B", "Analysis C");
    }

    /**
     * Tests that list endpoint returns full analysis details.
     * <p>
     * Expected behavior:
     * - List should contain complete analysis objects
     * - Each analysis should have all fields populated (id, name, rows, columns, statistics, etc.)
     * - Full analysis details (rows, columns, statistics) should be included
     */
    @Test
    void shouldReturnFullDetailsInList(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Test Analysis")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var result = mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andReturn();

        List<DataAnalysisResponse> analyses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<DataAnalysisResponse>>() {}
        );

        assertThat(analyses).hasSize(1);
        DataAnalysisResponse analysis = analyses.get(0);
        assertThat(analysis.id()).isNotNull();
        assertThat(analysis.name()).isEqualTo("Test Analysis");
        assertThat(analysis.numberOfRows()).isEqualTo(3);
        assertThat(analysis.numberOfColumns()).isEqualTo(3);
        assertThat(analysis.totalCharacters()).isPositive();
        assertThat(analysis.columnStatistics()).isNotEmpty();
        assertThat(analysis.createdAt()).isNotNull();
    }

    /**
     * Tests that analyses without names are included in the list.
     * <p>
     * Expected behavior:
     * - Analyses without names should still appear in the list
     * - Name field should be null for analyses without names
     * - All other fields should be populated correctly
     */
    @Test
    void shouldIncludeAnalysesWithoutNamesInList(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Named Analysis")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var result = mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andReturn();

        List<DataAnalysisResponse> analyses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<DataAnalysisResponse>>() {}
        );

        assertThat(analyses).hasSize(2);
        assertThat(analyses)
                .anyMatch(a -> a.name() != null && a.name().equals("Named Analysis"))
                .anyMatch(a -> a.name() == null);

        // Verify that unnamed analysis still has all other fields populated
        DataAnalysisResponse unnamedAnalysis = analyses.stream()
                .filter(a -> a.name() == null)
                .findFirst()
                .orElseThrow();
        assertThat(unnamedAnalysis.id()).isNotNull();
        assertThat(unnamedAnalysis.numberOfRows()).isEqualTo(3);
        assertThat(unnamedAnalysis.numberOfColumns()).isEqualTo(3);
        assertThat(unnamedAnalysis.totalCharacters()).isPositive();
        assertThat(unnamedAnalysis.columnStatistics()).isNotEmpty();
    }

    // ==================== MARKDOWN CONVERSION TESTS ====================

    /**
     * Tests successful conversion of CSV to Markdown table format.
     * <p>
     * Expected behavior:
     * - GET /api/analysis/{id}/markdown should return markdown formatted table
     * - Response should be properly formatted with header row, separator, and data rows
     * - HTTP status should be 200 OK
     */
    @Test
    void shouldConvertCsvToMarkdown(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var postResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "F1Drivers")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        Long analysisId = postResponse.id();

        var markdownResult = mockMvc.perform(get("/api/analysis/" + analysisId + "/markdown"))
                .andExpect(status().isOk())
                .andReturn();

        String markdown = markdownResult.getResponse().getContentAsString();

        // Verify markdown format
        assertThat(markdown).isNotEmpty();
        assertThat(markdown).contains("| driver | number | team |");
        assertThat(markdown).contains("| --- | --- | --- |");
        assertThat(markdown).contains("| Max Verstappen | 1 | Red Bull Racing |");
        assertThat(markdown).contains("| Lewis Hamilton | 44 | Mercedes |");
        assertThat(markdown).contains("| Charles Leclerc | 16 | Ferrari |");
    }

    /**
     * Tests that markdown endpoint returns proper Content-Type header.
     * <p>
     * Expected behavior:
     * - Content-Type header should be "text/markdown"
     */
    @Test
    void shouldReturnMarkdownContentType(
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

        mockMvc.perform(get("/api/analysis/" + postResponse.id() + "/markdown"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown"));
    }

    /**
     * Tests that markdown endpoint returns proper Content-Disposition header with filename.
     * <p>
     * Expected behavior:
     * - Content-Disposition header should be set for file download
     * - Filename should match the analysis name with .md extension
     */
    @Test
    void shouldReturnContentDispositionHeaderWithName(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);
        String analysisName = "TestMarkdown";

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

        mockMvc.perform(get("/api/analysis/" + postResponse.id() + "/markdown"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                    "form-data; name=\"attachment\"; filename=\"" + analysisName + ".md\""));
    }

    /**
     * Tests that markdown endpoint generates filename from ID when name is not provided.
     * <p>
     * Expected behavior:
     * - When analysis has no name, filename should be "analysis-{id}.md"
     */
    @Test
    void shouldGenerateFilenameFromIdWhenNameIsNull(
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

        mockMvc.perform(get("/api/analysis/" + analysisId + "/markdown"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                    "form-data; name=\"attachment\"; filename=\"analysis-" + analysisId + ".md\""));
    }

    /**
     * Tests markdown conversion with CSV containing null values.
     * <p>
     * Expected behavior:
     * - Null/empty values should be represented as empty cells in markdown
     * - Markdown table structure should remain intact
     */
    @Test
    void shouldHandleNullValuesInMarkdownConversion(
            @Value("classpath:test-data/with-nulls.csv")
            Resource nullsCsv
    ) throws Exception {
        String csvData = nullsCsv.getContentAsString(UTF_8);

        var postResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var markdownResult = mockMvc.perform(get("/api/analysis/" + postResponse.id() + "/markdown"))
                .andExpect(status().isOk())
                .andReturn();

        String markdown = markdownResult.getResponse().getContentAsString();

        // Verify markdown is generated even with null values
        assertThat(markdown).isNotEmpty();
        assertThat(markdown).contains("| --- |"); // Separator row should be present
        assertThat(markdown).contains("|"); // Table structure should be present
    }

    /**
     * Tests that markdown endpoint returns 404 for non-existent analysis.
     * <p>
     * Expected behavior:
     * - GET /api/analysis/{invalid-id}/markdown should return 404
     * - Error response should indicate analysis not found
     */
    @Test
    void shouldReturn404ForNonExistentAnalysisInMarkdown() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(get("/api/analysis/" + nonExistentId + "/markdown"))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests markdown conversion with single row CSV.
     * <p>
     * Expected behavior:
     * - Should generate markdown table with header and one data row
     * - Table structure should be valid
     */
    @Test
    void shouldConvertSingleRowCsvToMarkdown(
            @Value("classpath:test-data/single-row.csv")
            Resource singleRowCsv
    ) throws Exception {
        String csvData = singleRowCsv.getContentAsString(UTF_8);

        var postResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var markdownResult = mockMvc.perform(get("/api/analysis/" + postResponse.id() + "/markdown"))
                .andExpect(status().isOk())
                .andReturn();

        String markdown = markdownResult.getResponse().getContentAsString();

        // Verify markdown has header, separator, and exactly one data row
        assertThat(markdown).isNotEmpty();
        String[] lines = markdown.split("\n");
        assertThat(lines.length).isEqualTo(3); // header + separator + 1 data row
        assertThat(lines[1]).contains("---"); // Separator row
    }

    /**
     * Tests that markdown table format is correct for different CSV structures.
     * <p>
     * Expected behavior:
     * - Each column should have proper markdown table cell delimiters
     * - Separator row should have correct number of --- columns
     * - All data should be properly aligned in cells
     */
    @Test
    void shouldGenerateProperMarkdownTableStructure(
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

        var markdownResult = mockMvc.perform(get("/api/analysis/" + postResponse.id() + "/markdown"))
                .andExpect(status().isOk())
                .andReturn();

        String markdown = markdownResult.getResponse().getContentAsString();
        String[] lines = markdown.split("\n");

        // Verify header row starts and ends with pipe
        assertThat(lines[0]).startsWith("|");
        assertThat(lines[0]).endsWith("|");

        // Verify separator row has correct number of columns (3 columns = 3 separators)
        long separatorCount = lines[1].chars().filter(ch -> ch == '|').count();
        assertThat(separatorCount).isEqualTo(4); // 3 columns means 4 pipes (start + 3 delimiters)

        // Verify all data rows have same structure
        for (int i = 2; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                assertThat(lines[i]).startsWith("|");
                assertThat(lines[i]).endsWith("|");
            }
        }
    }
}
