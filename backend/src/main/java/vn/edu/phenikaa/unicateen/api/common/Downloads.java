package vn.edu.phenikaa.unicateen.api.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import vn.edu.phenikaa.unicateen.application.port.IReportExporter;

/** Tra tep bao cao ve trinh duyet. */
public final class Downloads {
    private Downloads() {
    }

    public static ResponseEntity<byte[]> file(byte[] data, String baseName, IReportExporter.Format format) {
        boolean xlsx = format == IReportExporter.Format.XLSX;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + baseName + (xlsx ? ".xlsx" : ".pdf") + "\"")
                .contentType(xlsx
                        ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        : MediaType.APPLICATION_PDF)
                .body(data);
    }
}
