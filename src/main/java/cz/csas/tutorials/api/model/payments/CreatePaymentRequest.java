package cz.csas.tutorials.api.model.payments;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePaymentRequest {
    private PaymentTypeInformation paymentTypeInformation;
    private Amount amount;
    private LocalDate requestedExecutionDate;
    private Account debtorAccount;
    private Account creditorAccount;

}
