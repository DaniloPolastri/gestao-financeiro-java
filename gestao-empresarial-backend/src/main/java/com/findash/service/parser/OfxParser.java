package com.findash.service.parser;

import com.webcohesion.ofx4j.domain.data.MessageSetType;
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope;
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponse;
import com.webcohesion.ofx4j.domain.data.banking.BankingResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.common.Transaction;
import com.webcohesion.ofx4j.domain.data.common.TransactionList;
import com.webcohesion.ofx4j.io.AggregateUnmarshaller;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OfxParser implements BankStatementParser {

    @Override
    public List<ParsedTransaction> parse(InputStream input, String filename) throws Exception {
        AggregateUnmarshaller<ResponseEnvelope> unmarshaller =
            new AggregateUnmarshaller<>(ResponseEnvelope.class);
        ResponseEnvelope envelope = unmarshaller.unmarshal(input);

        var bankMessages = envelope.getMessageSet(MessageSetType.banking);
        if (bankMessages == null) {
            throw new IllegalArgumentException("Arquivo OFX nao contem transacoes bancarias");
        }

        List<ParsedTransaction> result = new ArrayList<>();
        BankingResponseMessageSet bankingMessageSet = (BankingResponseMessageSet) bankMessages;

        for (var responseTransaction : bankingMessageSet.getStatementResponses()) {
            BankStatementResponse statement = responseTransaction.getMessage();
            TransactionList txList = statement.getTransactionList();
            if (txList == null) continue;

            for (Transaction tx : txList.getTransactions()) {
                Double rawAmount = tx.getAmount();
                if (rawAmount == null) continue;

                BigDecimal amount = BigDecimal.valueOf(Math.abs(rawAmount));
                String type = rawAmount >= 0 ? "CREDIT" : "DEBIT";
                java.time.LocalDate date = tx.getDatePosted()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                String description = tx.getMemo() != null ? tx.getMemo() : tx.getName();

                result.add(new ParsedTransaction(
                    date, description, amount, type,
                    Map.of("fitid", tx.getId() != null ? tx.getId() : "",
                           "memo", description != null ? description : "")
                ));
            }
        }
        return result;
    }
}
