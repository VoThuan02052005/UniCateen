package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;

/** Xuat bao cao ra tep (UC5.4, UC5.5). */
public interface IReportExporter {
    byte[] export(ReportTable table, Format format);

    enum Format {
        XLSX, PDF
    }

    record ReportTable(String title, List<String> headers, List<List<Object>> rows) {
    }
}
