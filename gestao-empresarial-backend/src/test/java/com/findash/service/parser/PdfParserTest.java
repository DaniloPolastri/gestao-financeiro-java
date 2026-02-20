package com.findash.service.parser;

import com.findash.exception.BusinessRuleException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfParserTest {

    private final PdfParser parser = new PdfParser();

    private InputStream createSamplePdf(String... lines) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 10);
                content.setLeading(14f);
                content.newLineAtOffset(50, 700);

                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }

                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Test
    void parse_validPdfWithTransactions_returnsCorrectCount() throws Exception {
        InputStream pdf = createSamplePdf(
                "15/01/2026  PIX RECEBIDO - CLIENTE ABC      1.500,00 C",
                "16/01/2026  COMPRA DEBITO - LOJA XYZ           89,90",
                "18/01/2026  TED ENVIADA - FORNECEDOR 123    -3.000,00",
                "20/01/2026  DEPOSITO EM CONTA                 500,00 C"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "extrato.pdf").transactions();
        assertEquals(4, result.size());
    }

    @Test
    void parse_detectsCreditAndDebit_correctly() throws Exception {
        InputStream pdf = createSamplePdf(
                "15/01/2026  PIX RECEBIDO - CLIENTE ABC      1.500,00 C",
                "16/01/2026  COMPRA DEBITO - LOJA XYZ           89,90"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "extrato.pdf").transactions();
        assertEquals("CREDIT", result.get(0).type());
        assertEquals("DEBIT", result.get(1).type());
    }

    @Test
    void parse_negativeAmounts_detectedAsDebit() throws Exception {
        InputStream pdf = createSamplePdf(
                "18/01/2026  TED ENVIADA - FORNECEDOR 123    -3.000,00"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "extrato.pdf").transactions();
        assertEquals(1, result.size());
        assertEquals("DEBIT", result.get(0).type());
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.get(0).amount()));
    }

    @Test
    void parse_parsesAmountCorrectly_brazilianFormat() throws Exception {
        InputStream pdf = createSamplePdf(
                "15/01/2026  PIX RECEBIDO - CLIENTE ABC      1.234,56 C"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "extrato.pdf").transactions();
        assertEquals(0, new BigDecimal("1234.56").compareTo(result.get(0).amount()));
    }

    @Test
    void parse_shortDateFormat_ddMMyy() throws Exception {
        InputStream pdf = createSamplePdf(
                "15/01/26  TRANSFERENCIA    250,00 C"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "extrato.pdf").transactions();
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 1, 15), result.get(0).date());
    }

    @Test
    void parse_noTransactionsFound_throwsException() throws Exception {
        InputStream pdf = createSamplePdf(
                "Este documento nao contem transacoes",
                "Apenas texto informativo"
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> parser.parse(pdf, "extrato.pdf")
        );
        assertTrue(ex.getMessage().contains("Nao foi possivel extrair transacoes"));
    }

    @Test
    void parse_amountWithRsPrefix_parsedCorrectly() throws Exception {
        InputStream pdf = createSamplePdf(
                "15/01/2026  PAGAMENTO BOLETO    R$ 450,00"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "extrato.pdf").transactions();
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("450.00").compareTo(result.get(0).amount()));
    }

    // --- Banco Inter format tests ---

    @Test
    void parse_interFormat_parsesTransactionsCorrectly() throws Exception {
        InputStream pdf = createSamplePdf(
                "6 de fevereiro de 2026 Saldo do dia: R$ 1.000,00",
                "Pix recebido: CLIENTE ABC R$ 500,00 R$ 1.500,00",
                "Pagamento efetuado: BOLETO XYZ -R$ 200,00 R$ 1.300,00"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "inter.pdf").transactions();
        assertEquals(2, result.size());

        assertEquals(LocalDate.of(2026, 2, 6), result.get(0).date());
        assertEquals("CREDIT", result.get(0).type());
        assertEquals(0, new BigDecimal("500.00").compareTo(result.get(0).amount()));

        assertEquals(LocalDate.of(2026, 2, 6), result.get(1).date());
        assertEquals("DEBIT", result.get(1).type());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.get(1).amount()));
    }

    @Test
    void parse_interFormat_multipleDateHeaders() throws Exception {
        InputStream pdf = createSamplePdf(
                "6 de fevereiro de 2026 Saldo do dia: R$ 500,00",
                "Pix recebido: CLIENTE A R$ 100,00 R$ 600,00",
                "7 de fevereiro de 2026 Saldo do dia: R$ 600,00",
                "Pagamento efetuado: FORNECEDOR B -R$ 50,00 R$ 550,00"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "inter.pdf").transactions();
        assertEquals(2, result.size());

        assertEquals(LocalDate.of(2026, 2, 6), result.get(0).date());
        assertEquals(LocalDate.of(2026, 2, 7), result.get(1).date());
    }

    @Test
    void parse_interFormat_extractsBankName() throws Exception {
        InputStream pdf = createSamplePdf(
                "Instituicao: Banco Inter,",
                "6 de fevereiro de 2026 Saldo do dia: R$ 500,00",
                "Pix recebido: CLIENTE A R$ 100,00 R$ 600,00"
        );

        ParseResult result = parser.parse(pdf, "inter.pdf");
        assertEquals(1, result.transactions().size());
        assertEquals("Banco Inter", result.bankName());
    }

    @Test
    void parse_interFormat_ignoresSaldoLines() throws Exception {
        InputStream pdf = createSamplePdf(
                "6 de fevereiro de 2026 Saldo do dia: R$ 500,00",
                "Saldo anterior R$ 400,00 R$ 400,00",
                "Pix recebido: CLIENTE A R$ 100,00 R$ 500,00"
        );

        List<ParsedTransaction> result = parser.parse(pdf, "inter.pdf").transactions();
        assertEquals(1, result.size());
        assertEquals("CREDIT", result.get(0).type());
    }
}
