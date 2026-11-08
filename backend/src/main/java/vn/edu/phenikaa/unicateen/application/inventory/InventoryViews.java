package vn.edu.phenikaa.unicateen.application.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import vn.edu.phenikaa.unicateen.domain.enums.DocumentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Shift;
import vn.edu.phenikaa.unicateen.domain.model.StockAdjustment;
import vn.edu.phenikaa.unicateen.domain.model.StockIssue;
import vn.edu.phenikaa.unicateen.domain.model.StockReceipt;

/** Du lieu tra ve cho giao dien nhom kho. */
public final class InventoryViews {
    private InventoryViews() {
    }

    public record ReceiptLineView(Long ingredientId, String ingredientName, String unit, BigDecimal quantity,
                                  long unitCost, LocalDate expiryDate, long lineTotal) {
    }

    public record ReceiptView(Long id, String code, Long supplierId, String supplierName, LocalDate receiptDate,
                              DocumentStatus status, long totalValue, String createdBy, LocalDateTime confirmedAt,
                              List<ReceiptLineView> lines) {
        public static ReceiptView of(StockReceipt r) {
            return new ReceiptView(r.getId(), r.getCode(), r.getSupplier().getId(), r.getSupplier().getName(),
                    r.getReceiptDate(), r.getStatus(), r.getTotalValue(), r.getCreatedBy().getFullName(),
                    r.getConfirmedAt(), r.getDetails().stream().map(d -> new ReceiptLineView(
                            d.getIngredient().getId(), d.getIngredient().getName(), d.getIngredient().getUnit(),
                            d.getQuantity(), d.getUnitCost(), d.getExpiryDate(), d.getLineTotal())).toList());
        }
    }

    public record PlanView(Long menuItemId, String menuItemName, int plannedPortions) {
    }

    public record IssueLineView(Long lotId, String lotCode, String ingredientName, String unit,
                                BigDecimal suggestedQty, BigDecimal issuedQty, boolean outsideRecipe) {
    }

    public record IssueView(Long id, String code, LocalDate issueDate, Shift shift, DocumentStatus status,
                            String createdBy, LocalDateTime confirmedAt, List<PlanView> plans,
                            List<IssueLineView> lines) {
        public static IssueView of(StockIssue i) {
            return new IssueView(i.getId(), i.getCode(), i.getIssueDate(), i.getShift(), i.getStatus(),
                    i.getCreatedBy().getFullName(), i.getConfirmedAt(),
                    i.getPlans().stream().map(p -> new PlanView(p.getMenuItem().getId(), p.getMenuItem().getName(),
                            p.getPlannedPortions())).toList(),
                    i.getDetails().stream().map(d -> new IssueLineView(d.getLot().getId(), d.getLot().getCode(),
                            d.getLot().getIngredient().getName(), d.getLot().getIngredient().getUnit(),
                            d.getSuggestedQty(), d.getIssuedQty(), d.isOutsideRecipe())).toList());
        }
    }

    /** Dong goi y xuat theo lo (FEFO, BR34). */
    public record SuggestedLine(Long lotId, String lotCode, Long ingredientId, String ingredientName, String unit,
                                LocalDate expiryDate, BigDecimal available, BigDecimal suggestedQty) {
    }

    public record Shortage(Long ingredientId, String ingredientName, String unit, BigDecimal missingQty) {
    }

    public record IssueSuggestion(List<SuggestedLine> lines, List<Shortage> shortages,
                                  List<String> itemsWithoutRecipe) {
    }

    public record LotView(Long id, String code, Long ingredientId, String ingredientName, String unit,
                          LocalDate receivedDate, LocalDate expiryDate, BigDecimal remainingQty, boolean expired,
                          boolean nearExpiry) {
    }

    /** Ton theo nguyen lieu; {@code available} khong tinh lo qua han (BR34). */
    public record StockLevel(Long ingredientId, String code, String name, String groupName, String unit,
                             BigDecimal available, BigDecimal expiredQty, BigDecimal minStock, boolean low,
                             int nearExpiryLots) {
    }

    public record AdjustmentView(Long id, String code, String lotCode, String ingredientName, BigDecimal qtyBefore,
                                 BigDecimal qtyAfter, String reason, String createdBy, LocalDateTime createdAt) {
        public static AdjustmentView of(StockAdjustment a) {
            return new AdjustmentView(a.getId(), a.getCode(), a.getLot().getCode(),
                    a.getLot().getIngredient().getName(), a.getQtyBefore(), a.getQtyAfter(), a.getReason(),
                    a.getCreatedBy().getFullName(), a.getCreatedAt());
        }
    }
}
