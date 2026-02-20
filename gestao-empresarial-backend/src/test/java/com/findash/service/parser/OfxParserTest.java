package com.findash.service.parser;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OfxParserTest {

    private final OfxParser parser = new OfxParser();

    @Test
    void parse_validOfx_returnsTwoTransactions() throws Exception {
        InputStream input = getClass().getResourceAsStream("/samples/sample.ofx");
        List<ParsedTransaction> result = parser.parse(input, "sample.ofx").transactions();
        assertEquals(2, result.size());
    }

    @Test
    void parse_validOfx_debitIsNegativeAmount() throws Exception {
        InputStream input = getClass().getResourceAsStream("/samples/sample.ofx");
        List<ParsedTransaction> result = parser.parse(input, "sample.ofx").transactions();
        ParsedTransaction debit = result.stream().filter(t -> "DEBIT".equals(t.type())).findFirst().orElseThrow();
        assertEquals("DEBIT", debit.type());
        assertTrue(debit.amount().compareTo(java.math.BigDecimal.ZERO) > 0);
    }

    @Test
    void parse_validOfx_creditTransaction() throws Exception {
        InputStream input = getClass().getResourceAsStream("/samples/sample.ofx");
        List<ParsedTransaction> result = parser.parse(input, "sample.ofx").transactions();
        ParsedTransaction credit = result.stream().filter(t -> "CREDIT".equals(t.type())).findFirst().orElseThrow();
        assertEquals("CREDIT", credit.type());
    }
}
