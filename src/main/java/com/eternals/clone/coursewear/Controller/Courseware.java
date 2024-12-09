package com.eternals.clone.coursewear.Controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class Courseware {

    private static final String API_URL = "https://dle-cms-assemblyapi.mheducation.com/v3/container/{containerId}/{publishId}";

    // Get courseware by containerId
    @GetMapping("/container/{containerId}/{publishId}")
    public String fetchCourseware(@PathVariable String containerId, @PathVariable String publishId,
            @RequestParam String jwtToken) {
        // check if the token is present or not
        if (jwtToken == null || jwtToken.isEmpty()) {
            return "error JWT token is required";
        }

        // Create an instance of RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // Create HTTP headers and add the Authorization header with the Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);

        // Wrap the headers in an HttpEntity (which includes the Authorization header)
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Replace placeholders in the URL with the actual ID values
        String url = API_URL;

        // Call the API with the GET method and pass the path variables (id1, id2)
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class,
                containerId, publishId // Pass the path variables here
        );

        // Get the JSON response
        String jsonResponse = response.getBody();

        // Now, parse the JSON response
        try {
            // Create a mapper to parse the JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode rootMetadataResponse = fetchContainerMetadata(rootNode.get("id").asText(), publishId, jwtToken, true);
            JsonNode rootRawMetadata = rootMetadataResponse.get("rawMetadata");

            ((ObjectNode)rootNode).set("rawMetadata",rootRawMetadata);

              // Convert the updated rootNode back to a string (jsonResponse)
              jsonResponse = objectMapper.writeValueAsString(rootNode);
            // Extract the list of children containers (assuming they are under "children"
            // key)
            List<String> childContainerIds = new ArrayList<>();
            if (rootNode.has("children")) {
                for (JsonNode child : rootNode.get("children")) {
                    if (child.has("id")) {
                        childContainerIds.add(child.get("id").asText());
                        JsonNode metadataResponse = fetchContainerMetadata(child.get("id").asText(), publishId, jwtToken, true);

                        // Extract the raw metadata from the metadataResponse (assuming it's under "rawMetadata")
                       JsonNode rawMetadata = metadataResponse.has("rawMetadata") ? metadataResponse.get("rawMetadata") : null;
                        // If rawMetadata exists, append it to the child container's metadata
                    if (rawMetadata != null) {
                    ((ObjectNode) child).set("rawMetadata", rawMetadata);
                     }
                    }
                }
            }
                // Convert the updated rootNode back to a string (jsonResponse)
                jsonResponse = objectMapper.writeValueAsString(rootNode);

            // Return the responses from metadata fetch for each child container
            return jsonResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return "error! Error processing the response";
        }
    }

    // Get metadata by containerId
    @GetMapping("/container/{containerId}/{publishId}/entire")
    public JsonNode fetchContainerMetadata(@PathVariable String containerId, @PathVariable String publishId,
            @RequestParam String jwtToken, @RequestParam boolean metadata) {
                if (jwtToken == null || jwtToken.isEmpty()) {
                    throw new IllegalArgumentException("JWT token is required");
                }

        // Create an instance of RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // Create HTTP headers and add the Authorization header with the Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);

        // Wrap the headers in an HttpEntity (which includes the Authorization header)
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Replace placeholders in the URL with the actual ID values
        String url = "https://dle-cms-assemblyapi.mheducation.com/v3/container/{containerId}/{publishId}/entire?metadata="+metadata;

        // Call the API with the GET method and pass the path variables (containerId,
        // publishId)
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class,
                containerId, publishId // Pass the path variables here
        );

        // Get the JSON response

       // Get the JSON response and return it as a JsonNode (parsed object)
       try {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response.getBody());
    } catch (Exception e) {
        e.printStackTrace();
        return null;  // In case of an error, return null
    }
}
}
