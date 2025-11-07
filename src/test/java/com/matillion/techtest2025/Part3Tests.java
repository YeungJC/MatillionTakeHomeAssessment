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
 *   <li>Token counting utility for estimating LLM context usage (CSV and Markdown formats)</li>
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

    // ==================== TOKEN COUNTING TESTS ====================

    /**
     * Tests that CSV token count is returned in POST response.
     * <p>
     * Expected behavior:
     * - POST endpoint should calculate and return csvTokenCount
     * - CSV token count should be a positive integer
     * - Token count should represent the number of tokens in the raw CSV data
     */
    @Test
    void shouldReturnCsvTokenCountInPostResponse(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "TokenTest")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.csvTokenCount()).isPositive();
    }

    /**
     * Tests that Markdown token count is returned in POST response.
     * <p>
     * Expected behavior:
     * - POST endpoint should calculate and return markdownTokenCount
     * - Markdown token count should be a positive integer
     * - Token count should represent the number of tokens in the markdown table format
     */
    @Test
    void shouldReturnMarkdownTokenCountInPostResponse(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "TokenTest")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.markdownTokenCount()).isPositive();
    }

    /**
     * Tests that markdown token count is typically higher than CSV token count.
     * <p>
     * Expected behavior:
     * - Markdown format includes formatting characters (|, ---, spaces)
     * - Markdown token count should generally be higher than CSV token count
     * - This helps users understand the token overhead of using markdown format
     */
    @Test
    void shouldShowMarkdownTokenCountHigherThanCsvTokenCount(
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

        // Markdown format typically uses more tokens due to formatting
        assertThat(response.markdownTokenCount()).isGreaterThan(response.csvTokenCount());
    }

    /**
     * Tests that token counts are persisted and returned in GET endpoint.
     * <p>
     * Expected behavior:
     * - Token counts calculated during POST should be stored in database
     * - GET endpoint should return the same token counts
     * - Values should match exactly between POST and GET
     */
    @Test
    void shouldPersistAndRetrieveTokenCounts(
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
        int expectedCsvTokenCount = postResponse.csvTokenCount();
        int expectedMarkdownTokenCount = postResponse.markdownTokenCount();

        var getResult = mockMvc.perform(get("/api/analysis/" + analysisId))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse getResponse = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(getResponse.csvTokenCount()).isEqualTo(expectedCsvTokenCount);
        assertThat(getResponse.markdownTokenCount()).isEqualTo(expectedMarkdownTokenCount);
    }

    /**
     * Tests that token counts are included in list all analyses endpoint.
     * <p>
     * Expected behavior:
     * - GET /api/analysis should include token counts for all analyses
     * - Each analysis in the list should have both csvTokenCount and markdownTokenCount
     * - Values should be consistent with individual GET endpoint
     */
    @Test
    void shouldIncludeTokenCountsInListEndpoint(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

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

        var result = mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andReturn();

        List<DataAnalysisResponse> analyses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<DataAnalysisResponse>>() {}
        );

        assertThat(analyses).hasSize(2);
        for (DataAnalysisResponse analysis : analyses) {
            assertThat(analysis.csvTokenCount()).isPositive();
            assertThat(analysis.markdownTokenCount()).isPositive();
            assertThat(analysis.markdownTokenCount()).isGreaterThan(analysis.csvTokenCount());
        }
    }

    /**
     * Tests token counting with different CSV sizes.
     * <p>
     * Expected behavior:
     * - Larger CSVs should have higher token counts
     * - Token counts should scale proportionally with data size
     */
    @Test
    void shouldScaleTokenCountsWithDataSize(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv,
            @Value("classpath:test-data/large.csv")
            Resource largeCsv
    ) throws Exception {
        String smallCsvData = simpleCsv.getContentAsString(UTF_8);
        String largeCsvData = largeCsv.getContentAsString(UTF_8);

        var smallResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Small CSV")
                        .contentType(TEXT_PLAIN)
                        .content(smallCsvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse smallResponse = objectMapper.readValue(
                smallResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var largeResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Large CSV")
                        .contentType(TEXT_PLAIN)
                        .content(largeCsvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse largeResponse = objectMapper.readValue(
                largeResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        // Large CSV should have more tokens than small CSV
        assertThat(largeResponse.csvTokenCount()).isGreaterThan(smallResponse.csvTokenCount());
        assertThat(largeResponse.markdownTokenCount()).isGreaterThan(smallResponse.markdownTokenCount());
    }

    /**
     * Tests that token counts are non-zero for valid CSVs.
     * <p>
     * Expected behavior:
     * - Any valid CSV with data should produce non-zero token counts
     * - Both CSV and markdown token counts should be greater than zero
     */
    @Test
    void shouldProduceNonZeroTokenCountsForValidCsv(
            @Value("classpath:test-data/single-row.csv")
            Resource singleRowCsv
    ) throws Exception {
        String csvData = singleRowCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.csvTokenCount()).isGreaterThan(0);
        assertThat(response.markdownTokenCount()).isGreaterThan(0);
    }

    /**
     * Tests that token counts are consistent across multiple uploads of the same CSV.
     * <p>
     * Expected behavior:
     * - Uploading the same CSV multiple times should produce identical token counts
     * - Token counting should be deterministic
     */
    @Test
    void shouldProduceConsistentTokenCountsForSameCsv(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result1 = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var result2 = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        // Token counts should be identical for the same CSV
        assertThat(response1.csvTokenCount()).isEqualTo(response2.csvTokenCount());
        assertThat(response1.markdownTokenCount()).isEqualTo(response2.markdownTokenCount());
    }

    // ==================== STATISTICAL ANALYSIS TESTS ====================

    /**
     * Tests that mean, median, and standard deviation are calculated for integer columns.
     * <p>
     * Expected behavior:
     * - Integer columns should have statistical measures calculated
     * - Mean should be the average of all values
     * - Median should be the middle value
     * - Standard deviation should measure data spread
     */
    @Test
    void shouldCalculateStatisticsForIntegerColumns() throws Exception {
        // CSV with integer column: age = 20, 30, 40
        String csvData = "name,age\nAlice,20\nBob,30\nCharlie,40";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var ageStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("age"))
                .findFirst()
                .orElseThrow();

        assertThat(ageStats.inferredType()).isEqualTo("INTEGER");
        assertThat(ageStats.mean()).isEqualTo(30.0);
        assertThat(ageStats.median()).isEqualTo(30.0);
        assertThat(ageStats.standardDeviation()).isNotNull();
        assertThat(ageStats.standardDeviation()).isGreaterThan(0);
    }

    /**
     * Tests that mean, median, and standard deviation are calculated for decimal columns.
     * <p>
     * Expected behavior:
     * - Decimal columns should have statistical measures calculated
     * - Statistics should handle decimal precision correctly
     */
    @Test
    void shouldCalculateStatisticsForDecimalColumns() throws Exception {
        // CSV with decimal column: price = 10.5, 20.5, 30.5
        String csvData = "item,price\nA,10.5\nB,20.5\nC,30.5";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var priceStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("price"))
                .findFirst()
                .orElseThrow();

        assertThat(priceStats.inferredType()).isEqualTo("DECIMAL");
        assertThat(priceStats.mean()).isEqualTo(20.5);
        assertThat(priceStats.median()).isEqualTo(20.5);
        assertThat(priceStats.standardDeviation()).isNotNull();
        assertThat(priceStats.standardDeviation()).isGreaterThan(0);
    }

    /**
     * Tests that string columns have null statistical values.
     * <p>
     * Expected behavior:
     * - String columns should not have statistical measures
     * - Mean, median, and standard deviation should be null
     */
    @Test
    void shouldHaveNullStatisticsForStringColumns() throws Exception {
        String csvData = "name,city\nAlice,London\nBob,Paris\nCharlie,Berlin";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var nameStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("name"))
                .findFirst()
                .orElseThrow();

        var cityStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("city"))
                .findFirst()
                .orElseThrow();

        assertThat(nameStats.inferredType()).isEqualTo("STRING");
        assertThat(nameStats.mean()).isNull();
        assertThat(nameStats.median()).isNull();
        assertThat(nameStats.standardDeviation()).isNull();

        assertThat(cityStats.inferredType()).isEqualTo("STRING");
        assertThat(cityStats.mean()).isNull();
        assertThat(cityStats.median()).isNull();
        assertThat(cityStats.standardDeviation()).isNull();
    }

    /**
     * Tests that boolean columns have null statistical values.
     * <p>
     * Expected behavior:
     * - Boolean columns should not have statistical measures
     * - Mean, median, and standard deviation should be null
     */
    @Test
    void shouldHaveNullStatisticsForBooleanColumns() throws Exception {
        String csvData = "name,active\nAlice,true\nBob,false\nCharlie,true";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var activeStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("active"))
                .findFirst()
                .orElseThrow();

        assertThat(activeStats.inferredType()).isEqualTo("BOOLEAN");
        assertThat(activeStats.mean()).isNull();
        assertThat(activeStats.median()).isNull();
        assertThat(activeStats.standardDeviation()).isNull();
    }

    /**
     * Tests that statistical calculations exclude null values.
     * <p>
     * Expected behavior:
     * - Null/empty values should not be included in statistical calculations
     * - Statistics should be calculated only on non-null numeric values
     */
    @Test
    void shouldExcludeNullValuesFromStatisticalCalculations() throws Exception {
        // CSV with nulls: values = 10, [empty], 20, [empty], 30
        String csvData = "id,value\n1,10\n2,\n3,20\n4,\n5,30";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var valueStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("value"))
                .findFirst()
                .orElseThrow();

        // Should calculate statistics on only 3 values: 10, 20, 30
        assertThat(valueStats.inferredType()).isEqualTo("INTEGER");
        assertThat(valueStats.nullCount()).isEqualTo(2);
        assertThat(valueStats.mean()).isEqualTo(20.0); // (10 + 20 + 30) / 3
        assertThat(valueStats.median()).isEqualTo(20.0);
        assertThat(valueStats.standardDeviation()).isNotNull();
    }

    /**
     * Tests that standard deviation is null for single value columns.
     * <p>
     * Expected behavior:
     * - Columns with only one non-null value should have null standard deviation
     * - Mean and median should still be calculated
     */
    @Test
    void shouldReturnNullStandardDeviationForSingleValue() throws Exception {
        String csvData = "name,score\nAlice,100";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var scoreStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("score"))
                .findFirst()
                .orElseThrow();

        assertThat(scoreStats.inferredType()).isEqualTo("INTEGER");
        assertThat(scoreStats.mean()).isEqualTo(100.0);
        assertThat(scoreStats.median()).isEqualTo(100.0);
        assertThat(scoreStats.standardDeviation()).isNull(); // Can't calculate stddev for 1 value
    }

    /**
     * Tests correct mean calculation.
     * <p>
     * Expected behavior:
     * - Mean should be the arithmetic average of all values
     * - Mean = sum of values / count of values
     */
    @Test
    void shouldCalculateMeanCorrectly() throws Exception {
        // Values: 1, 2, 3, 4, 5 -> mean = 15/5 = 3.0
        String csvData = "id,value\n1,1\n2,2\n3,3\n4,4\n5,5";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var valueStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("value"))
                .findFirst()
                .orElseThrow();

        assertThat(valueStats.mean()).isEqualTo(3.0);
    }

    /**
     * Tests correct median calculation for odd number of values.
     * <p>
     * Expected behavior:
     * - For odd count, median should be the middle value
     * - Values: 1, 2, 3, 4, 5 -> median = 3
     */
    @Test
    void shouldCalculateMedianForOddNumberOfValues() throws Exception {
        String csvData = "id,value\n1,1\n2,2\n3,3\n4,4\n5,5";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var valueStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("value"))
                .findFirst()
                .orElseThrow();

        assertThat(valueStats.median()).isEqualTo(3.0);
    }

    /**
     * Tests correct median calculation for even number of values.
     * <p>
     * Expected behavior:
     * - For even count, median should be the average of two middle values
     * - Values: 1, 2, 3, 4 -> median = (2 + 3) / 2 = 2.5
     */
    @Test
    void shouldCalculateMedianForEvenNumberOfValues() throws Exception {
        String csvData = "id,value\n1,1\n2,2\n3,3\n4,4";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var valueStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("value"))
                .findFirst()
                .orElseThrow();

        assertThat(valueStats.median()).isEqualTo(2.5);
    }

    /**
     * Tests that standard deviation measures data spread correctly.
     * <p>
     * Expected behavior:
     * - Standard deviation should be positive for varying data
     * - Higher variation should result in higher standard deviation
     */
    @Test
    void shouldCalculateStandardDeviationCorrectly() throws Exception {
        // Values with known standard deviation
        String csvData = "id,value\n1,10\n2,20\n3,30";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var valueStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("value"))
                .findFirst()
                .orElseThrow();

        // For values 10, 20, 30: mean = 20, sample stddev = 10
        assertThat(valueStats.standardDeviation()).isNotNull();
        assertThat(valueStats.standardDeviation()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.01));
    }

    /**
     * Tests that statistics are persisted and retrievable via GET endpoint.
     * <p>
     * Expected behavior:
     * - Statistical measures calculated during POST should be stored in database
     * - GET endpoint should return the same statistical values
     */
    @Test
    void shouldPersistAndRetrieveStatisticalMeasures() throws Exception {
        String csvData = "name,age\nAlice,25\nBob,35\nCharlie,45";

        var postResult = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var postAgeStats = postResponse.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("age"))
                .findFirst()
                .orElseThrow();

        Double expectedMean = postAgeStats.mean();
        Double expectedMedian = postAgeStats.median();
        Double expectedStdDev = postAgeStats.standardDeviation();

        // Retrieve via GET endpoint
        var getResult = mockMvc.perform(get("/api/analysis/" + postResponse.id()))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse getResponse = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        var getAgeStats = getResponse.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("age"))
                .findFirst()
                .orElseThrow();

        assertThat(getAgeStats.mean()).isEqualTo(expectedMean);
        assertThat(getAgeStats.median()).isEqualTo(expectedMedian);
        assertThat(getAgeStats.standardDeviation()).isEqualTo(expectedStdDev);
    }

    /**
     * Tests that statistics are included in list all analyses endpoint.
     * <p>
     * Expected behavior:
     * - GET /api/analysis should include statistical measures for all analyses
     * - Each column in each analysis should have complete statistical data
     */
    @Test
    void shouldIncludeStatisticsInListEndpoint() throws Exception {
        String csvData = "product,price\nA,10.5\nB,20.5\nC,30.5";

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .param("name", "Price Analysis")
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

        var priceStats = analyses.get(0).columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("price"))
                .findFirst()
                .orElseThrow();

        assertThat(priceStats.inferredType()).isEqualTo("DECIMAL");
        assertThat(priceStats.mean()).isNotNull();
        assertThat(priceStats.median()).isNotNull();
        assertThat(priceStats.standardDeviation()).isNotNull();
    }

    /**
     * Tests that mixed column types have statistics calculated correctly.
     * <p>
     * Expected behavior:
     * - Numeric columns should have statistics
     * - Non-numeric columns should have null statistics
     * - Each column type should be handled independently
     */
    @Test
    void shouldHandleMixedColumnTypesCorrectly() throws Exception {
        String csvData = "name,age,active,salary\nAlice,25,true,50000.5\nBob,35,false,75000.75\nCharlie,45,true,100000.25";

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        // String column - no statistics
        var nameStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("name"))
                .findFirst()
                .orElseThrow();
        assertThat(nameStats.mean()).isNull();
        assertThat(nameStats.median()).isNull();
        assertThat(nameStats.standardDeviation()).isNull();

        // Integer column - has statistics
        var ageStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("age"))
                .findFirst()
                .orElseThrow();
        assertThat(ageStats.mean()).isNotNull();
        assertThat(ageStats.median()).isNotNull();
        assertThat(ageStats.standardDeviation()).isNotNull();

        // Boolean column - no statistics
        var activeStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("active"))
                .findFirst()
                .orElseThrow();
        assertThat(activeStats.mean()).isNull();
        assertThat(activeStats.median()).isNull();
        assertThat(activeStats.standardDeviation()).isNull();

        // Decimal column - has statistics
        var salaryStats = response.columnStatistics().stream()
                .filter(cs -> cs.columnName().equals("salary"))
                .findFirst()
                .orElseThrow();
        assertThat(salaryStats.mean()).isNotNull();
        assertThat(salaryStats.median()).isNotNull();
        assertThat(salaryStats.standardDeviation()).isNotNull();
    }
}
