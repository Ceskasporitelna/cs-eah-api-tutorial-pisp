package cz.csas.tutorials.api;

import cz.csas.tutorials.api.model.ExchangeCodeForTokenException;
import cz.csas.tutorials.api.model.ExpiredAccessTokenException;
import cz.csas.tutorials.api.model.ExpiredRefreshTokenException;
import cz.csas.tutorials.api.model.StateNotFoundException;
import cz.csas.tutorials.api.model.TokenResponse;
import cz.csas.tutorials.api.model.balance.BalanceCheckRequest;
import cz.csas.tutorials.api.model.payments.CreatePaymentRequest;
import cz.csas.tutorials.api.model.sign.FinishApiAuthorizationRequest;
import cz.csas.tutorials.api.model.sign.StartApiAuthorizationRequest;
import cz.csas.tutorials.api.services.AuthService;
import cz.csas.tutorials.api.services.PispService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class calls all PISP endpoints. All authorization calls are served in authService.
 */
@RestController
@Slf4j
public class CsasApiController {

    @Value("${authorizationRedirectUri}")
    private String authorizationRedirectUri;
    @Value("${webApiKey}")
    private String webApiKey;
    @Value("${clientId}")
    private String clientId;
    @Value("${clientSecret}")
    private String clientSecret;
    @Value("${signedPaymentCallbackUri}")
    private String signedPaymentCallbackUri;
    private String accessToken = null;
    private String refreshToken = null;

    private final AuthService authService;
    private final PispService pispService;

    @Autowired
    public CsasApiController(AuthService authService, PispService pispService) {
        this.authService = authService;
        this.pispService = pispService;
    }

    /**
     * Builds url that is used for user authorization.
     *
     * @return url for user authorization
     */
    @GetMapping("/auth/authUrl")
    public ResponseEntity getAuthorizationUrl() {
        String authorizationUrl = authService.getAuthorizationUrl(authorizationRedirectUri, clientId);
        return ResponseEntity.ok(authorizationUrl);
    }

    /**
     * CSAS IDP redirect user to this endpoint after successful authorization. Application exchange received code for for access and refresh tokens.
     *
     * @param code  received from CSAS
     * @param state received from CSAS
     * @return message for user
     * @throws StateNotFoundException        if received state is not the one we sent to CSAS.
     * @throws ExchangeCodeForTokenException if anything bad happens during exchanging code.
     */
    @GetMapping("/auth/callback")
    public ResponseEntity obtainTokens(@RequestParam String code,
                                       @RequestParam String state) throws StateNotFoundException, ExchangeCodeForTokenException {
        TokenResponse tokens = authService.obtainTokens(code, state);
        accessToken = tokens.getAccessToken();
        refreshToken = tokens.getRefreshToken();
        return ResponseEntity.ok("Code has been changed for tokens. Application is now ready to serve PISP API calls.");
    }

    /**
     * Calls PISP accounts endpoint /my/accounts, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param page  number for paging (paging and sorting works only in production, not sandbox environment)
     * @param size  of page
     * @param sort  for results sorting
     * @param order asc/desc
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @GetMapping("/pisp/accounts")
    public ResponseEntity<Object> getAccounts(@RequestParam(defaultValue = "0") String page,
                                              @RequestParam(defaultValue = "1") String size,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) String order) throws ExpiredAccessTokenException {
        ResponseEntity<Object> accounts = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            accounts = pispService.getAccounts(accessToken, webApiKey, page, size, sort, order);
            log.debug("Called PISP accounts endpoint. Response = " + accounts);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            accounts = pispService.getAccounts(accessToken, webApiKey, page, size, sort, order);
            log.debug("Called PISP accounts endpoint with new access token. Response = " + accounts);
        }

        return accounts;
    }

    /**
     * Calls PISP balance check endpoint /my/payments/balanceCheck, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param request in JSON form
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @PostMapping("/pisp/balanceCheck")
    public ResponseEntity<Object> balanceCheck(@RequestBody BalanceCheckRequest request) throws ExpiredAccessTokenException {
        ResponseEntity<Object> balanceCheck = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            balanceCheck = pispService.balanceCheck(accessToken, webApiKey, request);
            log.debug("Called PISP balance check endpoint. Response = " + balanceCheck);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            balanceCheck = pispService.balanceCheck(accessToken, webApiKey, request);
            log.debug("Called PISP balance check endpoint with new access token. Response = " + balanceCheck);
        }

        return balanceCheck;
    }

    /**
     * Calls PISP create payment endpoint /my/payments, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param request in JSON form
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @PostMapping("/pisp/createPayment")
    public ResponseEntity<Object> createPayment(@RequestBody CreatePaymentRequest request) throws ExpiredAccessTokenException {
        ResponseEntity<Object> createdPayment = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            createdPayment = pispService.createPayment(accessToken, webApiKey, request);
            log.debug("Called PISP create payment endpoint. Response = " + createdPayment);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            createdPayment = pispService.createPayment(accessToken, webApiKey, request);
            log.debug("Called PISP create payment endpoint with new access token. Response = " + createdPayment);
        }

        return createdPayment;
    }

    /**
     * Calls PISP detail of the authorization endpoint /my/payments/sign/{signId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId of payment, received in createPayment response
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @GetMapping("/pisp/apiAuth/{signId}")
    public ResponseEntity<Object> getApiAuthorization(@PathVariable String signId) throws ExpiredAccessTokenException {
        ResponseEntity<Object> apiAuth = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            apiAuth = pispService.getApiAuthorization(accessToken, webApiKey, signId);
            log.debug("Called PISP get API authorization endpoint. Response = " + apiAuth);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            apiAuth = pispService.getApiAuthorization(accessToken, webApiKey, signId);
            log.debug("Called PISP get API authorization endpoint with new access token. Response = " + apiAuth);
        }

        return apiAuth;
    }

    /**
     * Calls PISP initiation of payment authorization endpoint /my/payments/sign/{signId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId  of payment, received in createPayment response
     * @param request in JSON form with selected authorization type
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @PostMapping("/pisp/apiAuth/{signId}")
    public ResponseEntity<Object> startApiAuthorization(@PathVariable String signId,
                                                        @RequestBody StartApiAuthorizationRequest request) throws ExpiredAccessTokenException {
        ResponseEntity<Object> apiAuth = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            apiAuth = pispService.startApiAuthorization(accessToken, webApiKey, signId, request);
            log.debug("Called PISP start API authorization endpoint. Response = " + apiAuth);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            apiAuth = pispService.startApiAuthorization(accessToken, webApiKey, signId, request);
            log.debug("Called PISP start API authorization endpoint with new access token. Response = " + apiAuth);
        }

        return apiAuth;
    }

    /**
     * Calls PISP payment authorization finalization endpoint /my/payments/sign/{signId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId  of payment, received in createPayment response
     * @param request in JSON form with selected authorization type and oneTimePassword
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @PutMapping("/pisp/apiAuth/{signId}")
    public ResponseEntity<Object> finishApiAuthorization(@PathVariable String signId,
                                                         @RequestBody FinishApiAuthorizationRequest request) throws ExpiredAccessTokenException {
        ResponseEntity<Object> apiAuth = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            apiAuth = pispService.finishApiAuthorization(accessToken, webApiKey, signId, request);
            log.debug("Called PISP finish API authorization endpoint. Response = " + apiAuth);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            apiAuth = pispService.finishApiAuthorization(accessToken, webApiKey, signId, request);
            log.debug("Called PISP finish API authorization endpoint with new access token. Response = " + apiAuth);
        }

        return apiAuth;
    }

    /**
     * Calls PISP obtain authorization URL endpoint /my/payments/federate/sign/{signId}/hash/{hash}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId of payment, received in createPayment response
     * @param hash   of payment, received in createPayment response
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @GetMapping("/pisp/federatedAuth/{signId}/hash/{hash}")
    public ResponseEntity<Object> getFederatedAuthorization(@PathVariable String signId,
                                                            @PathVariable String hash) throws ExpiredAccessTokenException {
        ResponseEntity<Object> federatedAuth = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            federatedAuth = pispService.getFederatedAuthorization(accessToken, webApiKey, signedPaymentCallbackUri, signId, hash);
            log.debug("Called PISP get federated authorization endpoint. Response = " + federatedAuth);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            federatedAuth = pispService.getFederatedAuthorization(accessToken, webApiKey, signedPaymentCallbackUri, signId, hash);
            log.debug("Called PISP get federated authorization endpoint with new access token. Response = " + federatedAuth);
        }

        return federatedAuth;
    }

    /**
     * Calls PISP poll authorization state endpoint /my/payments/sign/poll/{pollId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param pollId received in federatedAuth response
     * @return JSON response
     * @throws ExpiredAccessTokenException if new access token is rejected by CSAS IDP.
     */
    @GetMapping("/pisp/pollAuthorization/{pollId}")
    public ResponseEntity<Object> pollAuthorizationState(@PathVariable String pollId) throws ExpiredAccessTokenException {
        ResponseEntity<Object> pollAuthorizationState = null;
        if (StringUtils.isEmpty(accessToken)) {
            log.debug("Client has to be authorized.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Client has to be authorized.");
        }
        try {
            pollAuthorizationState = pispService.pollAuthorizationState(accessToken, webApiKey, pollId);
            log.debug("Called PISP poll authorization state endpoint. Response = " + pollAuthorizationState);
        } catch (ExpiredAccessTokenException e) {
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            try {
                accessToken = authService.getNewAccessToken(refreshToken, clientId, clientSecret);
            } catch (ExpiredRefreshTokenException e1) {
                log.debug("Refresh token has expired. Client has to be authorized.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has expired. Client has to be authorized.");
            }
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            pollAuthorizationState = pispService.pollAuthorizationState(accessToken, webApiKey, pollId);
            log.debug("Called PISP poll authorization endpoint with new access token. Response = " + pollAuthorizationState);
        }

        return pollAuthorizationState;
    }
}
