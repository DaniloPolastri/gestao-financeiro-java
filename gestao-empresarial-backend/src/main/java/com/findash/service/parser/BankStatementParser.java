package com.findash.service.parser;

import java.io.InputStream;
import java.util.List;

public interface BankStatementParser {
    List<ParsedTransaction> parse(InputStream input, String filename) throws Exception;
}
