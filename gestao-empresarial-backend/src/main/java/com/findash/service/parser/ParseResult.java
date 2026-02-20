package com.findash.service.parser;

import java.util.List;

public record ParseResult(
    List<ParsedTransaction> transactions,
    String bankName
) {}
