package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCsvFileException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CsvUserParser {

    private CsvUserParser() {
    }

    public static List<ImportUserRow> parse(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        String header = reader.readLine();
        if (header == null) {
            throw new InvalidCsvFileException("CSV file is empty");
        }
        header = stripBom(header).trim().toLowerCase();
        String separator = header.contains(";") ? ";" : ",";
        String[] columns = header.split(separator);
        if (columns.length < 2 || !columns[0].trim().equals("name") || !columns[1].trim().equals("email")) {
            throw new InvalidCsvFileException("CSV header must be: name,email");
        }

        List<ImportUserRow> rows = new ArrayList<>();
        int lineNumber = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(separator, -1);
            String name = parts[0].trim();
            String email = parts.length > 1 ? parts[1].trim() : "";
            rows.add(new ImportUserRow(lineNumber, name, email));
        }

        if (rows.isEmpty()) {
            throw new InvalidCsvFileException("CSV file has no data rows");
        }
        return rows;
    }

    private static String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
}