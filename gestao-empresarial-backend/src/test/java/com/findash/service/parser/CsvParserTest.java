package com.findash.service.parser;

import com.findash.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void parse_validCsv_returnsTwoTransactions() throws Exception {
        InputStream input = getClass().getResourceAsStream("/samples/sample.csv");
        List<ParsedTransaction> result = parser.parse(input, "sample.csv").transactions();
        assertEquals(2, result.size());
    }

    @Test
    void parse_csvWithSemicolonSeparator_parsesCorrectly() throws Exception {
        String csv = "data;descricao;valor;tipo\n2026-01-15;Teste;500.00;DEBIT\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<ParsedTransaction> result = parser.parse(input, "test.csv").transactions();
        assertEquals(1, result.size());
        assertEquals("Teste", result.get(0).description());
    }

    @Test
    void parse_unrecognizedFormat_throwsBusinessRuleException() {
        String csv = "coluna1;coluna2\nvalor1;valor2\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        assertThrows(BusinessRuleException.class, () -> parser.parse(input, "test.csv"));
    }
}
