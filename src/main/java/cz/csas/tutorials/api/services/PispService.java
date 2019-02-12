package cz.csas.tutorials.api.services;

import cz.csas.tutorials.api.model.ExpiredAccessTokenException;
import cz.csas.tutorials.api.model.balance.BalanceCheckRequest;
import cz.csas.tutorials.api.model.payments.CreatePaymentRequest;
import cz.csas.tutorials.api.model.sign.FinishApiAuthorizationRequest;
import cz.csas.tutorials.api.model.sign.StartApiAuthorizationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class PispService {
    private final RestTemplate restTemplate;
    private final Environment environment;

    @Autowired
    public PispService(RestTemplate restTemplate, Environment environment) {
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    /**
     * Calls PISP accounts API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param page      number for paging (paging and sorting works only in production, not sandbox environment)
     * @param size      of page
     * @param sort      for results sorting
     * @param order     asc/desc
     * @return ResponseEntity with accounts in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> getAccounts(String token, String webApiKey, String page, String size, String sort, String order) throws ExpiredAccessTokenException {
        Map<String, String> uriParams = new HashMap<>();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispAccountsUrl"))
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort)
                .queryParam("order", order);
        String pispAccountsUrl = builder.buildAndExpand(uriParams).toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(pispAccountsUrl, HttpMethod.GET, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP balance check API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param request   containing mandatory fields for balance check
     * @return ResponseEntity with balance check in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> balanceCheck(String token, String webApiKey, BalanceCheckRequest request) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispAccBalanceCheckUrl"));
        String pispAccBalanceCheckUrl = builder.build().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<BalanceCheckRequest> entity = new HttpEntity<>(request, headers);
        try {
            return restTemplate.exchange(pispAccBalanceCheckUrl, HttpMethod.POST, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP create payment API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param request   containing mandatory fields for create payment
     * @return ResponseEntity with create payment in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> createPayment(String token, String webApiKey, CreatePaymentRequest request) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispCreatePaymentUrl"));
        String pispCreatePaymentUrl = builder.build().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(request, headers);
        try {
            return restTemplate.exchange(pispCreatePaymentUrl, HttpMethod.POST, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP detail of the authorization API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param signId    of created payment
     * @return ResponseEntity with detail of the authorization in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> getApiAuthorization(String token, String webApiKey, String signId) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispApiAuthUrl"));
        String pispApiAuthUrl = builder.buildAndExpand(signId).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(pispApiAuthUrl, HttpMethod.GET, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP initiation of payment authorization API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param signId    of created payment
     * @param request   containing mandatory fields for initiation of payment authorization
     * @return ResponseEntity with initiation of payment authorization in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> startApiAuthorization(String token, String webApiKey, String signId, StartApiAuthorizationRequest request) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispApiAuthUrl"));
        String pispApiAuthUrl = builder.buildAndExpand(signId).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        try {
            return restTemplate.exchange(pispApiAuthUrl, HttpMethod.POST, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP payment authorization finalization API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param signId    of created payment
     * @param request   containing mandatory fields for payment authorization finalization
     * @return ResponseEntity with payment authorization finalization in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> finishApiAuthorization(String token, String webApiKey, String signId, FinishApiAuthorizationRequest request) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispApiAuthUrl"));
        String pispApiAuthUrl = builder.buildAndExpand(signId).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        try {
            return restTemplate.exchange(pispApiAuthUrl, HttpMethod.PUT, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP obtain authorization url for federated authorization API
     *
     * @param token       access token
     * @param webApiKey   webapi key to connect to webapi
     * @param callbackUri URL for redirection by CSAS after successful payment authorization
     * @param signId      of created payment
     * @param hash        of created payment
     * @return ResponseEntity with obtain authorization url for federated authorization in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> getFederatedAuthorization(String token, String webApiKey, String callbackUri, String signId, String hash) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispFederatedAuthUrl"));
        String pispFederatedAuthUrl = builder.buildAndExpand(signId, hash).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        headers.add("Callback-Uri", callbackUri);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(pispFederatedAuthUrl, HttpMethod.GET, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }

    /**
     * Calls PISP poll authorization state API
     *
     * @param token     access token
     * @param webApiKey webapi key to connect to webapi
     * @param pollId    of authorization
     * @return ResponseEntity with poll authorization state in body
     * @throws ExpiredAccessTokenException if access token is expired
     */
    public ResponseEntity<Object> pollAuthorizationState(String token, String webApiKey, String pollId) throws ExpiredAccessTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getRequiredProperty("pispPollAuthUrl"));
        String pispPollAuthUrl = builder.buildAndExpand(pollId).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(pispPollAuthUrl, HttpMethod.GET, entity, Object.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
                throw new ExpiredAccessTokenException("Token has expired.");
            } else {
                throw ex;
            }
        }
    }
}
