package com.eternals.clone.coursewear.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AssetsDownloadService {

    private static final int BUFFER_SIZE = 8192; // Efficient buffer size for file handling
    private static final String BASE_DIRECTORY = "/Users/priyanka.killeder/Documents/Assets/";

    @Autowired
    private final ContainerService containerService;

    @Value("${asset-url}")
    private String API_URL;

    public AssetsDownloadService(ContainerService containerService) {
        this.containerService = containerService;
    }

    public void downloadAssets(String containerId, String authtoken, Integer depth, Boolean metadata, Boolean ordered, Boolean images) throws JsonProcessingException {
        String res = containerService.getContainerDetailsWithDepth(authtoken, containerId, depth, metadata, ordered, images);

        // Parse JSON
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(res);

        // List to hold all asset IDs with their tree paths
        List<AssetNode> assetNodes = new ArrayList<>();

        // Traverse and collect asset IDs
        traverseTree(rootNode, "/Container-" + rootNode.get("id").asText(), assetNodes);


        // Download and store assets
        for (AssetNode assetNode : assetNodes) {
            downloadAndSaveAsset(assetNode.assetId, assetNode.path, authtoken);
        }

        // Create a ZIP file from the downloaded assets
        createZipFromDirectory(BASE_DIRECTORY);
    }

    private void traverseTree(JsonNode node, String currentPath, List<AssetNode> assetNodes) {
        // Handle nodeImage at the current level
        if (node.has("nodeImage")) {
            for (JsonNode imageNode : node.get("nodeImage")) {
                if (imageNode.has("assetId")) {
                    String assetId = imageNode.get("assetId").asText();
                    String path = currentPath + "/nodeImage-" + assetId;
                    assetNodes.add(new AssetNode(assetId, path));
                }
            }
        }

        // Handle children at the current level
        if (node.has("children")) {
            for (JsonNode childNode : node.get("children")) {
                String childPath = currentPath + "/children-" + childNode.get("id").asText();
                // Add assetId from children if available
                if (childNode.has("assetId")) {
                    String assetId = childNode.get("assetId").asText();
                    assetNodes.add(new AssetNode(assetId, childPath));
                }
                // Recursively traverse deeper levels
                traverseTree(childNode, childPath, assetNodes);
            }
        }
    }


    public void downloadAndSaveAsset(String assetId, String path, String authtoken) {
        try {
            // Construct the URL
            String urlString = API_URL + assetId + "/content/exact";
            URL url = new URL(urlString);
            String fullPath = BASE_DIRECTORY + path;

            // Ensure the directory exists, create if it doesn't
            Path directoryPath = Paths.get(fullPath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);  // Creates the directory and all necessary parent directories
                System.out.println("Directory created: " + directoryPath.toString());
            }

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authtoken);

            // Get the response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Failed to download asset. Response code: " + responseCode);
                return;
            }

            // Get the content type to determine the file extension
            String contentType = connection.getContentType();
            String extension = getFileExtensionFromContentType(contentType);

            // Prepare the output file path
            Path outputPath = Paths.get(fullPath, assetId + extension);
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, outputPath);
                System.out.println("Asset downloaded and saved to " + outputPath.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get file extension based on content type
    private static String getFileExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".bin"; // Default to binary if content type is unknown
        }
        switch (contentType) {
            case "application/pdf":
                return ".pdf";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            default:
                return ".bin"; // Default extension for unknown content types
        }
    }

    public void createZipFromDirectory(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                System.err.println("Invalid directory path: " + directoryPath);
                return;
            }

            String zipFilePath = directoryPath + ".zip";

            try (FileOutputStream fos = new FileOutputStream(zipFilePath);
                 ZipOutputStream zipOut = new ZipOutputStream(fos)) {
                zipFiles(directory, directory.getName(), zipOut);
            }

            System.out.println("ZIP file created at: " + zipFilePath);

        } catch (IOException e) {
            System.err.println("Failed to create ZIP file: " + e.getMessage());
        }
    }

    private void zipFiles(File folder, String parentFolderName, ZipOutputStream zipOut) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    zipFiles(file, parentFolderName + "/" + file.getName(), zipOut);
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        String zipEntryName = parentFolderName + "/" + file.getName();
                        zipOut.putNextEntry(new ZipEntry(zipEntryName));
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zipOut.write(buffer, 0, bytesRead);
                        }
                        zipOut.closeEntry();
                    }
                }
            }
        }
    }

    static class AssetNode {
        String assetId;
        String path;

        AssetNode(String assetId, String path) {
            this.assetId = assetId;
            this.path = path;
        }
    }
}
