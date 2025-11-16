package com.ragchat.chat.client;

import com.ragchat.chat.model.dto.response.ApiResponse;
import com.ragchat.chat.model.dto.response.ErrorDetails;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserServiceClient {

    @Value("${user-service.url}")
    private String userServiceUrl; // http://user-service:8081/user

    private final RestTemplate restTemplate;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserValidationResponse validateToken(String token) {
        String url = userServiceUrl + "/api/auth/validate-token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<UserValidationResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<ApiResponse<UserValidationResponse>>() {});

        ApiResponse<UserValidationResponse> body = response.getBody();
        if (body == null || !body.success() || body.data() == null) {
            ErrorDetails error = body != null ? body.error() : null;
            String message = error != null ? error.message() : "Failed to validate token with user-service";
            throw new IllegalStateException(message);
        }

        return Objects.requireNonNull(body.data());
    }
}
