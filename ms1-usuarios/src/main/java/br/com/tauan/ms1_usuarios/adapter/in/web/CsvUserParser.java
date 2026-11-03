package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCsvFileException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CsvUserParser {

    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    private CsvUserParser() {
    }

    public static List<ImportUserRow> parse(InputStream input) throws IOException {
        String content = stripBom(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        if (content.isBlank()) {
            throw new InvalidCsvFileException("CSV file is empty");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(content))
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (CSVParser parser = format.parse(new StringReader(content))) {
            Set<String> headers = parser.getHeaderNames().stream()
                    .map(header -> header.trim().toLowerCase())
                    .collect(Collectors.toSet());
            if (!headers.containsAll(List.of(NAME, EMAIL, PASSWORD))) {
                throw new InvalidCsvFileException("CSV header must be: name,email,password");
            }

            List<ImportUserRow> rows = new ArrayList<>();
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                rows.add(new ImportUserRow(rowNumber,
                        value(record, NAME), value(record, EMAIL), value(record, PASSWORD)));
            }

            if (rows.isEmpty()) {
                throw new InvalidCsvFileException("CSV file has no data rows");
            }
            return rows;
        } catch (UncheckedIOException | IllegalArgumentException e) {
            throw new InvalidCsvFileException("Malformed CSV file: " + e.getMessage());
        }
    }

    private static String value(CSVRecord record, String column) {
        return record.isSet(column) ? record.get(column) : "";
    }

    private static char detectDelimiter(String content) {
        int end = content.indexOf('\n');
        String firstLine = end >= 0 ? content.substring(0, end) : content;
        return firstLine.contains(";") ? ';' : ',';
    }

    private static String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
}