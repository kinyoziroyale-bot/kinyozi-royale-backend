package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.WorkerCommissionRequest;
import com.kinyozi.royale.dto.WorkerDto;
import com.kinyozi.royale.model.SalaryPeriod;
import com.kinyozi.royale.model.Worker;
import com.kinyozi.royale.model.WorkerCategory;
import com.kinyozi.royale.repository.WorkerCategoryRepository;
import com.kinyozi.royale.repository.WorkerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class WorkerService {

    private final WorkerRepository repo;
    private final WorkerCategoryRepository catRepo;

    public WorkerService(WorkerRepository repo, WorkerCategoryRepository catRepo) {
        this.repo = repo;
        this.catRepo = catRepo;
    }

    public List<WorkerDto.Response> list(Boolean activeOnly) {
        UUID tenant = TenantContext.current();
        List<Worker> ws = (activeOnly != null && activeOnly)
                ? repo.findByTenantIdAndActive(tenant, true)
                : repo.findByTenantId(tenant);
        return ws.stream().map(this::toDto).toList();
    }

    public WorkerDto.Response create(WorkerDto.Request req) {
        UUID tenant = TenantContext.current();
        Worker w = Worker.builder()
                .tenantId(tenant)
                .fullName(req.fullName.trim())
                .phoneNumber(req.phoneNumber.trim())
                .email(req.email)
                .profilePhoto(req.profilePhoto)
                .active(true)
                .categories(resolveCategories(req.categoryIds, tenant))
                .employmentType(req.employmentType)
                .basicSalary(req.basicSalary)
                .salaryPeriod(req.salaryPeriod != null ? req.salaryPeriod : SalaryPeriod.MONTHLY)
                .build();
        return toDto(repo.save(w));
    }

    public WorkerDto.Response update(UUID id, WorkerDto.Request req) {
        Worker w = load(id);
        w.setFullName(req.fullName.trim());
        w.setPhoneNumber(req.phoneNumber.trim());
        w.setEmail(req.email);
        w.setProfilePhoto(req.profilePhoto);
        w.setCategories(resolveCategories(req.categoryIds, w.getTenantId()));
        // Only overwrite payroll fields when the request explicitly includes them,
        // so existing behaviour is preserved when older clients call PUT.
        if (req.employmentType != null) w.setEmploymentType(req.employmentType);
        if (req.basicSalary   != null)  w.setBasicSalary(req.basicSalary);
        if (req.salaryPeriod  != null)  w.setSalaryPeriod(req.salaryPeriod);
        return toDto(repo.save(w));
    }

    public WorkerDto.Response setActive(UUID id, boolean active) {
        Worker w = load(id); w.setActive(active); return toDto(repo.save(w));
    }

    public WorkerDto.Response get(UUID id) { return toDto(load(id)); }

    public void delete(UUID id) {
        Worker w = load(id); w.getCategories().clear(); repo.delete(w);
    }

    private Worker load(UUID id) {
        Worker w = repo.findById(id).orElseThrow();
        if (!w.getTenantId().equals(TenantContext.current()))
            throw new SecurityException("Wrong tenant");
        return w;
    }

    private Set<WorkerCategory> resolveCategories(List<UUID> ids, UUID tenant) {
        Set<WorkerCategory> out = new HashSet<>();
        for (UUID id : ids) {
            WorkerCategory c = catRepo.findById(id).orElseThrow(
                    () -> new IllegalArgumentException("Unknown category " + id));
            if (!c.getTenantId().equals(tenant))
                throw new SecurityException("Cross-tenant category");
            out.add(c);
        }
        return out;
    }

    private WorkerDto.Response toDto(Worker w) {
        WorkerDto.Response r = new WorkerDto.Response();
        r.id = w.getId();
        r.fullName = w.getFullName();
        r.phoneNumber = w.getPhoneNumber();
        r.email = w.getEmail();
        r.profilePhoto = w.getProfilePhoto();
        r.active = w.isActive();
        r.categoryIds = w.getCategories().stream().map(WorkerCategory::getId).toList();
        r.categoryNames = w.getCategories().stream().map(WorkerCategory::getName).toList();
        r.commissionType = w.getCommissionType();
        r.commissionValue = w.getCommissionValue();
        r.employmentType = w.getEmploymentType();
        r.basicSalary    = w.getBasicSalary();
        r.salaryPeriod   = w.getSalaryPeriod();
        r.createdAt = w.getCreatedAt();
        r.updatedAt = w.getUpdatedAt();
        return r;
    }

    public WorkerDto.Response updateCommission(UUID id, WorkerCommissionRequest req) {
        Worker w = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (!w.getTenantId().equals(TenantContext.current())) throw new SecurityException("Wrong tenant");
        w.setCommissionType(req.commissionType());
        w.setCommissionValue(req.commissionValue());
        return toDto(repo.save(w));
    }
}
