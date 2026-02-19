package com.findash.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfParser implements BankStatementParser {

    private static final DateTimeFormatter FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_SHORT = DateTimeFormatter.ofPattern("dd/MM/yy");

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^\\s*(\\d{2}/\\d{2}/(?:\\d{4}|\\d{2}))\\s+");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(-?)\\s*(?:R\\$\\s*)?(-?)(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*([CDcd])?\\s*$");

    @Override
    public List<ParsedTransaction> parse(InputStream input, String filename) throws Exception {
        String text;
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        }

        List<ParsedTransaction> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            ParsedTransaction tx = tryParseLine(line);
            if (tx != null) {
                transactions.add(tx);
            }
        }

        if (transactions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nao foi possivel extrair transacoes deste PDF. " +
                    "Tente exportar o extrato do seu banco em formato OFX ou CSV.");
        }

        return transactions;
    }

    private ParsedTransaction tryParseLine(String line) {
        Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (!dateMatcher.find()) {
            return null;
        }

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
        if (!amountMatcher.find()) {
            return null;
        }

        LocalDate date = parseDate(dateMatcher.group(1));
        if (date == null) {
            return null;
        }

        String description = line.substring(dateMatcher.end(), amountMatcher.start()).trim();
        if (description.isEmpty()) {
            return null;
        }

        String negPrefix1 = amountMatcher.group(1);
        String negPrefix2 = amountMatcher.group(2);
        String amountStr = amountMatcher.group(3);
        String indicator = amountMatcher.group(4);

        BigDecimal amount = parseAmount(amountStr);

        boolean isNegative = "-".equals(negPrefix1) || "-".equals(negPrefix2);

        String type;
        if (indicator != null && (indicator.equalsIgnoreCase("C"))) {
            type = "CREDIT";
        } else if (isNegative || (indicator != null && indicator.equalsIgnoreCase("D"))) {
            type = "DEBIT";
        } else {
            type = "DEBIT";
        }

        return new ParsedTransaction(date, description, amount, type, Map.of("rawLine", line.trim()));
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FMT_FULL);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr, FMT_SHORT);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private BigDecimal parseAmount(String amountStr) {
        String normalized = amountStr.replace(".", "").replace(",", ".");
        return new BigDecimal(normalized);
    }
}
