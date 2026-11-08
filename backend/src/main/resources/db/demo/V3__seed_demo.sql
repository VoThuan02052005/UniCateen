-- UniCateen V3: du lieu mau cho moi truong thu (chi chay voi profile "demo").
-- Mat khau cua cac tai khoan mau duoc DemoPasswordInitializer dat thanh "Demo@2026" khi khoi dong;
-- gia tri '{demo}' duoi day khong phai mat khau that.

INSERT INTO users (id, email, full_name, phone, role, card_code, password_hash, must_change_password, created_at) VALUES
 (1, 'admin@phenikaa-uni.edu.vn',     'Quản trị hệ thống',  NULL,         'ADMIN',           NULL,         '{demo}', FALSE, NOW()),
 (2, 'quanly@phenikaa-uni.edu.vn',    'Trần Thị Quản Lý',   '0912000001', 'MANAGER',         NULL,         '{demo}', FALSE, NOW()),
 (3, 'banhang1@phenikaa-uni.edu.vn',  'Lê Văn Bán Hàng',    '0912000002', 'CASHIER',         NULL,         '{demo}', FALSE, NOW()),
 (4, 'banhang2@phenikaa-uni.edu.vn',  'Phạm Thu Hà',        '0912000003', 'CASHIER',         NULL,         '{demo}', FALSE, NOW()),
 (5, 'kho@phenikaa-uni.edu.vn',       'Nguyễn Văn Kho',     '0912000004', 'INVENTORY_STAFF', NULL,         '{demo}', FALSE, NOW()),
 (6, 'bep@phenikaa-uni.edu.vn',       'Đỗ Thị Bếp',         '0912000005', 'KITCHEN_STAFF',   NULL,         '{demo}', FALSE, NOW()),
 (7, '23010427@st.phenikaa-uni.edu.vn','Võ Hữu Thuận',      NULL,         'STUDENT',         'SV23010427', '{demo}', FALSE, NOW()),
 (8, '23010320@st.phenikaa-uni.edu.vn','Nguyễn Văn Vũ',     NULL,         'STUDENT',         'SV23010320', '{demo}', FALSE, NOW()),
 (9, 'giangvien@phenikaa-uni.edu.vn', 'Giảng Viên Mẫu',     NULL,         'FACULTY',         'GV0001',     '{demo}', FALSE, NOW()),
 (10,'23010062@st.phenikaa-uni.edu.vn','Vũ Đức Toàn',       NULL,         'STUDENT',         'SV23010062DECLINE', '{demo}', FALSE, NOW());

INSERT INTO categories (id, code, name, display_order, description) VALUES
 (1, 'DM01', 'Cơm',      1, 'Các món cơm phần'),
 (2, 'DM02', 'Bún, phở', 2, 'Món nước'),
 (3, 'DM03', 'Đồ uống',  3, 'Nước đóng chai, pha chế'),
 (4, 'DM04', 'Ăn vặt',   4, NULL);

INSERT INTO menu_items (id, code, category_id, name, unit, status, created_at) VALUES
 (1, 'M001', 1, 'Cơm gà xối mỡ',     'phần', 'ON_SALE', NOW()),
 (2, 'M002', 1, 'Cơm sườn nướng',    'phần', 'ON_SALE', NOW()),
 (3, 'M003', 2, 'Phở bò',            'bát',  'ON_SALE', NOW()),
 (4, 'M004', 2, 'Bún chả',           'suất', 'ON_SALE', NOW()),
 (5, 'M005', 3, 'Trà đá',            'cốc',  'ON_SALE', NOW()),
 (6, 'M006', 3, 'Nước suối 500ml',   'chai', 'ON_SALE', NOW()),
 (7, 'M007', 3, 'Cà phê sữa đá',     'cốc',  'ON_SALE', NOW()),
 (8, 'M008', 4, 'Bánh mì trứng',     'cái',  'OUT_OF_STOCK', NOW());

INSERT INTO item_prices (menu_item_id, price, effective_from, reason, status, created_by) VALUES
 (1, 35000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (2, 40000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (3, 40000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (4, 35000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (5,  3000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (6,  8000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (7, 20000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2),
 (8, 15000, NOW() - INTERVAL 30 DAY, 'Giá ban đầu', 'ACTIVE', 2);

INSERT INTO ingredients (id, code, name, group_name, unit, min_stock, track_expiry, storage_note) VALUES
 (1, 'NL001', 'Gạo tẻ',        'Gạo, ngũ cốc', 'kg',  20.000, FALSE, 'Kho khô'),
 (2, 'NL002', 'Đùi gà',        'Thịt, cá',     'kg',  10.000, TRUE,  'Ngăn đông'),
 (3, 'NL003', 'Sườn heo',      'Thịt, cá',     'kg',   8.000, TRUE,  'Ngăn đông'),
 (4, 'NL004', 'Bánh phở',      'Gạo, ngũ cốc', 'kg',   5.000, TRUE,  'Ngăn mát'),
 (5, 'NL005', 'Thịt bò',       'Thịt, cá',     'kg',   5.000, TRUE,  'Ngăn đông'),
 (6, 'NL006', 'Nước suối 500ml','Đồ uống',     'chai', 24.000, TRUE, NULL),
 (7, 'NL007', 'Cà phê bột',    'Đồ uống',      'kg',   1.000, TRUE,  NULL);

-- Cong thuc dang ap dung (BR09); nuoc suoi ban nguyen chai, khong can cong thuc
INSERT INTO recipes (id, menu_item_id, version, status, note, created_at) VALUES
 (1, 1, 1, 'ACTIVE', 'Gà luộc sơ rồi chiên ngập dầu', NOW()),
 (2, 2, 1, 'ACTIVE', NULL, NOW()),
 (3, 3, 1, 'ACTIVE', NULL, NOW());
INSERT INTO recipe_details (recipe_id, ingredient_id, qty_per_portion) VALUES
 (1, 1, 0.150), (1, 2, 0.200),
 (2, 1, 0.150), (2, 3, 0.180),
 (3, 4, 0.150), (3, 5, 0.080);

INSERT INTO suppliers (id, code, name, tax_code, contact_name, phone, email, address, main_goods) VALUES
 (1, 'NCC001', 'Công ty Thực phẩm Hà Đông', '0101234567', 'Chị Lan', '0987000111', 'lan@thucphamhd.vn', 'Hà Đông, Hà Nội', 'Thịt, trứng'),
 (2, 'NCC002', 'Đại lý Gạo Phú Lãm',        NULL,          'Anh Minh','0987000222', NULL, 'Phú Lãm, Hà Đông', 'Gạo');

-- Mot phieu nhap da xac nhan va cac lo, giao dich IN tuong ung (BR33, GD6)
INSERT INTO stock_receipts (id, code, supplier_id, receipt_date, status, total_value, created_by, confirmed_at) VALUES
 (1, CONCAT('PN-', DATE_FORMAT(CURRENT_DATE - INTERVAL 2 DAY, '%y%m%d'), '-001'), 1, CURRENT_DATE - INTERVAL 2 DAY,
  'CONFIRMED', 6790000, 5, NOW() - INTERVAL 2 DAY);
INSERT INTO stock_receipt_details (id, receipt_id, ingredient_id, quantity, unit_cost, expiry_date, line_total) VALUES
 (1, 1, 1, 50.000,  18000, NULL,                         900000),
 (2, 1, 2, 30.000,  75000, CURRENT_DATE + INTERVAL 2 DAY, 2250000),
 (3, 1, 3, 20.000, 110000, CURRENT_DATE + INTERVAL 10 DAY, 2200000),
 (4, 1, 4, 10.000,  20000, CURRENT_DATE + INTERVAL 1 DAY, 200000),
 (5, 1, 5,  4.000, 250000, CURRENT_DATE + INTERVAL 5 DAY, 1000000),
 (6, 1, 6, 48.000,   5000, CURRENT_DATE + INTERVAL 180 DAY, 240000);
INSERT INTO stock_lots (id, code, ingredient_id, receipt_detail_id, received_date, expiry_date, unit_cost, remaining_qty)
SELECT d.id, CONCAT('L-', DATE_FORMAT(CURRENT_DATE - INTERVAL 2 DAY, '%y%m%d'), '-', LPAD(d.id, 3, '0')),
       d.ingredient_id, d.id, CURRENT_DATE - INTERVAL 2 DAY, d.expiry_date, d.unit_cost, d.quantity
FROM stock_receipt_details d WHERE d.receipt_id = 1;
INSERT INTO inventory_transactions (lot_id, tx_type, quantity, ref_type, ref_id, created_by, created_at)
SELECT l.id, 'IN', l.remaining_qty, 'RECEIPT', 1, 5, NOW() - INTERVAL 2 DAY FROM stock_lots l;
