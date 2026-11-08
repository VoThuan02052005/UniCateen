package vn.edu.phenikaa.unicateen.application.inventory;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.common.Text;
import vn.edu.phenikaa.unicateen.application.port.StockReceiptRepository;
import vn.edu.phenikaa.unicateen.application.port.SupplierRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.model.Supplier;

/** UC4.1 Quan ly nha cung cap (BR30). */
@Service
@Transactional
public class SupplierService {
    private final SupplierRepository suppliers;
    private final StockReceiptRepository receipts;

    public SupplierService(SupplierRepository suppliers, StockReceiptRepository receipts) {
        this.suppliers = suppliers;
        this.receipts = receipts;
    }

    public record SupplierInput(String name, String taxCode, String contactName, String phone, String email,
                                String address, String mainGoods) {
    }

    public record SupplierView(Long id, String code, String name, String taxCode, String contactName, String phone,
                               String email, String address, String mainGoods, ActiveStatus status) {
        static SupplierView of(Supplier s) {
            return new SupplierView(s.getId(), s.getCode(), s.getName(), s.getTaxCode(), s.getContactName(),
                    s.getPhone(), s.getEmail(), s.getAddress(), s.getMainGoods(), s.getStatus());
        }
    }

    @Transactional(readOnly = true)
    public List<SupplierView> list() {
        return suppliers.findAllByOrderByNameAsc().stream().map(SupplierView::of).toList();
    }

    public SupplierView create(SupplierInput in) {
        String tax = Text.blankToNull(in.taxCode());
        if (tax != null && suppliers.existsByTaxCode(tax)) {
            throw new BusinessRuleException("BR30", "Mã số thuế đã tồn tại");
        }
        List<String> codes = suppliers.findAllByOrderByNameAsc().stream().map(Supplier::getCode).toList();
        Supplier s = new Supplier(CodeGenerator.sequential("NCC", 3, codes), in.name(), tax, in.contactName(),
                in.phone(), in.email(), in.address(), in.mainGoods());
        return SupplierView.of(suppliers.save(s));
    }

    public SupplierView update(Long id, SupplierInput in) {
        Supplier s = get(id);
        String tax = Text.blankToNull(in.taxCode());
        if (tax != null && suppliers.existsByTaxCodeAndIdNot(tax, id)) {
            throw new BusinessRuleException("BR30", "Mã số thuế đã tồn tại");
        }
        s.update(in.name(), tax, in.contactName(), in.phone(), in.email(), in.address(), in.mainGoods());
        return SupplierView.of(s);
    }

    public SupplierView setStatus(Long id, ActiveStatus status) {
        Supplier s = get(id);
        s.setStatus(status);
        return SupplierView.of(s);
    }

    /** BR30: nha cung cap da co phieu nhap chi duoc Ngung giao dich. */
    public void delete(Long id) {
        Supplier s = get(id);
        if (receipts.existsBySupplierId(id)) {
            throw new BusinessRuleException("BR30", "Nhà cung cấp đã có phiếu nhập, chỉ được chuyển Ngừng giao dịch");
        }
        suppliers.delete(s);
    }

    Supplier get(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("nhà cung cấp", id));
    }
}
