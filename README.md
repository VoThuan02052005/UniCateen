# UniCateen – Hệ thống quản lý canteen trường học

Mã nguồn của đề tài học phần **Phân tích và thiết kế phần mềm** (Nhóm 10, Trường Công nghệ Thông tin, Đại học Phenikaa).
Mã được viết theo báo cáo thiết kế: mọi lớp, bảng, API và ca kiểm thử đều truy vết được về use case (UC), yêu cầu (R, NFR)
và quy tắc nghiệp vụ (BR).

| Phần | Công nghệ |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring Data JPA, Spring Security, Flyway, Maven |
| Frontend | React 18 (SPA), Vite 5 |
| Cơ sở dữ liệu | MySQL 8 (InnoDB, utf8mb4), 21 bảng |
| Kiểm thử | JUnit 5, Spring Boot Test, MockMvc, Testcontainers (MySQL thật) |

## Demo trên GitHub Codespaces (không cần cài gì trên máy)

1. Trên trang repo GitHub: **Code → Codespaces → Create codespace on main**. Lần đầu mất vài phút để cài JDK 17, Maven, Node, Docker.
2. Trong terminal của Codespace: `./demo.sh`. Khi thấy "Backend đã chạy", mở tab **PORTS** → cổng **5173** → biểu tượng quả địa cầu.
3. Chạy toàn bộ kiểm thử: `./test.sh` (kết quả ở `test_log.txt` và `backend/target/surefire-reports/`).

## Chạy thử nhanh trên máy (dữ liệu mẫu)

Cần: JDK 17+, Maven 3.9+, Node.js 18+, Docker.

```bash
# 1. MySQL
docker compose up -d

# 2. Backend, profile demo nạp dữ liệu mẫu (V3__seed_demo.sql)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo

# 3. Frontend (cửa sổ khác)
cd frontend
npm install
npm run dev          # mở http://localhost:5173
```

Tài khoản mẫu, mật khẩu chung `Demo@2026`:

| Email | Vai trò |
|---|---|
| admin@phenikaa-uni.edu.vn | Quản trị viên |
| quanly@phenikaa-uni.edu.vn | Quản lý canteen |
| banhang1@phenikaa-uni.edu.vn, banhang2@… | Nhân viên bán hàng |
| kho@phenikaa-uni.edu.vn | Nhân viên kho |
| bep@phenikaa-uni.edu.vn | Nhân viên chế biến |
| 23010427@st.phenikaa-uni.edu.vn (thẻ `SV23010427`) | Sinh viên |
| giangvien@phenikaa-uni.edu.vn (thẻ `GV0001`) | Giảng viên |

Dịch vụ thẻ là bản mô phỏng (GD2): mã thẻ kết thúc bằng `DECLINE` bị từ chối (ví dụ `SV23010062DECLINE`),
kết thúc bằng `SLOW` thì quá thời gian chờ; mã khác được chấp nhận nếu đã liên kết với tài khoản sinh viên, giảng viên.

## Đóng gói một tệp JAR

```bash
cd frontend && npm install && npm run build
cd ../backend && mvn -Pwith-frontend package
java -jar target/unicateen-1.0.0.jar     # biến môi trường DB_URL, DB_USER, DB_PASSWORD, COOKIE_SECURE=true khi chạy HTTPS
```

## Kiểm thử

```bash
cd backend
mvn test        # cần Docker đang chạy cho các test tích hợp (Testcontainers MySQL 8)
```

- `domain/*Test`: máy trạng thái đơn hàng, giao dịch thanh toán, chuẩn bị món, quy tắc kho, tài khoản (không cần CSDL).
- `integration/PaymentIntegrationTest`: BR20 (hai yêu cầu song song chỉ một thành công, UNIQUE `active_order_id`),
  idempotency (NFR06), rollback khi tạo trạng thái chuẩn bị lỗi (BR22), thẻ quá 1 phút thành FAILED (BR27), chuyển khoản.
- `integration/InventoryIntegrationTest`: FEFO (BR34), không xuất vượt tồn (BR35), điều chỉnh có lý do (BR37),
  sổ kho khớp tồn lô (`SUM(quantity) = remaining_qty`).
- `integration/SecurityIntegrationTest`: phân quyền khi gọi trực tiếp API (NFR04), quyền sở hữu dữ liệu (BR17),
  khóa đăng nhập sau 5 lần sai (BR43), mã lỗi 409 theo quy tắc nghiệp vụ.

## Cấu trúc

```
backend/src/main/java/vn/edu/phenikaa/unicateen/
  api/              Controller REST + DTO yêu cầu, xử lý lỗi (GlobalExceptionHandler)
  application/      Service theo 5 nhóm chức năng; port/ chứa giao diện repository và dịch vụ ngoài
  domain/           21 entity với máy trạng thái, enum, BusinessRuleException
  infrastructure/   Cài đặt port: JPA, dịch vụ thẻ mô phỏng, bcrypt, nhật ký, xuất Excel/PDF, SecurityConfig
backend/src/main/resources/db/migration   V1__schema.sql (21 bảng), V2__constraints.sql (khóa, UNIQUE, CHECK, cột sinh)
backend/src/main/resources/db/demo        V3__seed_demo.sql (chỉ chạy với profile demo)
frontend/src/pages                        pos, catalog, stock, kitchen, admin, report
```

Phụ thuộc đi vào trong: `api → application → domain`; `infrastructure` cài đặt các giao diện của `application.port`.

## Truy vết use case → mã nguồn

| Use case | API | Service |
|---|---|---|
| UC1.1 Danh mục món | `/api/catalog/categories` | CategoryService |
| UC1.2 Món ăn, đồ uống | `/api/catalog/menu-items` | MenuItemService |
| UC1.3 Cập nhật giá | `/api/catalog/menu-items/{id}/prices` | PriceService |
| UC1.4 Nguyên liệu | `/api/catalog/ingredients` | IngredientService |
| UC1.5 Công thức, định mức | `/api/catalog/recipes/{menuItemId}` | RecipeService |
| UC2.1–UC2.3 Tạo, sửa, hủy đơn | `/api/orders`, `/api/orders/{code}/…` | OrderService |
| UC2.4 Tra cứu đơn | `GET /api/orders` | OrderService.search |
| UC2.5 Lịch sử mua hàng | `GET /api/orders/mine` | OrderService.myHistory |
| UC3.1 Thanh toán thẻ | `POST /api/payments/card` | PaymentService.payCard |
| UC3.2 Tiền mặt | `POST /api/payments/cash` | PaymentService.payCash |
| UC3.3, UC3.4 Chuyển khoản | `POST /api/payments/transfer`, `/{code}/confirm`, `/{code}/fail` | PaymentService |
| UC3.5 Tra cứu giao dịch | `GET /api/payments` | PaymentService.search |
| UC4.1 Nhà cung cấp | `/api/stock/suppliers` | SupplierService |
| UC4.2 Nhập kho | `/api/stock/receipts` | InventoryService |
| UC4.3 Tồn kho, điều chỉnh | `/api/stock/levels`, `/lots`, `/adjustments` | InventoryService |
| UC4.4 Xuất nguyên liệu | `/api/stock/issues`, `/{id}/suggestion`, `/{id}/confirm` | InventoryService |
| UC4.5 Chuẩn bị món | `/api/kitchen/preparations` | KitchenService |
| UC5.1 Đăng nhập | `/api/auth/login`, `/logout`, `/password` | AuthService |
| UC5.2, UC5.3 Tài khoản, phân quyền | `/api/accounts` | AccountService |
| UC5.4 Báo cáo doanh thu | `/api/reports/revenue` | RevenueReportService |
| UC5.5 Báo cáo tồn kho | `/api/reports/stock` | StockReportService |

## Ghi chú thiết kế

- **Thanh toán**: mỗi đơn có tối đa một giao dịch PENDING hoặc SUCCESS (BR20), bảo đảm bằng khóa ghi trên đơn và
  cột sinh `active_order_id` có UNIQUE. Thanh toán thành công, chuyển đơn PAID và tạo trạng thái chuẩn bị nằm trong
  một giao dịch CSDL (BR22). Mỗi yêu cầu thanh toán gửi header `Idempotency-Key` (NFR06).
- **Thẻ**: giao dịch PENDING được commit trước khi gọi dịch vụ thẻ; tác vụ định kỳ 30 giây chuyển giao dịch thẻ
  PENDING quá 1 phút sang FAILED (BR27).
- **Kho**: tồn chỉ đổi qua `inventory_transactions` (IN, OUT, ADJUST), `quantity` mang dấu; tồn bị trừ khi xác nhận
  phiếu xuất theo ca, không trừ theo đơn bán (GD5).
- **Mã chứng từ**: `DH-yymmdd-nnnn`, `GD-…`, `PN-yymmdd-nnn`, `L-…`, `PX-…`, `DC-…`.
- Thay đổi CSDL chỉ qua tệp Flyway mới (`V4__…sql`), không sửa tệp đã chạy.
