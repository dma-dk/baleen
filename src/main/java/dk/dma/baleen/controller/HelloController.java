package dk.dma.baleen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String welcome() {
        return "Welcome to Baleen - S-124 Navigational Warnings Management System";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Baleen!";
    }

    @GetMapping("/status")
    public String status() {
        return "Baleen application is running successfully";
    }
}