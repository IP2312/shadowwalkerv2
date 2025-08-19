package org.example.shadowwalkerv2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class RoutController {

    @GetMapping("/test")
    public String test(){
        return "test";
    }

}
