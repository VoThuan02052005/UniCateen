package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;

/** Nha cung cap (UC4.1). Da co phieu nhap thi chi Ngung giao dich, khong xoa (BR30). */
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "tax_code", length = 14)
    private String taxCode;
    @Column(name = "contact_name", length = 100)
    private String contactName;
    @Column(nullable = false, length = 10)
    private String phone;
    @Column(length = 100)
    private String email;
    private String address;
    @Column(name = "main_goods", length = 100)
    private String mainGoods;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;

    protected Supplier() {
    }

    public Supplier(String code, String name, String taxCode, String contactName, String phone, String email,
                    String address, String mainGoods) {
        this.code = code;
        this.status = ActiveStatus.ACTIVE;
        update(name, taxCode, contactName, phone, email, address, mainGoods);
    }

    public void update(String name, String taxCode, String contactName, String phone, String email,
                       String address, String mainGoods) {
        this.name = name.trim();
        this.taxCode = (taxCode == null || taxCode.isBlank()) ? null : taxCode.trim();
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.mainGoods = mainGoods;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == ActiveStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTaxCode() { return taxCode; }
    public String getContactName() { return contactName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getMainGoods() { return mainGoods; }
    public ActiveStatus getStatus() { return status; }
}
