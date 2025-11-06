package com.matillion.techtest2025.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.exception.BadRequestException;
import com.matillion.techtest2025.exception.NotFoundException;
import com.matillion.techtest2025.model.ColumnStatistics;
import com.matillion.techtest2025.repository.ColumnStatisticsRepository;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import com.matillion.techtest2025.repository.entity.ColumnStatisticsEntity;
import com.matillion.techtest2025.repository.entity.DataAnalysisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service layer containing business logic for data analysis.
 * <p>
 * Responsible for parsing data, calculating statistics, and persisting results.
 */
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

    private final DataAnalysisRepository dataAnalysisRepository;
    private final ColumnStatisticsRepository columnStatisticsRepository;

    // Token encoding for GPT-4/GPT-3.5-turbo (cl100k_base encoding)
    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private static final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    /**
     * Analyzes CSV data and returns statistics.
     * <p>
     * Parses the CSV, calculates statistics (row count, column count, character count,
     * null counts per column), persists the results to the database, and returns the analysis.
     * <p>
     * <b>Note:</b> Current implementation is incomplete. Part 1 of the tech test
     * requires implementing the CSV parsing and analysis logic.
     *
     * @param name the user-provided name for this analysis
     * @param data raw CSV data (rows separated by newlines, columns by commas)
     * @return analysis results
     */
    public DataAnalysisResponse analyzeCsvData(String name, String data) {
        // Parse CSV data
        String[] lines = data.split("\n");

        // Extract header (column names)
        String[] headers = lines[0].split(",", -1);
        int numberOfColumns = headers.length;

        // Count data rows (excluding header)
        int numberOfRows = lines.length - 1;

        // Calculate total characters
        long totalCharacters = data.length();

        // Calculate token count for CSV data
        int csvTokenCount = countTokens(data);

        // Generate markdown for token counting (before database entity creation)
        String markdownVersion = convertCsvToMarkdown(data);
        int markdownTokenCount = countTokens(markdownVersion);

        String[][] dataRows = new String[numberOfRows][];
        for (int i = 1; i < lines.length; i++) {

            String[] row = lines[i].split(",", -1);

            // Validate: each row must have the same number of columns as the header
                if (row.length != numberOfColumns) {
                throw new BadRequestException(
                        String.format("Invalid CSV format: row %d has %d columns, expected %d", i,row.length, numberOfColumns));
             }
             dataRows[i - 1] = row;
        }

        // Create and persist the entity
        OffsetDateTime creationTimestamp = OffsetDateTime.now();

        DataAnalysisEntity dataAnalysisEntity = DataAnalysisEntity.builder()
                .name(name)
                .originalData(data)
                .numberOfRows(numberOfRows)
                .numberOfColumns(numberOfColumns)
                .totalCharacters(totalCharacters)
                .csvTokenCount(csvTokenCount)
                .markdownTokenCount(markdownTokenCount)
                .createdAt(creationTimestamp)
                .build();


        // Create the per-column statistics
        List<ColumnStatisticsEntity> columnStatisticsEntities = new java.util.ArrayList<>();
        for (int col = 0; col < numberOfColumns; col++) {
                String columnName = headers[col];
                int nullCount = 0;
                java.util.Set<String> uniqueValues = new java.util.HashSet<>();
                java.util.List<Double> numericValues = new java.util.ArrayList<>();

                // Count null/empty values and track unique non-null values in this column
                for (String[] row : dataRows) {
                        if (col < row.length) {
                                String value = row[col];
                                if (value == null || value.trim().isEmpty()) {
                                        nullCount++;
                                } else {
                                        uniqueValues.add(value);
                                }
                        }
                }

                String inferredType = inferColumnType(uniqueValues);

                // Calculate statistical measures for numerical columns
                Double mean = null;
                Double median = null;
                Double standardDeviation = null;

                if ("INTEGER".equals(inferredType) || "DECIMAL".equals(inferredType)) {
                        // Collect numeric values from the column
                        for (String[] row : dataRows) {
                                if (col < row.length) {
                                        String value = row[col];
                                        if (value != null && !value.trim().isEmpty()) {
                                                try {
                                                        numericValues.add(Double.parseDouble(value.trim()));
                                                } catch (NumberFormatException e) {
                                                        // Skip non-numeric values
                                                }
                                        }
                                }
                        }

                        // Calculate statistics if we have numeric values
                        if (!numericValues.isEmpty()) {
                                mean = calculateMean(numericValues);
                                median = calculateMedian(numericValues);
                                standardDeviation = calculateStandardDeviation(numericValues, mean);
                        }
                }

                ColumnStatisticsEntity columnStats = ColumnStatisticsEntity.builder()
                        .dataAnalysis(dataAnalysisEntity)
                        .columnName(columnName)
                        .nullCount(nullCount)
                        .uniqueCount(uniqueValues.size())
                        .inferredType(inferredType)
                        .mean(mean)
                        .median(median)
                        .standardDeviation(standardDeviation)
                        .build();

                columnStatisticsEntities.add(columnStats);
        }

        // Add column statistics to parent entity to maintain bidirectional relationship
        dataAnalysisEntity.getColumnStatistics().addAll(columnStatisticsEntities);

        //Save parent entity (cascade will save children)
        dataAnalysisRepository.save(dataAnalysisEntity);

        // Return the analysis values
        return new DataAnalysisResponse(
                dataAnalysisEntity.getId(),
                dataAnalysisEntity.getName(),
                numberOfRows,
                numberOfColumns,
                totalCharacters,
                csvTokenCount,
                markdownTokenCount,
                columnStatisticsEntities
                        .stream()
                        .map(e -> new ColumnStatistics(
                                e.getColumnName(),
                                e.getNullCount(),
                                e.getUniqueCount(),
                                e.getInferredType(),
                                e.getMean(),
                                e.getMedian(),
                                e.getStandardDeviation()
                        ))
                        .toList(),
                creationTimestamp
        );
    }
        /**
         * Retrieves a previously analyzed CSV by its ID.
         *
         * @param id the ID of the analysis to retrieve
         * @return analysis results
         * @throws NotFoundException if no analysis exists with the given ID
         */
        public DataAnalysisResponse getAnalysisById(Long id) {
                DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Analysis not found with id: " + id));

                return new DataAnalysisResponse(
                        entity.getId(),
                        entity.getName(),
                        entity.getNumberOfRows(),
                        entity.getNumberOfColumns(),
                        entity.getTotalCharacters(),
                        entity.getCsvTokenCount(),
                        entity.getMarkdownTokenCount(),
                        entity.getColumnStatistics()
                                .stream()
                                .map(e -> new ColumnStatistics(
                                        e.getColumnName(),
                                        e.getNullCount(),
                                        e.getUniqueCount(),
                                        e.getInferredType(),
                                        e.getMean(),
                                        e.getMedian(),
                                        e.getStandardDeviation()
                                ))
                                .toList(),
                        entity.getCreatedAt()
                );
        }

        /**
         * Deletes an analysis by its ID.
         *
         * @param id the ID of the analysis to delete
         * @throws NotFoundException if no analysis exists with the given ID
         */
        public void deleteAnalysisById(Long id) {
                DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Analysis not found with id: " + id));

                dataAnalysisRepository.delete(entity);
        }

        /**
         * Retrieves a list of all analyses with full details.
         *
         * @return list of all analyses with complete analysis data
         */
        public List<DataAnalysisResponse> getAllAnalyses() {
                return dataAnalysisRepository.findAll()
                        .stream()
                        .map(entity -> new DataAnalysisResponse(
                                entity.getId(),
                                entity.getName(),
                                entity.getNumberOfRows(),
                                entity.getNumberOfColumns(),
                                entity.getTotalCharacters(),
                                entity.getCsvTokenCount(),
                                entity.getMarkdownTokenCount(),
                                entity.getColumnStatistics()
                                        .stream()
                                        .map(e -> new ColumnStatistics(
                                                e.getColumnName(),
                                                e.getNullCount(),
                                                e.getUniqueCount(),
                                                e.getInferredType(),
                                                e.getMean(),
                                                e.getMedian(),
                                                e.getStandardDeviation()
                                        ))
                                        .toList(),
                                entity.getCreatedAt()
                        ))
                        .toList();
        }


            /**
     * Infers the data type of a column based on its unique values.
     * Possible types: STRING, INTEGER, DECIMAL, BOOLEAN
     */
    private String inferColumnType(java.util.Set<String> values) {
        if (values.isEmpty()) return "STRING";

        boolean allIntegers = true;
        boolean allDecimals = true;
        boolean allBooleans = true;

        for (String v : values) {
            String value = v.trim();

            if (!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) {
                allBooleans = false;
            }

            if (!value.matches("-?\\d+")) {
                allIntegers = false;
            }

            if (!value.matches("-?\\d*\\.\\d+")) {
                allDecimals = false;
            }

            if (!allBooleans && !allIntegers && !allDecimals) {
                break;
            }
        }

        if (allBooleans) return "BOOLEAN";
        if (allIntegers) return "INTEGER";
        if (allDecimals) return "DECIMAL";
        return "STRING";
    }

    /**
     * Counts the number of tokens in the provided text using GPT-4 tokenization.
     * <p>
     * This helps users estimate API costs and ensures token limits are not exceeded
     * when using the data in LLM contexts.
     *
     * @param text the text to count tokens for
     * @return the number of tokens
     */
    private int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.countTokens(text);
    }

    /**
     * Converts CSV string to markdown format for internal use (token counting).
     * This is a helper method that works with raw CSV strings.
     *
     * @param csvData the CSV data as a string
     * @return markdown formatted table
     */
    private String convertCsvToMarkdown(String csvData) {
        if (csvData == null || csvData.trim().isEmpty()) {
            return "";
        }

        String[] lines = csvData.split("\n");
        if (lines.length == 0) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();

        // Process header row
        String[] headers = lines[0].split(",", -1);
        markdown.append("|");
        for (String header : headers) {
            markdown.append(" ").append(header.trim()).append(" |");
        }
        markdown.append("\n");

        // Add separator row
        markdown.append("|");
        for (int i = 0; i < headers.length; i++) {
            markdown.append(" --- |");
        }
        markdown.append("\n");

        // Process data rows
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",", -1);
            markdown.append("|");
            for (String value : values) {
                markdown.append(" ").append(value.trim()).append(" |");
            }
            markdown.append("\n");
        }

        return markdown.toString();
    }

    /**
     * Converts CSV data to a GitHub-flavored Markdown table.
     * <p>
     * Takes the original CSV data and transforms it into a clean, formatted
     * Markdown table suitable for documentation, READMEs, or other Markdown-compatible platforms.
     *
     * @param id the ID of the analysis to convert
     * @return the CSV data as a Markdown table string
     * @throws NotFoundException if no analysis exists with the given ID
     */
    public String convertToMarkdown(Long id) {
        DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Analysis not found with id: " + id));

        String csvData = entity.getOriginalData();
        if (csvData == null || csvData.trim().isEmpty()) {
            return "";
        }

        // Parse CSV into rows
        String[] lines = csvData.split("\n");
        if (lines.length == 0) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();

        // Process header row
        String[] headers = lines[0].split(",", -1);
        markdown.append("|");
        for (String header : headers) {
            markdown.append(" ").append(header.trim()).append(" |");
        }
        markdown.append("\n");

        // Add separator row
        markdown.append("|");
        for (int i = 0; i < headers.length; i++) {
            markdown.append(" --- |");
        }
        markdown.append("\n");

        // Process data rows
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",", -1);
            markdown.append("|");
            for (String value : values) {
                markdown.append(" ").append(value.trim()).append(" |");
            }
            markdown.append("\n");
        }

        return markdown.toString();
    }

    /**
     * Calculates the mean (average) of a list of numeric values.
     *
     * @param values the list of numeric values
     * @return the mean value
     */
    private Double calculateMean(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Calculates the median (middle value) of a list of numeric values.
     *
     * @param values the list of numeric values (will be sorted)
     * @return the median value
     */
    private Double calculateMedian(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }

        // Sort the values
        List<Double> sorted = new java.util.ArrayList<>(values);
        java.util.Collections.sort(sorted);

        int size = sorted.size();
        if (size % 2 == 0) {
            // Even number of elements - average of middle two
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            // Odd number of elements - middle element
            return sorted.get(size / 2);
        }
    }

    /**
     * Calculates the standard deviation of a list of numeric values.
     * Uses the sample standard deviation formula (n-1 denominator).
     *
     * @param values the list of numeric values
     * @param mean the mean of the values
     * @return the standard deviation
     */
    private Double calculateStandardDeviation(List<Double> values, Double mean) {
        if (values.isEmpty() || mean == null || values.size() == 1) {
            return null;
        }

        double sumSquaredDifferences = 0.0;
        for (Double value : values) {
            double difference = value - mean;
            sumSquaredDifferences += difference * difference;
        }

        // Using sample standard deviation (n-1)
        return Math.sqrt(sumSquaredDifferences / (values.size() - 1));
    }

}


