package com.matillion.techtest2025.controller.response;

import com.matillion.techtest2025.model.ColumnStatistics;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO (Data Transfer Object) containing the results of a data analysis operation.
 * <p>
 * This is a Java record, introduced in Java 14 as a concise way to create immutable data
 * carrier classes. Records automatically generate:
 * <ul>
 *   <li>A constructor with all fields as parameters</li>
 *   <li>Getter methods for each field (without the "get" prefix)</li>
 *   <li>{@code equals()}, {@code hashCode()}, and {@code toString()} methods</li>
 * </ul>
 * <p>
 * This response is returned by the {@code POST /api/analysis/ingestCsv} endpoint and will be
 * automatically serialized to JSON by Spring. For example:
 * <pre>
 * {
 *   "id": 1,
 *   "name:" "First",
 *   "numberOfRows": 100,
 *   "numberOfColumns": 5,
 *   "totalCharacters": 2500,
 *   "csvTokenCount": 450,
 *   "markdownTokenCount": 520,
 *   "columnStatistics": [
 *     {"columnName": "age", "nullCount": 3},
 *     {"columnName": "name", "nullCount": 0}
 *   ]
 * }
 * </pre>
 *
 * @param id                  the unique identifier of the analysis
 * @param name                the user-provided name for this analysis
 * @param numberOfRows        the count of data rows in the CSV (excluding header)
 * @param numberOfColumns     the count of columns in the CSV
 * @param totalCharacters     the total character count in the CSV
 * @param csvTokenCount       the estimated token count for the CSV data (GPT-4 tokenization)
 * @param markdownTokenCount  the estimated token count for the markdown table format (GPT-4 tokenization)
 * @param columnStatistics    a list of statistics for each column
 * @see ColumnStatistics
 */
public record DataAnalysisResponse(
        Long id,
        String name,
        int numberOfRows,
        int numberOfColumns,
        long totalCharacters,
        int csvTokenCount,
        int markdownTokenCount,
        List<ColumnStatistics> columnStatistics,
        OffsetDateTime createdAt
) {
}
