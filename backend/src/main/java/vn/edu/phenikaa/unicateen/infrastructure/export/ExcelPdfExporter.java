package vn.edu.phenikaa.unicateen.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter;

/** Xuat bao cao ra Excel (Apache POI) hoac PDF (OpenPDF). */
@Component
public class ExcelPdfExporter implements IReportExporter {

    @Override
    public byte[] export(ReportTable table, Format format) {
        try {
            return format == Format.XLSX ? xlsx(table) : pdf(table);
        } catch (IOException e) {
            throw new IllegalStateException("Không xuất được báo cáo", e);
        }
    }

    private byte[] xlsx(ReportTable t) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bao cao");
            CellStyle bold = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font f = wb.createFont();
            f.setBold(true);
            bold.setFont(f);
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue(t.title());
            title.getCell(0).setCellStyle(bold);
            Row head = sheet.createRow(2);
            for (int i = 0; i < t.headers().size(); i++) {
                Cell c = head.createCell(i);
                c.setCellValue(t.headers().get(i));
                c.setCellStyle(bold);
            }
            int r = 3;
            for (List<Object> row : t.rows()) {
                Row xr = sheet.createRow(r++);
                for (int i = 0; i < row.size(); i++) {
                    Object v = row.get(i);
                    Cell c = xr.createCell(i);
                    if (v instanceof Number n) {
                        c.setCellValue(n.doubleValue());
                    } else {
                        c.setCellValue(v == null ? "" : v.toString());
                    }
                }
            }
            for (int i = 0; i < t.headers().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] pdf(ReportTable t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font normal = vietnameseFont(10, Font.NORMAL);
        Font bold = vietnameseFont(10, Font.BOLD);
        doc.add(new Paragraph(t.title(), vietnameseFont(14, Font.BOLD)));
        doc.add(new Paragraph(" "));
        PdfPTable pt = new PdfPTable(t.headers().size());
        pt.setWidthPercentage(100);
        for (String h : t.headers()) {
            pt.addCell(new PdfPCell(new Phrase(h, bold)));
        }
        for (List<Object> row : t.rows()) {
            for (Object v : row) {
                pt.addCell(new PdfPCell(new Phrase(v == null ? "" : v.toString(), normal)));
            }
        }
        doc.add(pt);
        doc.close();
        return out.toByteArray();
    }

    /** Dung font he thong co dau tieng Viet neu co (DejaVu, Liberation, Arial); neu khong dung Helvetica. */
    private static Font vietnameseFont(float size, int style) {
        String[] candidates = {
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/Library/Fonts/Arial.ttf"
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) {
                return FontFactory.getFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style);
            }
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }
}
