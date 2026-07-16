package com.kinyozi.royale.service;
import com.kinyozi.royale.dto.StockMovementDtos.CreateRequest;
import com.kinyozi.royale.exception.*;
import com.kinyozi.royale.model.*;
import com.kinyozi.royale.repository.*;
import com.kinyozi.royale.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class StockMovementService {
  private final InventoryItemRepository items;
  private final com.kinyozi.royale.repository.WorkerRepository workers;
  private final StockMovementRepository moves;
  public StockMovementService(InventoryItemRepository i, com.kinyozi.royale.repository.WorkerRepository w, StockMovementRepository m){
    items=i; workers=w; moves=m;
  }

  public List<StockMovement> list(){ return moves.findByTenantId(CurrentUser.tenantId()); }

  @Transactional
  public StockMovement create(CreateRequest r, @Valid CreateRequest createRequest){
    UUID tid = CurrentUser.tenantId();
    InventoryItem it = items.findById(r.itemId())
        .filter(x -> x.getTenantId().equals(tid))
        .orElseThrow(() -> new NotFoundException("Inventory item not found"));
    Worker w = r.workerId()==null ? null :
        workers.findById(r.workerId()).filter(x -> x.getTenantId().equals(tid)).orElse(null);
    int qty = Math.abs(r.quantity());
    int signed = "RESTOCK".equalsIgnoreCase(r.reason()) ? qty : -qty;
    it.setCurrentQty(Math.max(0, it.getCurrentQty() + signed));
    items.save(it);
    return moves.save(StockMovement.builder()
        .tenantId(tid).itemId(it.getId()).itemName(it.getName())
        .workerId(w==null?null:w.getId()).workerName(w==null?null:w.getFullName())
        .quantity(signed).unit(it.getUnit())
        .reason(r.reason().toUpperCase()).note(r.note())
        .build());
  }

    public List<?> listRecentMovements(Object tenantId, int days) {
        // If your existing tenantId type is UUID, Long, etc.,
        // keep the real typed version instead.
        throw new UnsupportedOperationException("Replace with your existing typed implementation");
    }

}
