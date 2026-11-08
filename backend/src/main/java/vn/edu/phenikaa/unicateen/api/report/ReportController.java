package vn.edu.phenikaa.unicateen.api.report;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.api.common.Downloads;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter.Format;
import vn.edu.phenikaa.unicateen.application.report.RevenueReportService;
import vn.edu.phenikaa.unicateen.application.report.RevenueReportService.RevenueReport;
import vn.edu.phenikaa.unicateen.application.report.StockReportService;
import vn.edu.phenikaa.unicateen.application.report.StockReportService.StockRow;

/** UC5.4 Bao cao doanh thu (MANAGER), UC5.5 Bao cao ton kho (MANAGER, INVENTORY_STAFF). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final RevenueReportService revenue;
    private final StockReportService stock;

    public ReportController(RevenueReportService revenue, StockReportService stock) {
        this.revenue = revenue;
        this.stock = stock;
    }

    @GetMapping("/revenue")
    public RevenueReport revenue(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return revenue.revenue(from, to);
    }

    @GetMapping("/revenue/export")
    public ResponseEntity<byte[]> revenueExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "XLSX") Format format) {
        return Downloads.file(revenue.export(from, to, format), "doanh-thu_" + from + "_" + to, format);
    }

    @GetMapping("/stock")
    public List<StockRow> stock(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stock.stock(from, to);
    }

    @GetMapping("/stock/export")
    public ResponseEntity<byte[]> stockExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "XLSX") Format format) {
        return Downloads.file(stock.export(from, to, format), "nhap-xuat-ton_" + from + "_" + to, format);
    }
}
