package com.matillion.techtest2025.service;

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

    /**
     * Analyzes CSV data and returns statistics.
     * <p>
     * Parses the CSV, calculates statistics (row count, column count, character count,
     * null counts per column), persists the results to the database, and returns the analysis.
     * <p>
     * <b>Note:</b> Current implementation is incomplete. Part 1 of the tech test
     * requires implementing the CSV parsing and analysis logic.
     *
     * @param data raw CSV data (rows separated by newlines, columns by commas)
     * @return analysis results
     */
    public DataAnalysisResponse analyzeCsvData(String data) {
        // Parse CSV data
        String[] lines = data.split("\n");

        // Extract header (column names)
        String[] headers = lines[0].split(",", -1);
        int numberOfColumns = headers.length;

        // Count data rows (excluding header)
        int numberOfRows = lines.length - 1;

        // Calculate total characters
        long totalCharacters = data.length();

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
                .originalData(data)
                .numberOfRows(numberOfRows)
                .numberOfColumns(numberOfColumns)
                .totalCharacters(totalCharacters)
                .createdAt(creationTimestamp)
                .build();


        // Create the per-column statistics
        List<ColumnStatisticsEntity> columnStatisticsEntities = new java.util.ArrayList<>();
        for (int col = 0; col < numberOfColumns; col++) {
                String columnName = headers[col];
                int nullCount = 0;
                java.util.Set<String> uniqueValues = new java.util.HashSet<>();

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

                ColumnStatisticsEntity columnStats = ColumnStatisticsEntity.builder()
                        .dataAnalysis(dataAnalysisEntity)
                        .columnName(columnName)
                        .nullCount(nullCount)
                        .uniqueCount(uniqueValues.size())
                        .inferredType(inferredType)
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
                numberOfRows,
                numberOfColumns,
                totalCharacters,
                columnStatisticsEntities
                        .stream()
                        .map(e -> new ColumnStatistics(
                                e.getColumnName(),
                                e.getNullCount(),
                                e.getUniqueCount(),
                                e.getInferredType()
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
                        entity.getNumberOfRows(),
                        entity.getNumberOfColumns(),
                        entity.getTotalCharacters(),
                        entity.getColumnStatistics()
                                .stream()
                                .map(e -> new ColumnStatistics(
                                        e.getColumnName(),
                                        e.getNullCount(),
                                        e.getUniqueCount(),
                                        e.getInferredType()
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


}


