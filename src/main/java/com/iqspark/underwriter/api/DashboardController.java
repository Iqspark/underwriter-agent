package com.iqspark.underwriter.api;

import com.iqspark.underwriter.dashboard.DashboardService;
import com.iqspark.underwriter.dashboard.DashboardView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Embedded underwriting-performance dashboard (KPIs). Read-only; metrics also go to Prometheus. */
@RestController
@RequestMapping("/api/underwriting/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardView dashboard() {
        return dashboardService.snapshot();
    }
}
