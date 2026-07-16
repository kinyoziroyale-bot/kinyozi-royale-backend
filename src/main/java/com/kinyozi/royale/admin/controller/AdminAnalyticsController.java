package com.kinyozi.royale.admin.controller;

import com.kinyozi.royale.admin.dto.AdminDtos.PlatformAnalytics;
import com.kinyozi.royale.admin.service.AdminBusinessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/analytics")
public class AdminAnalyticsController {

    private final AdminBusinessService svc;

    public AdminAnalyticsController(AdminBusinessService s) { this.svc = s; }

    @GetMapping
    public PlatformAnalytics analytics(@RequestParam(name = "days", defaultValue = "30") int days) {
        int d = Math.max(7, Math.min(days, 365));
        return svc.analytics(d);
    }
}
