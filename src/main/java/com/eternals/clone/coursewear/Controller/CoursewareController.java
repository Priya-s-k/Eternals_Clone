package com.eternals.clone.coursewear.Controller;

import com.eternals.clone.coursewear.Service.AssetsDownloadService;
import com.eternals.clone.coursewear.Service.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;

@RestController
@RequestMapping("/container")
public class CoursewareController {

     ContainerService containerService;

     @Autowired
     public CoursewareController(ContainerService containerService) {
        this.containerService = containerService;
     }

    @GetMapping("/{containerId}/{publishId}/recursive")
    public String fetchCoursewareRecursively(@PathVariable String containerId, @PathVariable String publishId, @RequestParam String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Authtoken is Missing");
        }
 
        try {
            containerService.fetchAndProcessCourseware(containerId, publishId, jwtToken, new ArrayList<>());
            return "Recursive fetch completed successfully.";
        } catch (Exception e) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR "+e);
        }
    }
}