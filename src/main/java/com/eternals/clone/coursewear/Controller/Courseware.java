package com.eternals.clone.coursewear.Controller;
 
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
 
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
 
@RestController
public class Courseware {
 
private static final String API_URL = "https://dle-cms-assemblyapi.mheducation.com/v3/container/{containerId}/{publishId}";
 
    @GetMapping("/container/{containerId}/{publishId}/recursive")
    public String fetchCoursewareRecursively(@PathVariable String containerId, @PathVariable String publishId, @RequestParam String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return "JWT token is required";
        }
 
        try {
            fetchAndProcessCourseware(containerId, publishId, jwtToken, new ArrayList<>());
            return "Recursive fetch completed successfully.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error during recursive fetch.";
        }
    }
 
    private void fetchAndProcessCourseware(String containerId, String publishId, String jwtToken, List<String> visitedContainers) throws IOException {
        if (visitedContainers.contains(containerId)) {
            return; // Avoid infinite loops
        }
        visitedContainers.add(containerId);
 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
 
        String url = API_URL;
ResponseEntity <String>response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class,
            containerId, publishId
        );
 
        String jsonResponse = response.getBody();
 
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
}