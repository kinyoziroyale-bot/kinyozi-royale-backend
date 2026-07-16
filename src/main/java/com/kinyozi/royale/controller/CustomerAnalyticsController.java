package com.kinyozi.royale.controller;

/**
 * Intentionally inert.
 *
 * The analytics, reminders, and next-visit endpoints all live on
 * {@link CustomerController} at /api/customers/*. This file used to expose a
 * second copy under @RequestMapping("/api/customers"), which (with the global
 * /api context-path) resolved to /api/api/customers/* and was unreachable.
 *
 * Keeping a non-bean class here means no duplicate handler mappings are
 * registered and the file can stay in place without breaking existing imports.
 */
public final class CustomerAnalyticsController {
    private CustomerAnalyticsController() {}
}
