package com.example.caffeine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
public class CaffeineBrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaffeineBrokerApplication.class, args);
    }

    @Autowired
    CaffeineServiceProperties props;

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    ServiceInstanceService serviceInstanceService(RestTemplate restTemplate) {
        return new ServiceInstanceService() {
            @Override
            public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
                Caffeine caffeine = Caffeine.valueOf(request.getPlanId().toUpperCase());
                try {
                    restTemplate.exchange(RequestEntity.post(UriComponentsBuilder.fromUri(props.uri)
                            .pathSegment("caffeine")
                            .queryParam("service_id", request.getServiceInstanceId())
                            .queryParam("expire_second", caffeine.expireSeconds)
                            .queryParam("maximum_size", caffeine.maximumSize)
                            .build().toUri())
                            .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                    .encodeToString((props.username + ":" + props.password).getBytes()))
                            .build(), String.class);
                } catch (RestClientException e) {
                    log.error("cannot create service instance", e);
                    throw new ServiceInstanceExistsException(request.getServiceInstanceId(), request.getServiceDefinitionId());
                }
                return new CreateServiceInstanceResponse();
            }

            @Override
            public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
                return new GetLastServiceOperationResponse().withOperationState(OperationState.SUCCEEDED);
            }

            @Override
            public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
                try {
                    restTemplate.exchange(RequestEntity.delete(UriComponentsBuilder.fromUri(props.uri)
                            .pathSegment("caffeine", request.getServiceInstanceId())
                            .build().toUri())
                            .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                    .encodeToString((props.username + ":" + props.password).getBytes()))
                            .build(), Void.class);
                } catch (RestClientException e) {
                    log.error("cannot delete service instance", e);
                    throw e;
                }
                return new DeleteServiceInstanceResponse();
            }

            @Override
            public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
                return new UpdateServiceInstanceResponse();
            }
        };
    }

    @Bean
    ServiceInstanceBindingService serviceInstanceBindingService(RestTemplate restTemplate) {
        return new ServiceInstanceBindingService() {
            @Override
            public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
                try {
                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            RequestEntity.post(UriComponentsBuilder.fromUri(props.uri)
                                    .pathSegment("credentials")
                                    .queryParam("service_id", request.getServiceInstanceId())
                                    .queryParam("username", request.getBindingId())
                                    .build().toUri())
                                    .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                            .encodeToString((props.username + ":" + props.password).getBytes()))
                                    .build(),
                            new ParameterizedTypeReference<Map<String, Object>>() {
                            });
                    Map<String, Object> credentials = new HashMap<>(response.getBody());
                    credentials.put("uri", props.uri.toString() + "/caffeine/" + request.getServiceInstanceId());
                    return new CreateServiceInstanceAppBindingResponse().withCredentials(credentials);
                } catch (RestClientException e) {
                    log.error("cannot create service instance binding", e);
                    throw e;
                }
            }

            @Override
            public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
                try {
                    restTemplate.exchange(RequestEntity.delete(UriComponentsBuilder.fromUri(props.uri)
                            .pathSegment("credentials", request.getServiceInstanceId(), request.getBindingId())
                            .build().toUri())
                            .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                    .encodeToString((props.username + ":" + props.password).getBytes()))
                            .build(), Void.class);
                } catch (RestClientException e) {
                    log.error("cannot delete service instance binding", e);
                    throw e;
                }
            }
        };
    }

    @Bean
    Catalog catalog() {
        return new Catalog(Collections.singletonList(new ServiceDefinition(
                "caffeine-broker",
                "p-caffeine",
                "A caffeine service broker",
                true,
                false,
                Stream.of(Caffeine.values()).map(c -> new Plan(c.name().toLowerCase(),
                        c.name().toLowerCase(),
                        String.format("%s caffeine plan (%d elements, expires %d seconds after last access)", c.name().toLowerCase(), c.maximumSize, c.expireSeconds),
                        new HashMap<String, Object>() {
                            {
                                put("costs", Collections.singletonList(new HashMap<String, Object>() {
                                    {
                                        put("amount", Collections.singletonMap("usd", 0.0));
                                        put("unit", "MONTHLY");
                                    }
                                }));
                                put("bullets", Arrays.asList(c.name().toLowerCase() + " caffeine", c.maximumSize + " elements", "expires " + c.expireSeconds + " seconds after last access"));
                            }
                        }, true)).collect(Collectors.toList()),
                Collections.singletonList("caffeine"),
                new HashMap<String, Object>() {{
                    put("displayName", "caffeine");
                    put("longDescription", "Caffeine as a Service");
                    put("imageUrl", "https://raw.githubusercontent.com/ben-manes/caffeine/master/wiki/logo.png");
                    put("providerDisplayName", "@making");
                }},
                null,
                null
        )));
    }

    @AllArgsConstructor
    enum Caffeine {
        STRONG(600, 1000), WEAK(60, 100);
        private final int expireSeconds;
        private final int maximumSize;
    }

    @Component
    @ConfigurationProperties("caffeine.service")
    @Data
    public static class CaffeineServiceProperties {
        private URI uri;
        private String username;
        private String password;
    }
}
