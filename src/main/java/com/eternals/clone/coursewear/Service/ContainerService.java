package com.eternals.clone.coursewear.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class ContainerService {


    @Value("${dle-cms-url}")
    private String API_URL;

    public void fetchAndProcessCourseware(String containerId, String publishId, String jwtToken, List<String> visitedContainers) throws IOException {
        if (visitedContainers.contains(containerId)) {
            return ; // Avoid infinite loops
        }
        visitedContainers.add(containerId);

        String jsonResponse = getContainerDetails(jwtToken,containerId,publishId);

        // Save the response to a file
        File file = new File("response_" + containerId + ".json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        objectMapper.writeValue(file, jsonNode);

        // Extract and process child containers
        JsonNode children = jsonNode.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                String childContainerId = child.get("id").asText();
                fetchAndProcessCourseware(childContainerId, publishId, jwtToken, visitedContainers);
            }
        }
    }


    public String getContainerDetails(String  jwtToken,String containerId,String publishId)
    {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        System.out.println(containerId);
        String url = API_URL+"/"+containerId+"/"+publishId;

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class,
                containerId, publishId
        );

        return response.getBody();

    }

    public String getContainerDetailsWithDepth(String jwtToken, String containerId, Integer depth,
                                      Boolean metadata, Boolean ordered, Boolean images) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Base URL
        String url = API_URL + "/" + containerId;

        // Build query parameters only if they are not null
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        if (depth != null) {
            uriBuilder.queryParam("depth", depth);
        }
        if (metadata != null) {
            uriBuilder.queryParam("metadata", metadata);
        }
        if (ordered != null) {
            uriBuilder.queryParam("ordered", ordered);
        }
        if (images != null) {
            uriBuilder.queryParam("images", images);
        }

        // Final URL with query parameters
        String finalUrl = uriBuilder.toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }





}
