package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.model.InventoryHistory;
import com.ecommerce.inventory_service.model.InventoryHistory.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    List<InventoryHistory> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryHistory> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long productId, LocalDateTime start, LocalDateTime end);

    List<InventoryHistory> findByOperationType(OperationType operationType);

    List<InventoryHistory> findByReferenceIdAndReferenceType(String referenceId, String referenceType);

    List<InventoryHistory> findByPerformedBy(String performedBy);

    @Query("SELECT ih FROM InventoryHistory ih WHERE ih.createdAt >= :fromDate ORDER BY ih.createdAt DESC")
    List<InventoryHistory> findRecentHistory(@Param("fromDate") LocalDateTime fromDate);

    // Get stock movements summary
    @Query("SELECT ih.operationType, COUNT(ih), SUM(ih.quantityChange) FROM InventoryHistory ih " +
            "WHERE ih.productId = :productId AND ih.createdAt >= :fromDate " +
            "GROUP BY ih.operationType")
    List<Object[]> getStockMovementsSummary(@Param("productId") Long productId, @Param("fromDate") LocalDateTime fromDate);
}
