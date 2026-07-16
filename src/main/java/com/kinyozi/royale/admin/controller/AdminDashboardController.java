package com.kinyozi.royale.admin.controller;

import com.kinyozi.royale.admin.dto.AdminDtos.DashboardOverview;
import com.kinyozi.royale.admin.service.AdminBusinessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminBusinessService svc;

    public AdminDashboardController(AdminBusinessService s) { this.svc = s; }

    @GetMapping("/overview")
    public DashboardOverview overview() {
        return svc.overview();
    }
}
