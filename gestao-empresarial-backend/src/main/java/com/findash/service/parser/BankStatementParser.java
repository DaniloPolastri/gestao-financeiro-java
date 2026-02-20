package com.findash.service.parser;

import java.io.InputStream;

public interface BankStatementParser {
    ParseResult parse(InputStream input, String filename) throws Exception;
}
