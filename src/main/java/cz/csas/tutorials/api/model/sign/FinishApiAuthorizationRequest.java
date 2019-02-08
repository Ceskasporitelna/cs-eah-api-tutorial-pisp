package cz.csas.tutorials.api.model.sign;

import lombok.Data;

@Data
public class FinishApiAuthorizationRequest extends StartApiAuthorizationRequest {
    private String oneTimePassword;
}
