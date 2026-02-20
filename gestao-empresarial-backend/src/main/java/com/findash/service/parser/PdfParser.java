package com.findash.service.parser;

import com.findash.exception.BusinessRuleException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);

    // --- Formato padrao: "dd/MM/yyyy  descricao  1.234,56 C" ---
    private static final DateTimeFormatter FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_SHORT = DateTimeFormatter.ofPattern("dd/MM/yy");

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^\\s*(\\d{2}/\\d{2}/(?:\\d{4}|\\d{2}))\\s+");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(-?)\\s*(?:R\\$\\s*)?(-?)(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*([CDcd])?\\s*$");

    // --- Formato Banco Inter: data por extenso como header, transacao na linha seguinte ---
    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("fevereiro", 2), Map.entry("março", 3),
            Map.entry("marco", 3), Map.entry("abril", 4), Map.entry("maio", 5),
            Map.entry("junho", 6), Map.entry("julho", 7), Map.entry("agosto", 8),
            Map.entry("setembro", 9), Map.entry("outubro", 10), Map.entry("novembro", 11),
            Map.entry("dezembro", 12)
    );

    // "6 de Fevereiro de 2026 Saldo do dia: R$ 73,24"
    private static final Pattern INTER_DATE_HEADER = Pattern.compile(
            "^(\\d{1,2})\\s+de\\s+(\\S+)\\s+de\\s+(\\d{4})\\s+Saldo do dia:");

    // "Pix recebido: \"desc\" R$ 73,24 R$ 73,24" ou "Pagamento efetuado: \"desc\" -R$ 73,24 R$ 0,00"
    private static final Pattern INTER_TX_PATTERN = Pattern.compile(
            "^(.+?)\\s+(-?)R\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s+R\\$\\s*\\d{1,3}(?:\\.\\d{3})*,\\d{2}\\s*$");

    // "Instituição: Banco Inter,"
    private static final Pattern INSTITUTION_PATTERN = Pattern.compile(
            "Institui[çc][aã]o:\\s*(.+?)\\s*,?\\s*$");

    @Override
    public ParseResult parse(InputStream input, String filename) throws Exception {
        String text;
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        }

        String[] lines = text.split("\\r?\\n");
        log.debug("PDF parser: {} linhas extraidas do arquivo '{}'", lines.length, filename);

        // Tenta formato padrao primeiro
        List<ParsedTransaction> transactions = parseStandardFormat(lines);

        // Se nao encontrou, tenta formato Banco Inter
        if (transactions.isEmpty()) {
            transactions = parseInterFormat(lines);
        }

        // Extrai nome do banco do header
        String bankName = extractBankName(lines);

        log.debug("PDF parser: {} transacoes encontradas, banco: {}", transactions.size(), bankName);

        if (transactions.isEmpty()) {
            throw new BusinessRuleException(
                    "Nao foi possivel extrair transacoes deste PDF. " +
                    "Tente exportar o extrato do seu banco em formato OFX ou CSV.");
        }

        return new ParseResult(transactions, bankName);
    }

    private List<ParsedTransaction> parseStandardFormat(String[] lines) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        for (String line : lines) {
            ParsedTransaction tx = tryParseStandardLine(line);
            if (tx != null) {
                transactions.add(tx);
            }
        }
        return transactions;
    }

    private List<ParsedTransaction> parseInterFormat(String[] lines) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        LocalDate currentDate = null;

        for (String line : lines) {
            if (line == null || line.isBlank()) continue;

            // Tenta extrair data do header
            LocalDate headerDate = tryParseInterDateHeader(line);
            if (headerDate != null) {
                currentDate = headerDate;
                continue;
            }

            // Se temos uma data ativa, tenta extrair transacao
            if (currentDate != null) {
                ParsedTransaction tx = tryParseInterTransaction(line, currentDate);
                if (tx != null) {
                    transactions.add(tx);
                }
            }
        }
        return transactions;
    }

    private LocalDate tryParseInterDateHeader(String line) {
        Matcher m = INTER_DATE_HEADER.matcher(line.trim());
        if (!m.find()) return null;

        int day = Integer.parseInt(m.group(1));
        String mesStr = m.group(2).toLowerCase();
        int year = Integer.parseInt(m.group(3));

        Integer month = MESES.get(mesStr);
        if (month == null) return null;

        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private ParsedTransaction tryParseInterTransaction(String line, LocalDate date) {
        Matcher m = INTER_TX_PATTERN.matcher(line.trim());
        if (!m.find()) return null;

        String description = m.group(1).trim();
        boolean isNegative = "-".equals(m.group(2));
        BigDecimal amount = parseAmount(m.group(3));

        // Ignora linhas de saldo
        if (description.toLowerCase().startsWith("saldo")) return null;

        String type = isNegative ? "DEBIT" : "CREDIT";

        return new ParsedTransaction(date, description, amount, type, Map.of("rawLine", line.trim()));
    }

    private String extractBankName(String[] lines) {
        for (String line : lines) {
            if (line == null) continue;
            Matcher m = INSTITUTION_PATTERN.matcher(line.trim());
            if (m.find()) {
                String name = m.group(1).trim();
                return name.isEmpty() ? null : name;
            }
        }
        return null;
    }

    // --- Formato padrao ---

    private ParsedTransaction tryParseStandardLine(String line) {
        if (line == null || line.isBlank()) return null;

        Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (!dateMatcher.find()) return null;

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
        if (!amountMatcher.find()) return null;

        LocalDate date = parseDate(dateMatcher.group(1));
        if (date == null) return null;

        String description = line.substring(dateMatcher.end(), amountMatcher.start()).trim();
        if (description.isEmpty()) return null;

        String negPrefix1 = amountMatcher.group(1);
        String negPrefix2 = amountMatcher.group(2);
        String amountStr = amountMatcher.group(3);
        String indicator = amountMatcher.group(4);

        BigDecimal amount = parseAmount(amountStr);
        boolean isNegative = "-".equals(negPrefix1) || "-".equals(negPrefix2);

        String type;
        if (indicator != null && indicator.equalsIgnoreCase("C")) {
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
