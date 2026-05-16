package com.englishwords.service;

import com.englishwords.common.BusinessException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WordImportParser {

    public List<ImportWordRow> parse(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (filename.endsWith(".csv")) {
                return parseCsv(file.getInputStream());
            }
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(file.getInputStream());
            }
            throw new BusinessException("仅支持 CSV、XLSX 或 XLS 文件");
        } catch (IOException ex) {
            throw new BusinessException("文件读取失败: " + ex.getMessage());
        }
    }

    private List<ImportWordRow> parseCsv(InputStream inputStream) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(reader)
        ) {
            Map<String, String> headers = csvHeaders(parser);
            if (!headers.containsKey("word") || !headers.containsKey("translation")) {
                throw new BusinessException("CSV 文件必须包含 word 和 translation 表头");
            }
            List<ImportWordRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new ImportWordRow(
                    (int) record.getRecordNumber() + 1,
                    get(record, headers, "word"),
                    get(record, headers, "translation"),
                    get(record, headers, "phonetic"),
                    get(record, headers, "example"),
                    get(record, headers, "tags")
                ));
            }
            return rows;
        }
    }

    private List<ImportWordRow> parseExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("Excel 文件缺少表头");
            }

            HeaderIndexes headers = HeaderIndexes.from(headerRow, formatter);
            List<ImportWordRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                ImportWordRow importRow = new ImportWordRow(
                    i + 1,
                    value(row, headers.wordIndex(), formatter),
                    value(row, headers.translationIndex(), formatter),
                    value(row, headers.phoneticIndex(), formatter),
                    value(row, headers.exampleIndex(), formatter),
                    value(row, headers.tagsIndex(), formatter)
                );
                if (!importRow.isBlank()) {
                    rows.add(importRow);
                }
            }
            return rows;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Excel 解析失败: " + ex.getMessage());
        }
    }

    private Map<String, String> csvHeaders(CSVParser parser) {
        Map<String, String> headers = new HashMap<>();
        for (String header : parser.getHeaderMap().keySet()) {
            String normalized = normalizeCsvHeader(header);
            if (normalized != null) {
                headers.putIfAbsent(normalized, header);
            }
        }
        return headers;
    }

    private String get(CSVRecord record, Map<String, String> headers, String name) {
        String actualName = headers.get(name);
        if (actualName == null || !record.isMapped(actualName)) {
            return null;
        }
        return trimToNull(record.get(actualName));
    }

    private static String value(Row row, int index, DataFormatter formatter) {
        if (index < 0) {
            return null;
        }
        Cell cell = row.getCell(index);
        return trimToNull(formatter.formatCellValue(cell));
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeCsvHeader(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1).trim();
        }
        return trimToNull(normalized);
    }

    private record HeaderIndexes(int wordIndex, int translationIndex, int phoneticIndex, int exampleIndex, int tagsIndex) {
        static HeaderIndexes from(Row row, DataFormatter formatter) {
            int wordIndex = -1;
            int translationIndex = -1;
            int phoneticIndex = -1;
            int exampleIndex = -1;
            int tagsIndex = -1;
            for (Cell cell : row) {
                String name = trimToNull(formatter.formatCellValue(cell));
                if (name == null) {
                    continue;
                }
                String normalized = name.toLowerCase();
                if ("word".equals(normalized)) {
                    wordIndex = cell.getColumnIndex();
                } else if ("translation".equals(normalized)) {
                    translationIndex = cell.getColumnIndex();
                } else if ("phonetic".equals(normalized)) {
                    phoneticIndex = cell.getColumnIndex();
                } else if ("example".equals(normalized)) {
                    exampleIndex = cell.getColumnIndex();
                } else if ("tags".equals(normalized)) {
                    tagsIndex = cell.getColumnIndex();
                }
            }
            if (wordIndex < 0 || translationIndex < 0) {
                throw new BusinessException("上传文件必须包含 word 和 translation 表头");
            }
            return new HeaderIndexes(wordIndex, translationIndex, phoneticIndex, exampleIndex, tagsIndex);
        }
    }

    public record ImportWordRow(
        int rowNumber,
        String word,
        String translation,
        String phonetic,
        String example,
        String tags
    ) {
        boolean isBlank() {
            return word == null && translation == null && phonetic == null && example == null && tags == null;
        }
    }
}
