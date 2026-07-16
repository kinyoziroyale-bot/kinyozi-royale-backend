package com.kinyozi.royale.controller;

import com.kinyozi.royale.model.ServiceItem;
import com.kinyozi.royale.model.Worker;
import com.kinyozi.royale.repository.ServiceItemRepository;
import com.kinyozi.royale.repository.WorkerRepository;
import com.kinyozi.royale.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Server-side enforcement of category-based worker filtering for the POS.
 * GET /workers/by-service/{serviceId} returns only workers whose
 * category set intersects the service's category set, scoped to the
 * current tenant. If the service has no categories configured, returns
 * an empty list — we never silently leak every worker.
 *
 * Worker's category accessor name varies across branches
 * (getCategoryIds / getCategories / getWorkerCategories), so we resolve
 * it reflectively at runtime to stay compatible.
 */
@RestController
@RequestMapping("/workers/by-service")
public class WorkerFilterController {

    private final WorkerRepository workers;
    private final ServiceItemRepository services;

    public WorkerFilterController(WorkerRepository w, ServiceItemRepository s) {
        this.workers = w; this.services = s;
    }

    @GetMapping("/{serviceId}")
    public List<Worker> qualifiedFor(@PathVariable UUID serviceId) {
        UUID tenant = CurrentUser.tenantId();
        ServiceItem svc = services.findById(serviceId).orElse(null);
        if (svc == null || !tenant.equals(svc.getTenantId())) return List.of();

        Collection<UUID> svcCatsRaw = svc.getCategoryIds();
        Set<UUID> svcCats = svcCatsRaw == null ? Set.of() : new HashSet<>(svcCatsRaw);
        if (svcCats.isEmpty()) return List.of();

        return workers.findByTenantId(tenant).stream()
                .filter(Worker::isActive)
                .filter(w -> {
                    Collection<UUID> wc = workerCategories(w);
                    if (wc == null || wc.isEmpty()) return false;
                    for (UUID c : wc) if (svcCats.contains(c)) return true;
                    return false;
                })
                .toList();
    }

    /* ------------------------------------------------------------------ */

    private static volatile Method WORKER_CAT_ACCESSOR;
    private static volatile boolean WORKER_CAT_RESOLVED;

    @SuppressWarnings("unchecked")
    private static Collection<UUID> workerCategories(Worker w) {
        Method m = resolveAccessor(w.getClass());
        if (m == null) return null;
        try {
            Object v = m.invoke(w);
            if (v == null) return null;
            if (v instanceof Collection<?> col) {
                List<UUID> out = new ArrayList<>(col.size());
                for (Object o : col) {
                    if (o instanceof UUID u) out.add(u);
                    else if (o != null) {
                        try { out.add(UUID.fromString(o.toString())); } catch (Exception ignored) {}
                    }
                }
                return out;
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    private static Method resolveAccessor(Class<?> cls) {
        if (WORKER_CAT_RESOLVED) return WORKER_CAT_ACCESSOR;
        synchronized (WorkerFilterController.class) {
            if (WORKER_CAT_RESOLVED) return WORKER_CAT_ACCESSOR;
            String[] names = {
                "getCategoryIds", "getCategories", "getWorkerCategories",
                "getCategoryIdList", "getAllowedCategoryIds"
            };
            for (String n : names) {
                try {
                    Method m = cls.getMethod(n);
                    if (Collection.class.isAssignableFrom(m.getReturnType())) {
                        WORKER_CAT_ACCESSOR = m;
                        break;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            WORKER_CAT_RESOLVED = true;
            return WORKER_CAT_ACCESSOR;
        }
    }
}
