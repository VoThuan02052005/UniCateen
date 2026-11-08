package vn.edu.phenikaa.unicateen.api.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.api.common.StatusRequest;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.IssueLineInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.PlanInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.ReceiptLineInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryViews.*;
import vn.edu.phenikaa.unicateen.application.inventory.SupplierService;
import vn.edu.phenikaa.unicateen.application.inventory.SupplierService.SupplierInput;
import vn.edu.phenikaa.unicateen.application.inventory.SupplierService.SupplierView;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Shift;

/** Nhom 4: nha cung cap, phieu nhap, ton kho, dieu chinh, phieu xuat (UC4.1-UC4.4). */
@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final SupplierService suppliers;
    private final InventoryService inventory;

    public StockController(SupplierService suppliers, InventoryService inventory) {
        this.suppliers = suppliers;
        this.inventory = inventory;
    }

    /** DV07: so dien thoai 10 chu so, bat dau bang 0. */
    public record SupplierRequest(@NotBlank @Size(min = 2, max = 100) String name, @Size(max = 14) String taxCode,
                                  @Size(max = 100) String contactName,
                                  @NotBlank @Pattern(regexp = "0\\d{9}", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
                                  String phone,
                                  @Email @Size(max = 100) String email, @Size(max = 255) String address,
                                  @Size(max = 100) String mainGoods) {
        SupplierInput toInput() {
            return new SupplierInput(name, taxCode, contactName, phone, email, address, mainGoods);
        }
    }

    public record ReceiptLineRequest(@NotNull Long ingredientId,
                                     @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity,
                                     @PositiveOrZero long unitCost, LocalDate expiryDate) {
    }

    public record ReceiptRequest(@NotNull Long supplierId, LocalDate receiptDate,
                                 @NotEmpty List<@Valid ReceiptLineRequest> lines) {
    }

    public record PlanRequest(@NotNull Long menuItemId, @Positive int plannedPortions) {
    }

    public record IssueRequest(LocalDate issueDate, @NotNull Shift shift, @NotEmpty List<@Valid PlanRequest> plans) {
    }

    public record IssueLineRequest(@NotNull Long lotId, BigDecimal suggestedQty,
                                   @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal issuedQty,
                                   boolean outsideRecipe) {
    }

    public record ConfirmIssueRequest(@NotEmpty List<@Valid IssueLineRequest> lines) {
    }

    public record AdjustRequest(@NotNull Long lotId,
                                @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal actualQty,
                                @NotBlank @Size(max = 255) String reason) {
    }

    // ---------------------------------------------------------------- UC4.1
    @GetMapping("/suppliers")
    public List<SupplierView> suppliers() {
        return suppliers.list();
    }

    @PostMapping("/suppliers")
    public SupplierView createSupplier(@Valid @RequestBody SupplierRequest r) {
        return suppliers.create(r.toInput());
    }

    @PutMapping("/suppliers/{id}")
    public SupplierView updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest r) {
        return suppliers.update(id, r.toInput());
    }

    @PatchMapping("/suppliers/{id}/status")
    public SupplierView supplierStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return suppliers.setStatus(id, ActiveStatus.valueOf(r.status()));
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        suppliers.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- UC4.2
    @GetMapping("/receipts")
    public List<ReceiptView> receipts() {
        return inventory.receipts();
    }

    @GetMapping("/receipts/{id}")
    public ReceiptView receipt(@PathVariable Long id) {
        return inventory.receipt(id);
    }

    @PostMapping("/receipts")
    public ReceiptView createReceipt(@Valid @RequestBody ReceiptRequest r, @AuthenticationPrincipal CurrentUser me) {
        return inventory.createReceipt(r.supplierId(), r.receiptDate(), receiptLines(r), me);
    }

    @PutMapping("/receipts/{id}")
    public ReceiptView updateReceipt(@PathVariable Long id, @Valid @RequestBody ReceiptRequest r) {
        return inventory.updateReceipt(id, receiptLines(r));
    }

    @PostMapping("/receipts/{id}/confirm")
    public ReceiptView confirmReceipt(@PathVariable Long id, @AuthenticationPrincipal CurrentUser me) {
        return inventory.confirmReceipt(id, me);
    }

    @PostMapping("/receipts/{id}/cancel")
    public ReceiptView cancelReceipt(@PathVariable Long id) {
        return inventory.cancelReceipt(id);
    }

    // ---------------------------------------------------------------- UC4.3
    @GetMapping("/levels")
    public List<StockLevel> levels() {
        return inventory.stockLevels();
    }

    @GetMapping("/lots")
    public List<LotView> lots() {
        return inventory.lotsInStock();
    }

    @GetMapping("/adjustments")
    public List<AdjustmentView> adjustments() {
        return inventory.adjustments();
    }

    /** BR37: chi MANAGER (SecurityConfig), bat buoc ly do. */
    @PostMapping("/adjustments")
    public AdjustmentView adjust(@Valid @RequestBody AdjustRequest r, @AuthenticationPrincipal CurrentUser me) {
        return inventory.adjust(r.lotId(), r.actualQty(), r.reason(), me);
    }

    // ---------------------------------------------------------------- UC4.4
    @GetMapping("/issues")
    public List<IssueView> issues() {
        return inventory.issues();
    }

    @GetMapping("/issues/{id}")
    public IssueView issue(@PathVariable Long id) {
        return inventory.issue(id);
    }

    @PostMapping("/issues")
    public IssueView createIssue(@Valid @RequestBody IssueRequest r, @AuthenticationPrincipal CurrentUser me) {
        return inventory.createIssue(r.issueDate(), r.shift(), plans(r.plans()), me);
    }

    @PutMapping("/issues/{id}/plans")
    public IssueView updatePlans(@PathVariable Long id, @Valid @RequestBody IssueRequest r) {
        return inventory.updatePlans(id, plans(r.plans()));
    }

    @GetMapping("/issues/{id}/suggestion")
    public IssueSuggestion suggest(@PathVariable Long id) {
        return inventory.suggestIssue(id);
    }

    @PostMapping("/issues/{id}/confirm")
    public IssueView confirmIssue(@PathVariable Long id, @Valid @RequestBody ConfirmIssueRequest r,
                                  @AuthenticationPrincipal CurrentUser me) {
        return inventory.confirmIssue(id, r.lines().stream().map(l -> new IssueLineInput(l.lotId(), l.suggestedQty(),
                l.issuedQty(), l.outsideRecipe())).toList(), me);
    }

    @PostMapping("/issues/{id}/cancel")
    public IssueView cancelIssue(@PathVariable Long id) {
        return inventory.cancelIssue(id);
    }

    private static List<ReceiptLineInput> receiptLines(ReceiptRequest r) {
        return r.lines().stream().map(l -> new ReceiptLineInput(l.ingredientId(), l.quantity(), l.unitCost(),
                l.expiryDate())).toList();
    }

    private static List<PlanInput> plans(List<PlanRequest> plans) {
        return plans.stream().map(p -> new PlanInput(p.menuItemId(), p.plannedPortions())).toList();
    }
}
