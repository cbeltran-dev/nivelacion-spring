package com.example.api_tienda.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/test")
public class HolaMundoController {

    @GetMapping("/hola")
    public String holaMundo(){
        return "Hola mundo desde Spring Boot!";
    }
}
