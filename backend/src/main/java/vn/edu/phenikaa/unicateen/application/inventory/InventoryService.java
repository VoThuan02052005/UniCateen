package vn.edu.phenikaa.unicateen.application.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.IngredientService;
import vn.edu.phenikaa.unicateen.application.catalog.RecipeService;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryViews.*;
import vn.edu.phenikaa.unicateen.application.port.IAuditLogger;
import vn.edu.phenikaa.unicateen.application.port.IngredientRepository;
import vn.edu.phenikaa.unicateen.application.port.InventoryTransactionRepository;
import vn.edu.phenikaa.unicateen.application.port.MenuItemRepository;
import vn.edu.phenikaa.unicateen.application.port.StockAdjustmentRepository;
import vn.edu.phenikaa.unicateen.application.port.StockIssueRepository;
import vn.edu.phenikaa.unicateen.application.port.StockLotRepository;
import vn.edu.phenikaa.unicateen.application.port.StockReceiptRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.Shift;
import vn.edu.phenikaa.unicateen.domain.model.*;

/**
 * Nhom 4: nhap kho (UC4.2), theo doi ton va dieu chinh (UC4.3), xuat nguyen lieu theo ca (UC4.4).
 * Ton chi thay doi qua giao dich kho IN, OUT, ADJUST (BR33); quantity mang dau.
 */
@Service
@Transactional
public class InventoryService {
    private final StockReceiptRepository receipts;
    private final StockIssueRepository issues;
    private final StockLotRepository lots;
    private final StockAdjustmentRepository adjustments;
    private final InventoryTransactionRepository transactions;
    private final IngredientRepository ingredientRepo;
    private final MenuItemRepository menuItems;
    private final SupplierService suppliers;
    private final IngredientService ingredients;
    private final RecipeService recipes;
    private final CodeGenerator codes;
    private final Actors actors;
    private final IAuditLogger audit;
    private final int expiryWarningDays;

    public InventoryService(StockReceiptRepository receipts, StockIssueRepository issues, StockLotRepository lots,
                            StockAdjustmentRepository adjustments, InventoryTransactionRepository transactions,
                            IngredientRepository ingredientRepo, MenuItemRepository menuItems,
                            SupplierService suppliers, IngredientService ingredients, RecipeService recipes,
                            CodeGenerator codes, Actors actors, IAuditLogger audit,
                            @Value("${unicateen.stock.expiry-warning-days:3}") int expiryWarningDays) {
        this.receipts = receipts;
        this.issues = issues;
        this.lots = lots;
        this.adjustments = adjustments;
        this.transactions = transactions;
        this.ingredientRepo = ingredientRepo;
        this.menuItems = menuItems;
        this.suppliers = suppliers;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.codes = codes;
        this.actors = actors;
        this.audit = audit;
        this.expiryWarningDays = expiryWarningDays;
    }

    public record ReceiptLineInput(Long ingredientId, BigDecimal quantity, long unitCost, LocalDate expiryDate) {
    }

    public record PlanInput(Long menuItemId, int plannedPortions) {
    }

    public record IssueLineInput(Long lotId, BigDecimal suggestedQty, BigDecimal issuedQty, boolean outsideRecipe) {
    }

    // ------------------------------------------------------------ UC4.2 phieu nhap

    @Transactional(readOnly = true)
    public List<ReceiptView> receipts() {
        return receipts.findTop200ByOrderByIdDesc().stream().map(ReceiptView::of).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptView receipt(Long id) {
        return ReceiptView.of(getReceipt(id));
    }

    public ReceiptView createReceipt(Long supplierId, LocalDate date, List<ReceiptLineInput> lines, CurrentUser actor) {
        StockReceipt r = new StockReceipt(codes.next("PN", 3, receipts::findMaxCodeWithPrefix),
                suppliers.get(supplierId), date == null ? Clock.today() : date, actors.load(actor));
        fillReceipt(r, lines);
        return ReceiptView.of(receipts.save(r));
    }

    /** Sua phieu o trang thai Nhap (BR31). */
    public ReceiptView updateReceipt(Long id, List<ReceiptLineInput> lines) {
        StockReceipt r = receipts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("phiếu nhập", id));
        r.clearLines();
        fillReceipt(r, lines);
        return ReceiptView.of(r);
    }

    /** Xac nhan: moi dong tao mot lo va mot giao dich IN; phieu chi ghi kho mot lan (BR33, GD6). */
    public ReceiptView confirmReceipt(Long id, CurrentUser actor) {
        StockReceipt r = receipts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("phiếu nhập", id));
        r.confirm();
        User by = actors.load(actor);
        for (StockReceiptDetail line : r.getDetails()) {
            StockLot lot = lots.save(StockLot.fromReceiptLine(codes.next("L", 3, lots::findMaxCodeWithPrefix), line));
            transactions.save(InventoryTransaction.in(lot, line.getQuantity(), r.getId(), by));
        }
        return ReceiptView.of(r);
    }

    public ReceiptView cancelReceipt(Long id) {
        StockReceipt r = receipts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("phiếu nhập", id));
        r.cancel();
        return ReceiptView.of(r);
    }

    private void fillReceipt(StockReceipt r, List<ReceiptLineInput> lines) {
        for (ReceiptLineInput l : lines) {
            r.addLine(ingredients.get(l.ingredientId()), l.quantity(), l.unitCost(), l.expiryDate());
        }
    }

    private StockReceipt getReceipt(Long id) {
        return receipts.findById(id).orElseThrow(() -> new NotFoundException("phiếu nhập", id));
    }

    // ------------------------------------------------------------ UC4.4 phieu xuat

    @Transactional(readOnly = true)
    public List<IssueView> issues() {
        return issues.findTop200ByOrderByIdDesc().stream().map(IssueView::of).toList();
    }

    @Transactional(readOnly = true)
    public IssueView issue(Long id) {
        return IssueView.of(getIssue(id));
    }

    public IssueView createIssue(LocalDate date, Shift shift, List<PlanInput> plans, CurrentUser actor) {
        StockIssue i = new StockIssue(codes.next("PX", 3, issues::findMaxCodeWithPrefix),
                date == null ? Clock.today() : date, shift, actors.load(actor));
        setPlans(i, plans);
        return IssueView.of(issues.save(i));
    }

    public IssueView updatePlans(Long id, List<PlanInput> plans) {
        StockIssue i = issues.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("phiếu xuất", id));
        i.getPlans().clear();
        setPlans(i, plans);
        return IssueView.of(i);
    }

    /**
     * Goi y xuat: nhu cau moi nguyen lieu = so phan du kien x dinh muc cong thuc dang ap dung,
     * phan bo vao cac lo theo FEFO, bo qua lo qua han (BR34). Khong thay doi ton. Truy van FEFO co khoa ghi
     * nen khong danh dau readOnly.
     */
    public IssueSuggestion suggestIssue(Long id) {
        StockIssue issue = getIssue(id);
        Map<Ingredient, BigDecimal> need = new LinkedHashMap<>();
        List<String> noRecipe = new ArrayList<>();
        for (StockIssuePlan plan : issue.getPlans()) {
            var recipe = recipes.activeRecipe(plan.getMenuItem().getId());
            if (recipe.isEmpty()) {
                noRecipe.add(plan.getMenuItem().getName());
                continue;
            }
            for (RecipeDetail d : recipe.get().getDetails()) {
                BigDecimal q = d.getQtyPerPortion().multiply(BigDecimal.valueOf(plan.getPlannedPortions()));
                need.merge(d.getIngredient(), q, BigDecimal::add);
            }
        }
        LocalDate today = Clock.today();
        List<SuggestedLine> lines = new ArrayList<>();
        List<Shortage> shortages = new ArrayList<>();
        need.forEach((ing, qty) -> {
            BigDecimal remaining = qty;
            for (StockLot lot : lots.findUsableFefoForUpdate(ing.getId(), today)) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal take = remaining.min(lot.getRemainingQty());
                lines.add(new SuggestedLine(lot.getId(), lot.getCode(), ing.getId(), ing.getName(), ing.getUnit(),
                        lot.getExpiryDate(), lot.getRemainingQty(), take));
                remaining = remaining.subtract(take);
            }
            if (remaining.signum() > 0) {
                shortages.add(new Shortage(ing.getId(), ing.getName(), ing.getUnit(), remaining));
            }
        });
        return new IssueSuggestion(lines, shortages, noRecipe);
    }

    /**
     * Xac nhan phieu xuat: khoa tung lo, tru ton (BR34, BR35), ghi giao dich OUT; ton bi tru tai day,
     * khong tru theo don ban (GD5, BR36).
     */
    public IssueView confirmIssue(Long id, List<IssueLineInput> lines, CurrentUser actor) {
        StockIssue issue = issues.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("phiếu xuất", id));
        if (lines == null || lines.isEmpty()) {
            throw new BusinessRuleException("BR33", "Phiếu xuất chưa có dòng nào");
        }
        issue.clearLines();
        LocalDate today = Clock.today();
        Map<StockLot, BigDecimal> perLot = new LinkedHashMap<>();
        for (IssueLineInput l : lines) {
            StockLot lot = lots.findByIdForUpdate(l.lotId()).orElseThrow(() -> new NotFoundException("lô", l.lotId()));
            lot.deduct(l.issuedQty(), today);
            issue.addLine(lot, l.suggestedQty(), l.issuedQty(), l.outsideRecipe());
            perLot.merge(lot, l.issuedQty(), BigDecimal::add);
        }
        issue.confirm();
        User by = actors.load(actor);
        perLot.forEach((lot, qty) -> transactions.save(InventoryTransaction.out(lot, qty, issue.getId(), by)));
        return IssueView.of(issue);
    }

    public IssueView cancelIssue(Long id) {
        StockIssue i = issues.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("phiếu xuất", id));
        i.cancel();
        return IssueView.of(i);
    }

    private void setPlans(StockIssue i, List<PlanInput> plans) {
        for (PlanInput p : plans) {
            MenuItem item = menuItems.findById(p.menuItemId())
                    .orElseThrow(() -> new NotFoundException("món", p.menuItemId()));
            i.setPlan(item, p.plannedPortions());
        }
    }

    private StockIssue getIssue(Long id) {
        return issues.findById(id).orElseThrow(() -> new NotFoundException("phiếu xuất", id));
    }

    // ------------------------------------------------------------ UC4.3 ton kho, canh bao, dieu chinh

    /** Ton theo nguyen lieu kem canh bao ton thap va sap het han (BR38). */
    @Transactional(readOnly = true)
    public List<StockLevel> stockLevels() {
        LocalDate today = Clock.today();
        Map<Long, List<StockLot>> byIngredient = new LinkedHashMap<>();
        for (StockLot l : lots.findInStock()) {
            byIngredient.computeIfAbsent(l.getIngredient().getId(), k -> new ArrayList<>()).add(l);
        }
        List<StockLevel> result = new ArrayList<>();
        for (Ingredient ing : ingredientRepo.findAllByOrderByGroupNameAscNameAsc()) {
            List<StockLot> ls = byIngredient.getOrDefault(ing.getId(), List.of());
            BigDecimal available = ls.stream().filter(l -> !l.isExpired(today)).map(StockLot::getRemainingQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expired = ls.stream().filter(l -> l.isExpired(today)).map(StockLot::getRemainingQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int near = (int) ls.stream().filter(l -> l.isNearExpiry(today, expiryWarningDays)).count();
            result.add(new StockLevel(ing.getId(), ing.getCode(), ing.getName(), ing.getGroupName(), ing.getUnit(),
                    available, expired, ing.getMinStock(), available.compareTo(ing.getMinStock()) <= 0, near));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<LotView> lotsInStock() {
        LocalDate today = Clock.today();
        return lots.findInStock().stream().map(l -> new LotView(l.getId(), l.getCode(), l.getIngredient().getId(),
                l.getIngredient().getName(), l.getIngredient().getUnit(), l.getReceivedDate(), l.getExpiryDate(),
                l.getRemainingQty(), l.isExpired(today), l.isNearExpiry(today, expiryWarningDays))).toList();
    }

    /** BR37: chi MANAGER (kiem tra o tang api), bat buoc ly do, ghi giao dich ADJUST va nhat ky. */
    public AdjustmentView adjust(Long lotId, BigDecimal actualQty, String reason, CurrentUser actor) {
        StockLot lot = lots.findByIdForUpdate(lotId).orElseThrow(() -> new NotFoundException("lô", lotId));
        User by = actors.load(actor);
        BigDecimal before = lot.getRemainingQty();
        StockAdjustment adj = adjustments.saveAndFlush(StockAdjustment.apply(
                codes.next("DC", 3, adjustments::findMaxCodeWithPrefix), lot, actualQty, reason, by));
        transactions.save(InventoryTransaction.adjust(adj, by));
        audit.log(by, "STOCK_ADJUST", "stock_lots", lotId, Map.of("remainingQty", before),
                Map.of("remainingQty", actualQty, "reason", adj.getReason(), "adjustment", adj.getCode()));
        return AdjustmentView.of(adj);
    }

    @Transactional(readOnly = true)
    public List<AdjustmentView> adjustments() {
        return adjustments.findTop200ByOrderByIdDesc().stream().map(AdjustmentView::of).toList();
    }
}
