package vn.edu.phenikaa.unicateen.application.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter.ReportTable;
import vn.edu.phenikaa.unicateen.application.port.PaymentRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;
import vn.edu.phenikaa.unicateen.domain.model.OrderDetail;
import vn.edu.phenikaa.unicateen.domain.model.Payment;

/**
 * UC5.4 Bao cao doanh thu: chi tinh giao dich SUCCESS, theo ngay cua thoi diem thanh cong (BR29);
 * chi doc du lieu (BR46).
 */
@Service
@Transactional(readOnly = true)
public class RevenueReportService {
    private final PaymentRepository payments;
    private final IReportExporter exporter;

    public RevenueReportService(PaymentRepository payments, IReportExporter exporter) {
        this.payments = payments;
        this.exporter = exporter;
    }

    public record Amount(String key, long orders, long revenue) {
    }

    public record ItemAmount(String category, String item, long quantity, long revenue) {
    }

    public record RevenueReport(LocalDate from, LocalDate to, long totalOrders, long totalRevenue,
                                List<Amount> byDay, List<Amount> byMethod, List<Amount> byCategory,
                                List<ItemAmount> byItem) {
    }

    public RevenueReport revenue(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("DV05", "Ngày kết thúc trước ngày bắt đầu");
        }
        List<Payment> success = payments.findByStatusAndTransactionTimeGreaterThanEqualAndTransactionTimeLessThan(
                PaymentStatus.SUCCESS, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<String, long[]> day = new TreeMap<>();
        Map<String, long[]> method = new TreeMap<>();
        Map<String, long[]> category = new TreeMap<>();
        Map<String, ItemAcc> item = new TreeMap<>();
        long total = 0;
        for (Payment p : success) {
            total += p.getAmount();
            add(day, p.getTransactionTime().toLocalDate().toString(), p.getAmount());
            add(method, label(p.getMethod()), p.getAmount());
            for (OrderDetail d : p.getOrder().getDetails()) {
                String cat = d.getMenuItem().getCategory().getName();
                add(category, cat, d.getLineTotal());
                item.computeIfAbsent(cat + "|" + d.getMenuItem().getName(),
                        k -> new ItemAcc(cat, d.getMenuItem().getName())).add(d.getQuantity(), d.getLineTotal());
            }
        }
        return new RevenueReport(from, to, success.size(), total, list(day), list(method), list(category),
                item.values().stream().map(a -> new ItemAmount(a.category, a.item, a.qty, a.revenue))
                        .sorted((a, b) -> Long.compare(b.revenue(), a.revenue())).toList());
    }

    public byte[] export(LocalDate from, LocalDate to, IReportExporter.Format format) {
        RevenueReport r = revenue(from, to);
        List<List<Object>> rows = new ArrayList<>();
        r.byDay().forEach(a -> rows.add(List.of("Ngày " + a.key(), a.orders(), a.revenue())));
        r.byMethod().forEach(a -> rows.add(List.of("Hình thức: " + a.key(), a.orders(), a.revenue())));
        r.byItem().forEach(a -> rows.add(List.of("Món: " + a.item() + " (" + a.category() + ")", a.quantity(),
                a.revenue())));
        rows.add(List.of("Tổng cộng", r.totalOrders(), r.totalRevenue()));
        return exporter.export(new ReportTable("Báo cáo doanh thu từ " + from + " đến " + to,
                List.of("Nhóm", "Số đơn / số lượng", "Doanh thu (đồng)"), rows), format);
    }

    private static String label(PaymentMethod m) {
        return switch (m) {
            case STUDENT_FACULTY_CARD -> "Thẻ SV/GV";
            case CASH -> "Tiền mặt";
            case BANK_TRANSFER -> "Chuyển khoản";
        };
    }

    private static void add(Map<String, long[]> m, String key, long amount) {
        long[] acc = m.computeIfAbsent(key, k -> new long[2]);
        acc[0]++;
        acc[1] += amount;
    }

    private static List<Amount> list(Map<String, long[]> m) {
        return m.entrySet().stream().map(e -> new Amount(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    private static final class ItemAcc {
        final String category;
        final String item;
        long qty;
        long revenue;

        ItemAcc(String category, String item) {
            this.category = category;
            this.item = item;
        }

        void add(int q, long r) {
            qty += q;
            revenue += r;
        }
    }
}
