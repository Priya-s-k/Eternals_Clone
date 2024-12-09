package com.eternals.clone.coursewear.Controller;

import com.eternals.clone.coursewear.Service.AssetsDownloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
@RestController
@RequestMapping("/assets")
public class AssetsController {


    AssetsDownloadService assetsDownloadService;

    @Autowired
    public AssetsController(AssetsDownloadService assetsDownloadService) {
        this.assetsDownloadService = assetsDownloadService;
    }

    @GetMapping("/assetDownload")
    public String downloadAssets(
            @RequestHeader("Authorization") String authToken,
            @RequestParam String containerId,
            @RequestParam(required = false) Integer depth,
            @RequestParam(required = false) Boolean metadata,
            @RequestParam(required = false) Boolean ordered,
            @RequestParam(required = false) Boolean images){
        if (authToken == null || authToken.isEmpty()) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Authtoken is Missing");
        }
        try {
            assetsDownloadService.downloadAssets(containerId,authToken,depth,metadata,ordered,images);
            return "Download is Success";
        } catch (Exception e) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR "+e);
        }
    }


    @GetMapping("/assetDownload1")
    public String downloadAssets(){
        return "ok";
    }

}
