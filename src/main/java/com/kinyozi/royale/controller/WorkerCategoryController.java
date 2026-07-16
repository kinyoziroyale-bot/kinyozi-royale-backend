package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.WorkerCategoryDto;
import com.kinyozi.royale.service.WorkerCategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/worker-categories")
public class WorkerCategoryController {

    private final WorkerCategoryService svc;
    public WorkerCategoryController(WorkerCategoryService s) { this.svc = s; }

    @GetMapping
    public List<WorkerCategoryDto.Response> list() { return svc.list(); }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public WorkerCategoryDto.Response create(@Valid @RequestBody WorkerCategoryDto.Request r) {
        return svc.create(r);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public WorkerCategoryDto.Response update(@PathVariable UUID id,
                                             @Valid @RequestBody WorkerCategoryDto.Request r) {
        return svc.update(id, r);
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { svc.delete(id); }
}
