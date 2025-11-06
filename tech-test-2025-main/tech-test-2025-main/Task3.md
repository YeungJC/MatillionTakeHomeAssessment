# Additional Features

---

## 1. ID Returned in POST Method

### What problem does it solve?
Previously, when uploading a CSV for analysis, there was no reliable way to identify or reference a specific analysis once it had been created. Users had to manually keep track of their uploads without a unique identifier.

### How does it work?
The `POST /api/analysis/ingestCsv` endpoint now generates and returns a unique **ID** for each new analysis. This ID is stored in the database along with the analysis results.

### How would someone use it?
The process remains unchanged — users still upload their CSV and call the same endpoint. The difference is that the response now includes a unique ID that can be used to reference the analysis later.

---

## 2. Optional `name` Parameter Added to POST Method

### What problem does it solve?
Previously, there was no way to label or distinguish between multiple uploaded analyses. Users had to rely solely on automatically generated IDs, which made it difficult to identify or organise analyses when several CSVs were processed.

### How does it work?
The `POST /api/analysis/ingestCsv` endpoint now accepts an optional **name** parameter alongside the CSV data. This name is saved in the database and returned in the response. It can later be used to retrieve or delete the analysis by name rather than by ID.

### How would someone use it?
When uploading a CSV, users can include a custom `name` field to identify the dataset. This allows for more descriptive and human-readable management of analyses through the API.

---

## 3. New GET Endpoint for Listing Analyses

### What problem does it solve?
Previously, viewing multiple analyses was tedious and time-consuming. Users had to retrieve each analysis individually by ID or name, making it difficult to get an overview of everything stored in the system.

### How does it work?
A new endpoint, `GET /api/analysis`, lists all stored analyses in a single response allowing users to easily browse and identify available analyses.

### How would someone use it?
Users can query this endpoint to quickly see all analyses in the system, making it easier to decide which ones to view in detail or delete.


---

## 4. Token Counting Utility

### What problem does it solve?
When working with large CSV files in a language model (LLM) context, users often need to estimate the number of tokens the file will consume. This helps with cost estimation and ensures that token limits are not exceeded during processing.

### How does it work?
A new utility integrates a token-counting library to calculate how many LLM tokens a CSV file and its associated markdown file contains. This enables users to plan their model usage and manage context size constraints more effectively.

### How would someone use it?
Once implemented, users will be able to upload a CSV and receive the token count either as part of the analysis results or through a dedicated endpoint.

---

## 5. Markdown Conversion Utility

### What problem does it solve?
Raw CSV data is often difficult to read or include in documentation. Users frequently need a way to present CSV data in a clean, formatted, and readable format.

### How does it work?
A new converter will transform CSV data into GitHub-flavoured Markdown tables. This allows data to be displayed neatly in documentation, READMEs, or other Markdown-compatible platforms.

### How would someone use it?
Once implemented, users will be able to convert a CSV file into a Markdown table via an API endpoint or internal utility, making it simple to share or document analysis results.

---

## 6. Statistical Analysis (Mean, Median, Standard Deviation) added to POST method

### What problem does it solve?
Previously, when uploading a CSV for analysis, users received no immediate statistical insights about their data. They had to manually calculate key metrics such as the mean, median, or standard deviation using external tools.

### How does it work?
The `POST /api/analysis/ingestCsv` endpoint now automatically computes basic statistical measures — **mean**, **median**, and **standard deviation** — for all numerical columns within the uploaded CSV. These values are stored in the database along with the analysis results and included in the API response.

### How would someone use it?
The usage remains the same. Users upload their CSV via the existing endpoint, and the response now includes summary statistics for each numerical column, providing an immediate overview of the dataset without any additional steps.
