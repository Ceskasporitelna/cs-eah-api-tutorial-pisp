package cz.csas.tutorials.api.model.balance;

import lombok.Data;

@Data
public class BalanceCheckRequest {
    private Long exchangeIdentification;
    private DebtorAccount debtorAccount;
    private TransactionDetails transactionDetails;
}
