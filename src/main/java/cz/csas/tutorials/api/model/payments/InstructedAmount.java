package cz.csas.tutorials.api.model.payments;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InstructedAmount {
    private String currency;
    private BigDecimal value;
}
