package cz.csas.tutorials.api.services;

import cz.csas.tutorials.api.model.ExpiredTokenException;
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
     * @return accounts - JSON response in String form
     * @throws ExpiredTokenException if access token is expired
     */
    public String getAccounts(String token, String webApiKey, String page, String size, String sort, String order) throws ExpiredTokenException {
        Map<String, String> uriParams = new HashMap<>();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getProperty("pispAccountsUrl"))
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort)
                .queryParam("order", order);
        String pispAccountsUrl = builder.buildAndExpand(uriParams).toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        ResponseEntity<String> accounts = restTemplate.exchange(pispAccountsUrl, HttpMethod.GET, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(accounts.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return accounts.getBody();
    }

    public String balanceCheck(String token, String webApiKey, BalanceCheckRequest request) throws ExpiredTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getProperty("pispAccBalanceCheckUrl"));
        String pispAccBalanceCheckUrl = builder.build().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<BalanceCheckRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> balanceCheckResponse = restTemplate.exchange(pispAccBalanceCheckUrl, HttpMethod.POST, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(balanceCheckResponse.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return balanceCheckResponse.getBody();
    }

    public String createPayment(String token, String webApiKey, CreatePaymentRequest request) throws ExpiredTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getProperty("pispCreatePaymentUrl"));
        String pispCreatePaymentUrl = builder.build().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> createPaymentResponse = restTemplate.exchange(pispCreatePaymentUrl, HttpMethod.POST, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(createPaymentResponse.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return createPaymentResponse.getBody();
    }

    public String getApiAuthorization(String token, String webApiKey, String signId) throws ExpiredTokenException {
        String pispApiAuthUrl = createApiAuthorizationUrl(signId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity(headers);
        ResponseEntity<String> apiAuthorization = restTemplate.exchange(pispApiAuthUrl, HttpMethod.GET, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(apiAuthorization.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return apiAuthorization.getBody();
    }

    public String startApiAuthorization(String token, String webApiKey, String signId, StartApiAuthorizationRequest request) throws ExpiredTokenException {
        String pispApiAuthUrl = createApiAuthorizationUrl(signId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity(request, headers);
        ResponseEntity<String> apiAuthorization = restTemplate.exchange(pispApiAuthUrl, HttpMethod.POST, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(apiAuthorization.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return apiAuthorization.getBody();
    }

    public String finishApiAuthorization(String token, String webApiKey, String signId, FinishApiAuthorizationRequest request) throws ExpiredTokenException {
        String pispApiAuthUrl = createApiAuthorizationUrl(signId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity(request, headers);
        ResponseEntity<String> apiAuthorization = restTemplate.exchange(pispApiAuthUrl, HttpMethod.PUT, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(apiAuthorization.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return apiAuthorization.getBody();
    }

    public String getFederatedAuthorization(String token, String webApiKey, String callbackUri, String signId, String hash) throws ExpiredTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getProperty("pispFederatedAuthUrl"));
        String pispFederatedAuthUrl = builder.buildAndExpand(signId, hash).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        headers.add("Callback-Uri", callbackUri);
        HttpEntity<Object> entity = new HttpEntity(headers);
        ResponseEntity<String> federatedAuthorization = restTemplate.exchange(pispFederatedAuthUrl, HttpMethod.GET, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(federatedAuthorization.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return federatedAuthorization.getBody();
    }

    public String pollAuthorizationState(String token, String webApiKey, String pollId) throws ExpiredTokenException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getProperty("pispPollAuthUrl"));
        String pispPollAuthUrl = builder.buildAndExpand(pollId).toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("web-api-key", webApiKey);
        HttpEntity<Object> entity = new HttpEntity(headers);
        ResponseEntity<String> pollAuthorizationState = restTemplate.exchange(pispPollAuthUrl, HttpMethod.GET, entity, String.class);
        if (HttpStatus.UNAUTHORIZED.equals(pollAuthorizationState.getStatusCode())) {
            throw new ExpiredTokenException("Token has expired.");
        }
        return pollAuthorizationState.getBody();
    }

    private String createApiAuthorizationUrl(String signId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(environment.getProperty("pispApiAuthUrl"));
        return builder.buildAndExpand(signId).toString();
    }
}
