package com.findash.service.parser;

import com.findash.exception.BusinessRuleException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CsvParser implements BankStatementParser {

    // Colunas esperadas pelo template padrao
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public ParseResult parse(InputStream input, String filename) throws Exception {
        byte[] bytes = input.readAllBytes();

        // Tenta UTF-8 primeiro, depois ISO-8859-1
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("ISO-8859-1"))) {
            try {
                List<ParsedTransaction> transactions = tryParse(bytes, charset);
                return new ParseResult(transactions, null);
            } catch (Exception ignored) {}
        }

        throw new BusinessRuleException(
            "Nao foi possivel reconhecer o formato do arquivo CSV. " +
            "Por favor, utilize o template padrao disponivel para download."
        );
    }

    private List<ParsedTransaction> tryParse(byte[] bytes, Charset charset) throws Exception {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(bytes), charset));

        // Auto-detect separador: tenta ; depois ,
        String firstLine = reader.readLine();
        if (firstLine == null) throw new IllegalArgumentException("Arquivo vazio");

        char separator = firstLine.contains(";") ? ';' : ',';

        // Reinicia o reader
        reader = new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(bytes), charset));

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(separator)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build();

        List<ParsedTransaction> result = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(reader, format)) {
            // Valida que tem as colunas esperadas
            var headers = parser.getHeaderNames().stream()
                .map(String::toLowerCase).toList();
            boolean hasRequiredColumns = headers.contains("data") &&
                headers.contains("descricao") && headers.contains("valor");
            if (!hasRequiredColumns) {
                throw new IllegalArgumentException("Colunas obrigatorias nao encontradas");
            }

            for (CSVRecord record : parser) {
                String rawDate = record.get("data").trim();
                String description = record.get("descricao").trim();
                String rawAmount = record.get("valor").trim()
                    .replace(",", ".");
                String tipo = headers.contains("tipo") ?
                    record.get("tipo").trim().toUpperCase() : "DEBIT";

                LocalDate date = LocalDate.parse(rawDate, DATE_FORMAT);
                BigDecimal amount = new BigDecimal(rawAmount).abs();
                String type = "CREDIT".equals(tipo) ? "CREDIT" : "DEBIT";

                result.add(new ParsedTransaction(
                    date, description, amount, type,
                    Map.of("raw", record.toMap())
                ));
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma transacao encontrada");
        }

        return result;
    }
}
