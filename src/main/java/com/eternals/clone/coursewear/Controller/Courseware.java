package com.eternals.clone.coursewear.Controller;

import java.io.File;
import java.io.IOException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class Courseware {

    private static final String API_URL = "https://dle-cms-assemblyapi.mheducation.com/v3/container/{containerId}/{publishId}"; 

     // Get courseware by containerId
    @GetMapping("/container/{containerId}/{publishId}")
    public String fetchCourseware(@PathVariable String containerId, @PathVariable String publishId, @RequestParam String jwtToken) {
        // check if the token is present or not
        if (jwtToken == null || jwtToken.isEmpty()) {
            return "JWT token is required";
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
                containerId, publishId  // Pass the path variables here
        );

        // Get the JSON response
        String jsonResponse = response.getBody();

        // Now, write the response to a file
        try {
            // Create the file object (or overwrite the existing one)
            File file = new File("response.json");

            // Create a mapper to write the JSON response to the file
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(file, objectMapper.readTree(jsonResponse));

            return "Response saved successfully in response.json";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error saving response to file";
        }
    }

     // Get metadata by containerId
     @GetMapping("/container/{containerId}/{publishId}/entire")
     public String fetchContainereMetadata(@PathVariable String containerId, @PathVariable String publishId, @RequestParam String jwtToken, @RequestParam String metadata) {
         // Check if the token is present or not
         if (jwtToken == null || jwtToken.isEmpty()) {
             return "JWT token is required";
         }
 
         // Create an instance of RestTemplate
         RestTemplate restTemplate = new RestTemplate();
 
         // Create HTTP headers and add the Authorization header with the Bearer token
         HttpHeaders headers = new HttpHeaders();
         headers.set("Authorization", "Bearer " + jwtToken);
 
         // Wrap the headers in an HttpEntity (which includes the Authorization header)
         HttpEntity<String> entity = new HttpEntity<>(headers);
 
         // Replace placeholders in the URL with the actual ID values
         String url = API_URL + "/entire";
 
         // Call the API with the GET method and pass the path variables (containerId, publishId)
         ResponseEntity<String> response = restTemplate.exchange(
                 url, 
                 HttpMethod.GET, 
                 entity, 
                 String.class, 
                 containerId, publishId  // Pass the path variables here
         );
 
         // Get the JSON response
         String jsonResponse = response.getBody();
 
         // Now, write the response to a file
         try {
             // Create the file object (or overwrite the existing one)
             File file = new File("metadata.json");
 
             // Create a mapper to write the JSON response to the file
             ObjectMapper objectMapper = new ObjectMapper();
             objectMapper.writeValue(file, objectMapper.readTree(jsonResponse));
 
             return "Response saved successfully in metadata.json";
         } catch (IOException e) {
             e.printStackTrace();
             return "Error saving response to file";
         }
     }
}
