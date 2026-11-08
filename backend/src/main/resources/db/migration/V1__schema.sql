-- UniCateen V1: 21 bang theo tu dien du lieu (Phu luc B cua bao cao).
-- Khoa ngoai, chi muc, CHECK va cot sinh duoc them o V2__constraints.sql.

CREATE TABLE users (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  email                VARCHAR(100) NOT NULL,
  full_name            VARCHAR(100) NOT NULL,
  phone                VARCHAR(10)  NULL,
  role                 ENUM('ADMIN','MANAGER','CASHIER','INVENTORY_STAFF','KITCHEN_STAFF','STUDENT','FACULTY') NOT NULL,
  card_code            VARCHAR(20)  NULL,
  password_hash        VARCHAR(100) NOT NULL,
  must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
  failed_attempts      TINYINT      NOT NULL DEFAULT 0,
  locked_until         DATETIME     NULL,
  status               ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  last_login_at        DATETIME     NULL,
  created_at           DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  code          VARCHAR(10)  NOT NULL,
  name          VARCHAR(100) NOT NULL,
  display_order INT          NOT NULL,
  description   VARCHAR(255) NULL,
  status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE menu_items (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(10)  NOT NULL,
  category_id BIGINT       NOT NULL,
  name        VARCHAR(100) NOT NULL,
  unit        VARCHAR(20)  NOT NULL,
  image_url   VARCHAR(255) NULL,
  description VARCHAR(255) NULL,
  status      ENUM('ON_SALE','OUT_OF_STOCK','DISCONTINUED') NOT NULL,
  created_at  DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE item_prices (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  menu_item_id   BIGINT       NOT NULL,
  price          INT UNSIGNED NOT NULL,
  effective_from DATETIME     NOT NULL,
  effective_to   DATETIME     NULL,
  reason         VARCHAR(255) NULL,
  status         ENUM('ACTIVE','SCHEDULED','CANCELLED') NOT NULL,
  created_by     BIGINT       NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ingredients (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  code         VARCHAR(10)   NOT NULL,
  name         VARCHAR(100)  NOT NULL,
  group_name   VARCHAR(50)   NOT NULL,
  unit         VARCHAR(10)   NOT NULL,
  min_stock    DECIMAL(12,3) NOT NULL DEFAULT 0,
  track_expiry BOOLEAN       NOT NULL DEFAULT TRUE,
  storage_note VARCHAR(255)  NULL,
  status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recipes (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  menu_item_id BIGINT   NOT NULL,
  version      INT      NOT NULL,
  status       ENUM('ACTIVE','RETIRED') NOT NULL,
  note         TEXT     NULL,
  created_at   DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recipe_details (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipe_id       BIGINT        NOT NULL,
  ingredient_id   BIGINT        NOT NULL,
  qty_per_portion DECIMAL(10,3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  code          VARCHAR(20)  NOT NULL,
  cashier_id    BIGINT       NOT NULL,
  customer_id   BIGINT       NULL,
  status        ENUM('DRAFT','PENDING_PAYMENT','PAID','CANCELLED') NOT NULL,
  total_amount  INT UNSIGNED NOT NULL DEFAULT 0,
  cancel_reason VARCHAR(255) NULL,
  cancelled_by  BIGINT       NULL,
  created_at    DATETIME     NOT NULL,
  paid_at       DATETIME     NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_details (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id     BIGINT       NOT NULL,
  menu_item_id BIGINT       NOT NULL,
  quantity     TINYINT      NOT NULL,
  unit_price   INT UNSIGNED NOT NULL,
  line_total   INT UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  code             VARCHAR(20)  NOT NULL,
  order_id         BIGINT       NOT NULL,
  payment_method   ENUM('STUDENT_FACULTY_CARD','CASH','BANK_TRANSFER') NOT NULL,
  amount           INT UNSIGNED NOT NULL,
  payment_status   ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL,
  reference_code   VARCHAR(50)  NULL,
  received_amount  INT UNSIGNED NULL,
  change_amount    INT UNSIGNED NULL,
  failure_reason   VARCHAR(255) NULL,
  idempotency_key  VARCHAR(64)  NOT NULL,
  payer_id         BIGINT       NULL,
  card_code        VARCHAR(20)  NULL,
  confirmed_by     BIGINT       NULL,
  transaction_time DATETIME     NULL,
  created_at       DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE preparations (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT   NOT NULL,
  status     ENUM('NOT_STARTED','PREPARING','READY','SERVED') NOT NULL,
  started_at DATETIME NULL,
  ready_at   DATETIME NULL,
  served_at  DATETIME NULL,
  updated_by BIGINT   NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE suppliers (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  code         VARCHAR(10)  NOT NULL,
  name         VARCHAR(100) NOT NULL,
  tax_code     VARCHAR(14)  NULL,
  contact_name VARCHAR(100) NULL,
  phone        VARCHAR(10)  NOT NULL,
  email        VARCHAR(100) NULL,
  address      VARCHAR(255) NULL,
  main_goods   VARCHAR(100) NULL,
  status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_receipts (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  code         VARCHAR(20) NOT NULL,
  supplier_id  BIGINT      NOT NULL,
  receipt_date DATE        NOT NULL,
  status       ENUM('DRAFT','CONFIRMED','CANCELLED') NOT NULL,
  total_value  BIGINT      NOT NULL DEFAULT 0,
  created_by   BIGINT      NOT NULL,
  confirmed_at DATETIME    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_receipt_details (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  receipt_id    BIGINT        NOT NULL,
  ingredient_id BIGINT        NOT NULL,
  quantity      DECIMAL(12,3) NOT NULL,
  unit_cost     INT UNSIGNED  NOT NULL,
  expiry_date   DATE          NULL,
  line_total    BIGINT        NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_lots (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  code              VARCHAR(20)   NOT NULL,
  ingredient_id     BIGINT        NOT NULL,
  receipt_detail_id BIGINT        NOT NULL,
  received_date     DATE          NOT NULL,
  expiry_date       DATE          NULL,
  unit_cost         INT UNSIGNED  NOT NULL,
  remaining_qty     DECIMAL(12,3) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_issues (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  code         VARCHAR(20) NOT NULL,
  issue_date   DATE        NOT NULL,
  shift        ENUM('MORNING','LUNCH','EVENING') NOT NULL,
  status       ENUM('DRAFT','CONFIRMED','CANCELLED') NOT NULL,
  created_by   BIGINT      NOT NULL,
  confirmed_at DATETIME    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_issue_plans (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  issue_id         BIGINT NOT NULL,
  menu_item_id     BIGINT NOT NULL,
  planned_portions INT    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_issue_details (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  issue_id       BIGINT        NOT NULL,
  lot_id         BIGINT        NOT NULL,
  suggested_qty  DECIMAL(12,3) NULL,
  issued_qty     DECIMAL(12,3) NOT NULL,
  outside_recipe BOOLEAN       NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_adjustments (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(20)   NOT NULL,
  lot_id     BIGINT        NOT NULL,
  qty_before DECIMAL(12,3) NOT NULL,
  qty_after  DECIMAL(12,3) NOT NULL,
  reason     VARCHAR(255)  NOT NULL,
  created_by BIGINT        NOT NULL,
  created_at DATETIME      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_transactions (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  lot_id     BIGINT        NOT NULL,
  tx_type    ENUM('IN','OUT','ADJUST') NOT NULL,
  quantity   DECIMAL(12,3) NOT NULL,
  ref_type   ENUM('RECEIPT','ISSUE','ADJUSTMENT') NOT NULL,
  ref_id     BIGINT        NOT NULL,
  reason     VARCHAR(255)  NULL,
  created_by BIGINT        NOT NULL,
  created_at DATETIME      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id     BIGINT      NOT NULL,
  action       VARCHAR(50) NOT NULL,
  entity_type  VARCHAR(50) NOT NULL,
  entity_id    BIGINT      NOT NULL,
  before_value JSON        NULL,
  after_value  JSON        NULL,
  created_at   DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
