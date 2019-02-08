package cz.csas.tutorials.api;

import cz.csas.tutorials.api.model.ExchangeCodeForTokenException;
import cz.csas.tutorials.api.model.ExpiredTokenException;
import cz.csas.tutorials.api.model.GetCodeException;
import cz.csas.tutorials.api.model.RefreshAccessTokenException;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;

/**
 * Controller class, 3 methods for corporate API, 1 method for personal accounts API. All authorization calls are served
 * in authService.
 */
@RestController
@Slf4j
public class CsasApiController {

    @Value("${redirectUri}")
    private String redirectUri;
    @Value("${webApiKey}")
    private String webApiKey;
    @Value("${clientId}")
    private String clientId;
    @Value("${clientSecret}")
    private String clientSecret;
    @Value("${callbackUri}")
    private String callbackUri;
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
     * Calls PISP accounts endpoint /my/accounts, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param page  number for paging (paging and sorting works only in production, not sandbox environment)
     * @param size  of page
     * @param sort  for results sorting
     * @param order asc/desc
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @GetMapping("/pispAccounts")
    public String getAccounts(@RequestParam(defaultValue = "0") String page,
                              @RequestParam(defaultValue = "1") String size,
                              @RequestParam(required = false) String sort,
                              @RequestParam(required = false) String order) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String accounts = null;
        try {
            accounts = pispService.getAccounts(accessToken, webApiKey, page, size, sort, order);
            log.debug("Calling PISP accounts endpoint. Response = " + accounts);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                accounts = pispService.getAccounts(accessToken, webApiKey, page, size, sort, order);
                log.debug("Calling PISP accounts endpoint with new access token. Response = " + accounts);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return accounts;
    }

    /**
     * Calls PISP balance check endpoint /my/payments/balanceCheck, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param request in JSON form
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @PostMapping("/pispBalanceCheck")
    public String balanceCheck(@RequestBody BalanceCheckRequest request) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String balanceCheck = null;
        try {
            balanceCheck = pispService.balanceCheck(accessToken, webApiKey, request);
            log.debug("Calling PISP balance check endpoint. Response = " + balanceCheck);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                balanceCheck = pispService.balanceCheck(accessToken, webApiKey, request);
                log.debug("Calling PISP balance check endpoint with new access token. Response = " + balanceCheck);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return balanceCheck;
    }

    /**
     * Calls PISP create payment endpoint /my/payments, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param request in JSON form
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @PostMapping("/pispCreatePayment")
    public String createPayment(@RequestBody CreatePaymentRequest request) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String createdPayment = null;
        try {
            createdPayment = pispService.createPayment(accessToken, webApiKey, request);
            log.debug("Calling PISP create payment endpoint. Response = " + createdPayment);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                createdPayment = pispService.createPayment(accessToken, webApiKey, request);
                log.debug("Calling PISP create payment endpoint with new access token. Response = " + createdPayment);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return createdPayment;
    }

    /**
     * Calls PISP detail of the authorization endpoint /my/payments/sign/{signId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId of payment, received in createPayment response
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @GetMapping("/pispApiAuth/{signId}")
    public String getApiAuthorization(@PathVariable String signId) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String apiAuth = null;
        try {
            apiAuth = pispService.getApiAuthorization(accessToken, webApiKey, signId);
            log.debug("Calling PISP get API authorization endpoint. Response = " + apiAuth);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                apiAuth = pispService.getApiAuthorization(accessToken, webApiKey, signId);
                log.debug("Calling PISP get API authorization endpoint with new access token. Response = " + apiAuth);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return apiAuth;
    }

    /**
     * Calls PISP initiation of payment authorization endpoint /my/payments/sign/{signId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId of payment, received in createPayment response
     * @param request in JSON form with selected authorization type
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @PostMapping("/pispApiAuth/{signId}")
    public String startApiAuthorization(@PathVariable String signId,
                                        @RequestBody StartApiAuthorizationRequest request) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String apiAuth = null;
        try {
            apiAuth = pispService.startApiAuthorization(accessToken, webApiKey, signId, request);
            log.debug("Calling PISP start API authorization endpoint. Response = " + apiAuth);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                apiAuth = pispService.startApiAuthorization(accessToken, webApiKey, signId, request);
                log.debug("Calling PISP start API authorization endpoint with new access token. Response = " + apiAuth);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return apiAuth;
    }

    /**
     * Calls PISP payment authorization finalization endpoint /my/payments/sign/{signId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId of payment, received in createPayment response
     * @param request in JSON form with selected authorization type and oneTimePassword
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @PutMapping("/pispApiAuth/{signId}")
    public String finishApiAuthorization(@PathVariable String signId,
                                         @RequestBody FinishApiAuthorizationRequest request) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String apiAuth = null;
        try {
            apiAuth = pispService.finishApiAuthorization(accessToken, webApiKey, signId, request);
            log.debug("Calling PISP finish API authorization endpoint. Response = " + apiAuth);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                apiAuth = pispService.finishApiAuthorization(accessToken, webApiKey, signId, request);
                log.debug("Calling PISP finish API authorization endpoint with new access token. Response = " + apiAuth);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return apiAuth;
    }

    /**
     * Calls PISP obtain authorization URL endpoint /my/payments/federate/sign/{signId}/hash/{hash}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param signId of payment, received in createPayment response
     * @param hash of payment, received in createPayment response
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @GetMapping("/pispFederatedAuth/{signId}/hash/{hash}")
    public String getFederatedAuthorization(@PathVariable String signId,
                                            @PathVariable String hash) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String federatedAuth = null;
        try {
            federatedAuth = pispService.getFederatedAuthorization(accessToken, webApiKey, callbackUri, signId, hash);
            log.debug("Calling PISP get federated authorization endpoint. Response = " + federatedAuth);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                federatedAuth = pispService.getFederatedAuthorization(accessToken, webApiKey, callbackUri, signId, hash);
                log.debug("Calling PISP get federated authorization endpoint with new access token. Response = " + federatedAuth);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return federatedAuth;
    }

    /**
     * Calls PISP poll authorization state endpoint /my/payments/sign/poll/{pollId}, see docs https://developers.erstegroup.com/docs/apis/bank.csas/v1/payment-initiation
     *
     * @param pollId received in federatedAuth response
     * @return JSON response in String form (object is not returned on purpose)
     * @throws MalformedURLException if redirectUri is not correct URI
     */
    @GetMapping("/pispPollAuthorization/{pollId}")
    public String pollAuthorizationState(@PathVariable String pollId) throws MalformedURLException, RefreshAccessTokenException,
            GetCodeException, ExchangeCodeForTokenException {
        checkAccessToken();
        String pollAuthorizationState = null;
        try {
            pollAuthorizationState = pispService.pollAuthorizationState(accessToken, webApiKey, pollId);
            log.debug("Calling PISP poll authorization state endpoint. Response = " + pollAuthorizationState);
        } catch (ExpiredTokenException e) {
            accessToken = authService.refreshAccessToken(refreshToken, clientId, clientSecret);
            log.debug("Refreshing access token with refresh token = " + refreshToken); // Do not log token in production!
            log.debug("Obtained new access token = " + accessToken); // Do not log token in production!
            try {
                pollAuthorizationState = pispService.pollAuthorizationState(accessToken, webApiKey, pollId);
                log.debug("Calling PISP poll authorization state endpoint with new access token. Response = " + pollAuthorizationState);
            } catch (ExpiredTokenException e1) {
                log.error("Error when trying to refresh access token");
                throw new RefreshAccessTokenException("Error when trying to refresh access token");
            }
        }

        return pollAuthorizationState;
    }

    private void checkAccessToken() throws MalformedURLException, GetCodeException, ExchangeCodeForTokenException {
        if (accessToken == null) {
            TokenResponse tokenResponse = authService.getNewTokenResponse(redirectUri, clientId);
            accessToken = tokenResponse.getAccessToken();
            refreshToken = tokenResponse.getRefreshToken();
        }
    }
}
