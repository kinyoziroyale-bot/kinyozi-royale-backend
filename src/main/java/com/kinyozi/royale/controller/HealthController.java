package com.kinyozi.royale.controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class HealthController {
  @GetMapping("/health") public Map<String,Object> ok(){ return Map.of("status","ok"); }
}
