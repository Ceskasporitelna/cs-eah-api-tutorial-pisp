package cz.csas.tutorials.api.model.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDetails {
    private String currency;
    private BigDecimal totalAmount;
}
