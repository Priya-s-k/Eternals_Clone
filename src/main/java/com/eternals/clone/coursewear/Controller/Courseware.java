package com.eternals.clone.coursewear.Controller;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
 
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
 
    // Get courseware by containerId
    @GetMapping("/container/{containerId}/{publishId}")
    public String fetchCourseware(
            @RequestParam String containerId,
            @RequestParam String publishId,
            @RequestParam String jwtToken) {
 
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }
 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
 
        String url = API_URL + "/" + containerId + "/" + publishId;
 
        try {
ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String jsonResponse = response.getBody();
 
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
 
            JsonNode rootMetadataResponse = fetchContainerMetadata(containerId, publishId, jwtToken, true);
            JsonNode rootRawMetadata = rootMetadataResponse.get("rawMetadata");
            ((ObjectNode) rootNode).set("rawMetadata", rootRawMetadata);
 
            if (rootNode.has("children")) {
                for (JsonNode child : rootNode.get("children")) {
                    if (child.has("id")) {
                        String childId = child.get("id").asText();
                        JsonNode metadataResponse = fetchContainerMetadata(childId, publishId, jwtToken, true);
 
                        JsonNode rawMetadata = metadataResponse.has("rawMetadata") ? metadataResponse.get("rawMetadata") : null;
                        if (rawMetadata != null) {
                            ((ObjectNode) child).set("rawMetadata", rawMetadata);
                        }
                    }
                }
            }
 
            jsonResponse = objectMapper.writeValueAsString(rootNode);
            return jsonResponse;
 
        } catch (IOException e) {
            throw new RuntimeException("Error processing the response", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
 
    // Get metadata by containerId
    @GetMapping("/container/{containerId}/{publishId}/entire")
    public JsonNode fetchContainerMetadata(
            @RequestParam String containerId,
            @RequestParam String publishId,
            @RequestParam String jwtToken,
            @RequestParam boolean metadata) {
 
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }
 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
 
        String url = API_URL + "/" + containerId + "/" + publishId + "/entire?metadata=" + metadata;
 
        try {
ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.getBody());
 
        } catch (IOException e) {
            throw new RuntimeException("Error parsing the metadata response", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred while fetching metadata", e);
        }
    }
    @GetMapping("/container/{containerId}/{publishId}/download-images")
    public String downloadImages(
        @RequestParam String containerId,
        @RequestParam String publishId,
        @RequestParam String jwtToken) {
        
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }
 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String url = API_URL + "/" + containerId + "/" + publishId;
 
        try {
ResponseEntity <String>response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            
            List<String> imageUrls = new ArrayList<>();
            if (rootNode.has("children")) {
                for (JsonNode child : rootNode.get("children")) {
                    if (child.has("nodeImage") && child.get("nodeImage").isArray()) {
                        for (JsonNode image : child.get("nodeImage")) {
                            if (image.has("imageUrl")) {
                                imageUrls.add(image.get("imageUrl").asText());
                            }
                        }
                    }
                }
            }
 
            // Create ZIP file in project directory
String zipFileName = "downloaded_images.zip";
            File zipFile = new File(zipFileName);
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
                for (String imageUrl : imageUrls) {
                    try (InputStream in = new URL(imageUrl).openStream()) {
                        ZipEntry zipEntry = new ZipEntry(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
                        zipOut.putNextEntry(zipEntry);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, length);
                        }
                    }
                }
            }
 
            return "Images downloaded and saved as " + zipFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Error processing images or creating ZIP file", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

}