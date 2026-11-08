package vn.edu.phenikaa.unicateen.application.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter.ReportTable;
import vn.edu.phenikaa.unicateen.application.port.IngredientQty;
import vn.edu.phenikaa.unicateen.application.port.IngredientRepository;
import vn.edu.phenikaa.unicateen.application.port.InventoryTransactionRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.model.Ingredient;
import vn.edu.phenikaa.unicateen.domain.model.InventoryTransaction;

/** UC5.5 Bao cao nhap - xuat - ton theo nguyen lieu, tinh tu inventory_transactions (BR33, BR46). */
@Service
@Transactional(readOnly = true)
public class StockReportService {
    private final InventoryTransactionRepository transactions;
    private final IngredientRepository ingredients;
    private final IReportExporter exporter;

    public StockReportService(InventoryTransactionRepository transactions, IngredientRepository ingredients,
                              IReportExporter exporter) {
        this.transactions = transactions;
        this.ingredients = ingredients;
        this.exporter = exporter;
    }

    public record StockRow(String code, String name, String unit, BigDecimal opening, BigDecimal in, BigDecimal out,
                           BigDecimal adjust, BigDecimal closing) {
    }

    public List<StockRow> stock(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("DV05", "Ngày kết thúc trước ngày bắt đầu");
        }
        Map<Long, BigDecimal> opening = new HashMap<>();
        for (IngredientQty q : transactions.sumByIngredientBefore(from.atStartOfDay())) {
            opening.put(q.ingredientId(), q.quantity());
        }
        Map<Long, BigDecimal[]> moves = new HashMap<>();          // [in, out, adjust]
        for (InventoryTransaction t : transactions.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            BigDecimal[] m = moves.computeIfAbsent(t.getLot().getIngredient().getId(),
                    k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            int idx = switch (t.getType()) {
                case IN -> 0;
                case OUT -> 1;
                case ADJUST -> 2;
            };
            m[idx] = m[idx].add(t.getQuantity());
        }
        List<StockRow> rows = new ArrayList<>();
        for (Ingredient i : ingredients.findAllByOrderByGroupNameAscNameAsc()) {
            BigDecimal o = opening.getOrDefault(i.getId(), BigDecimal.ZERO);
            BigDecimal[] m = moves.getOrDefault(i.getId(),
                    new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            // xuat luu so am; hien thi so duong
            rows.add(new StockRow(i.getCode(), i.getName(), i.getUnit(), o, m[0], m[1].negate(), m[2],
                    o.add(m[0]).add(m[1]).add(m[2])));
        }
        return rows;
    }

    public byte[] export(LocalDate from, LocalDate to, IReportExporter.Format format) {
        List<List<Object>> rows = stock(from, to).stream().map(r -> List.<Object>of(r.code(), r.name(), r.unit(),
                r.opening(), r.in(), r.out(), r.adjust(), r.closing())).toList();
        return exporter.export(new ReportTable("Báo cáo nhập - xuất - tồn từ " + from + " đến " + to,
                List.of("Mã", "Nguyên liệu", "ĐVT", "Tồn đầu", "Nhập", "Xuất", "Điều chỉnh", "Tồn cuối"), rows),
                format);
    }
}
