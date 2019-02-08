package cz.csas.tutorials.api.model.sign;

import lombok.Data;

@Data
public class StartApiAuthorizationRequest {
    private String authorizationType;
}
