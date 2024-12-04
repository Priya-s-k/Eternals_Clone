package com.eternals.clone.coursewear.Controller;
 
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
 
@RestController
public class Courseware {
 
private static final String API_URL = "https://dle-cms-assemblyapi.mheducation.com/v3/container";
 
    @GetMapping("/container")
    public String fetchCourseware(
            @RequestParam String containerId,
            @RequestParam String publishId,
            @RequestParam String jwtToken) {
 
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("Container ID is required");
        }
        if (publishId == null || publishId.isEmpty()) {
            throw new IllegalArgumentException("Publish ID is required");
        }
 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = API_URL + "?containerId=" + containerId + "&publishId=" + publishId;
 
ResponseEntity <String>response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String jsonResponse = response.getBody();
 
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
 
            JsonNode rootMetadataResponse = fetchContainerMetadata(containerId, publishId, jwtToken, true);
            JsonNode rootRawMetadata = rootMetadataResponse.get("rawMetadata");
            ((ObjectNode) rootNode).set("rawMetadata", rootRawMetadata);
 
            List<String> childContainerIds = new ArrayList<>();
            if (rootNode.has("children")) {
                for (JsonNode child : rootNode.get("children")) {
                    if (child.has("id")) {
                        childContainerIds.add(child.get("id").asText());
                        JsonNode metadataResponse = fetchContainerMetadata(child.get("id").asText(), publishId, jwtToken, true);
                        JsonNode rawMetadata = metadataResponse.has("rawMetadata") ? metadataResponse.get("rawMetadata") : null;
                        if (rawMetadata != null) {
                            ((ObjectNode) child).set("rawMetadata", rawMetadata);
                        }
                    }
                }
            }
            jsonResponse = objectMapper.writeValueAsString(rootNode);
            return jsonResponse;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing the response", e);
        }
    }
 
    @GetMapping("/container/metadata")
    public JsonNode fetchContainerMetadata(
            @RequestParam String containerId,
            @RequestParam String publishId,
            @RequestParam String jwtToken,
            @RequestParam boolean metadata) {
 
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("Container ID is required");
        }
        if (publishId == null || publishId.isEmpty()) {
            throw new IllegalArgumentException("Publish ID is required");
        }
 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = API_URL + "/" + containerId + "/" + publishId + "/entire?metadata=" + metadata;
 
ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
 
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing metadata response", e);
        }
    }
}