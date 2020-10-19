package com.midcielab.tgbotgateway.controller;

import java.util.List;

import com.midcielab.tgbotgateway.model.Model;
import com.midcielab.tgbotgateway.service.processVidService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/processVid", produces = MediaType.APPLICATION_JSON_VALUE)
public class MainController {
    
    @PostMapping
    public ResponseEntity<String> processM3U8(@RequestBody List<Model> modelInput) {
        System.out.println(modelInput);
        new processVidService(modelInput);               
        return ResponseEntity.ok("okla");
    }
}
