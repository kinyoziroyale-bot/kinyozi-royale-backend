package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.WorkerCategoryDto;
import com.kinyozi.royale.model.WorkerCategory;
import com.kinyozi.royale.repository.WorkerCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkerCategoryService {

    private final WorkerCategoryRepository repo;

    public WorkerCategoryService(WorkerCategoryRepository repo) {
        this.repo = repo;
    }

    public List<WorkerCategoryDto.Response> list() {
        UUID tenant = TenantContext.current();
        return repo.findByTenantId(tenant).stream().map(this::toDto).toList();
    }

    public WorkerCategoryDto.Response create(WorkerCategoryDto.Request req) {
        UUID tenant = TenantContext.current();
        repo.findByTenantIdAndNameIgnoreCase(tenant, req.name).ifPresent(c -> {
            throw new IllegalArgumentException("Category already exists");
        });
        WorkerCategory c = WorkerCategory.builder()
                .tenantId(tenant)
                .name(req.name.trim())
                .description(req.description)
                .build();
        return toDto(repo.save(c));
    }

    public WorkerCategoryDto.Response update(UUID id, WorkerCategoryDto.Request req) {
        WorkerCategory c = load(id);
        c.setName(req.name.trim());
        c.setDescription(req.description);
        return toDto(repo.save(c));
    }

    public void delete(UUID id) {
        WorkerCategory c = load(id);
        if (!c.getWorkers().isEmpty()) {
            throw new IllegalStateException("Category in use by workers");
        }
        repo.delete(c);
    }

    private WorkerCategory load(UUID id) {
        WorkerCategory c = repo.findById(id).orElseThrow();
        if (!c.getTenantId().equals(TenantContext.current()))
            throw new SecurityException("Wrong tenant");
        return c;
    }

    private WorkerCategoryDto.Response toDto(WorkerCategory c) {
        WorkerCategoryDto.Response r = new WorkerCategoryDto.Response();
        r.id = c.getId();
        r.name = c.getName();
        r.description = c.getDescription();
        r.workerCount = c.getWorkers() == null ? 0 : c.getWorkers().size();
        return r;
    }
}
