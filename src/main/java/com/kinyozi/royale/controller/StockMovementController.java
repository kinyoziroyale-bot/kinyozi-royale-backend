package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.StockMovementDtos.CreateRequest;
import com.kinyozi.royale.service.StockMovementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock-movements")
public class StockMovementController {

    private final StockMovementService svc;

    public StockMovementController(StockMovementService svc) {
        this.svc = svc;
    }

    @GetMapping
    public List<?> list() {
        return svc.list();
    }

    @PostMapping
    public Object create(@Valid @RequestBody CreateRequest r) {
        return svc.create(r, r);
    }
}
