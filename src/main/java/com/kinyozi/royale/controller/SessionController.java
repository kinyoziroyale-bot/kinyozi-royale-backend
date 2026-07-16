package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.SessionDtos.AddLineRequest;
import com.kinyozi.royale.dto.SessionDtos.OpenSessionRequest;
import com.kinyozi.royale.dto.SessionDtos.SessionResponse;
import com.kinyozi.royale.dto.SessionDtos.UpdateLineRequest;
import com.kinyozi.royale.service.SessionService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/open")
    public List<SessionResponse> open() {
        return sessionService.listOpen();
    }

    @GetMapping("/completed")
    public List<SessionResponse> completed(@RequestParam(value = "date", required = false) String date) {
        return sessionService.listCompletedOnDate(date);
    }

    @GetMapping("/{id}")
    public SessionResponse get(@PathVariable UUID id) {
        return sessionService.get(id);
    }

    @PostMapping
    public SessionResponse create(@Valid @RequestBody OpenSessionRequest req) {
        return sessionService.open(req);
    }

    @PostMapping("/{id}/lines")
    public SessionResponse addLine(@PathVariable UUID id, @Valid @RequestBody AddLineRequest req) {
        return sessionService.addLine(id, req);
    }

    @PutMapping("/{id}/lines/{lineId}")
    public SessionResponse updateLine(@PathVariable UUID id,
                                      @PathVariable UUID lineId,
                                      @Valid @RequestBody UpdateLineRequest req) {
        return sessionService.updateLine(id, lineId, req);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    public SessionResponse removeLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        return sessionService.removeLine(id, lineId);
    }

    @PostMapping("/{id}/finalize")
    public SessionResponse finalizeSession(@PathVariable UUID id) {
        return sessionService.finalize(id);
    }

    @PostMapping("/{id}/cancel")
    public SessionResponse cancel(@PathVariable UUID id) {
        return sessionService.cancel(id);
    }
}
