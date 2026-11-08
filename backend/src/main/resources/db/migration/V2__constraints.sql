-- UniCateen V2: khoa ngoai, UNIQUE, CHECK, cot sinh va chi muc (Bang rang buoc CSDL, Chuong 3).

-- users
ALTER TABLE users
  ADD CONSTRAINT uq_users_email UNIQUE (email),
  ADD CONSTRAINT uq_users_card UNIQUE (card_code),                                   -- BR45
  ADD CONSTRAINT ck_users_card_role CHECK (card_code IS NULL OR role IN ('STUDENT','FACULTY')),
  ADD CONSTRAINT ck_users_failed CHECK (failed_attempts >= 0);

-- danh muc va mon
ALTER TABLE categories
  ADD CONSTRAINT uq_cat_code UNIQUE (code),
  ADD CONSTRAINT uq_cat_name UNIQUE (name);                                          -- BR01

ALTER TABLE menu_items
  ADD CONSTRAINT uq_item_code UNIQUE (code),
  ADD CONSTRAINT uq_item_cat_name UNIQUE (category_id, name),
  ADD CONSTRAINT fk_item_cat FOREIGN KEY (category_id) REFERENCES categories(id);

ALTER TABLE item_prices
  ADD CONSTRAINT ck_price_pos CHECK (price > 0),                                     -- BR02
  ADD CONSTRAINT ck_price_range CHECK (effective_to IS NULL OR effective_to > effective_from),
  ADD CONSTRAINT fk_price_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
  ADD CONSTRAINT fk_price_user FOREIGN KEY (created_by) REFERENCES users(id),
  ADD INDEX ix_price_item_from (menu_item_id, effective_from);

ALTER TABLE ingredients
  ADD CONSTRAINT uq_ing_code UNIQUE (code),
  ADD CONSTRAINT uq_ing_name UNIQUE (name),
  ADD CONSTRAINT ck_ing_min CHECK (min_stock >= 0);

-- BR09: moi mon toi da mot cong thuc dang ap dung
ALTER TABLE recipes
  ADD COLUMN active_item_id BIGINT AS (CASE WHEN status = 'ACTIVE' THEN menu_item_id END) STORED,
  ADD CONSTRAINT uq_recipe_active UNIQUE (active_item_id),
  ADD CONSTRAINT uq_recipe_version UNIQUE (menu_item_id, version),
  ADD CONSTRAINT fk_recipe_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id);

ALTER TABLE recipe_details
  ADD CONSTRAINT uq_rd UNIQUE (recipe_id, ingredient_id),                            -- BR08
  ADD CONSTRAINT ck_rd_qty CHECK (qty_per_portion > 0),
  ADD CONSTRAINT fk_rd_recipe FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_rd_ing FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

-- don hang
ALTER TABLE orders
  ADD CONSTRAINT uq_order_code UNIQUE (code),                                        -- BR10
  ADD CONSTRAINT ck_order_cancel CHECK (status <> 'CANCELLED' OR cancelled_by IS NOT NULL),
  ADD CONSTRAINT fk_order_cashier FOREIGN KEY (cashier_id) REFERENCES users(id),
  ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  ADD CONSTRAINT fk_order_cancel FOREIGN KEY (cancelled_by) REFERENCES users(id),
  ADD INDEX ix_order_status_created (status, created_at),
  ADD INDEX ix_order_cashier_created (cashier_id, created_at);

ALTER TABLE order_details
  ADD CONSTRAINT uq_od UNIQUE (order_id, menu_item_id),                              -- BR11
  ADD CONSTRAINT ck_od_qty CHECK (quantity BETWEEN 1 AND 99),                        -- DV03
  ADD CONSTRAINT ck_od_total CHECK (line_total = quantity * unit_price),
  ADD CONSTRAINT fk_od_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_od_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id);

-- thanh toan: BR20 bang cot sinh + UNIQUE; NFR06 bang idempotency_key UNIQUE
ALTER TABLE payments
  ADD COLUMN active_order_id BIGINT AS
      (CASE WHEN payment_status IN ('PENDING','SUCCESS') THEN order_id END) STORED,
  ADD CONSTRAINT uq_pay_code UNIQUE (code),
  ADD CONSTRAINT uq_pay_idem UNIQUE (idempotency_key),
  ADD CONSTRAINT uq_pay_active UNIQUE (active_order_id),
  ADD CONSTRAINT ck_pay_amount CHECK (amount > 0),                                   -- BR19
  ADD CONSTRAINT ck_pay_cash CHECK (payment_method <> 'CASH' OR payment_status <> 'SUCCESS'
      OR (received_amount >= amount AND change_amount = received_amount - amount)),  -- BR25
  ADD CONSTRAINT ck_pay_card CHECK (payment_method <> 'STUDENT_FACULTY_CARD' OR card_code IS NOT NULL),
  ADD CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_pay_payer FOREIGN KEY (payer_id) REFERENCES users(id),
  ADD CONSTRAINT fk_pay_user FOREIGN KEY (confirmed_by) REFERENCES users(id),
  ADD INDEX ix_pay_pending (payment_status, payment_method, created_at),
  ADD INDEX ix_pay_time (payment_status, transaction_time);

ALTER TABLE preparations
  ADD CONSTRAINT uq_prep_order UNIQUE (order_id),                                    -- GD8
  ADD CONSTRAINT fk_prep_order FOREIGN KEY (order_id) REFERENCES orders(id),
  ADD CONSTRAINT fk_prep_user FOREIGN KEY (updated_by) REFERENCES users(id),
  ADD INDEX ix_prep_status (status);

-- kho
ALTER TABLE suppliers
  ADD CONSTRAINT uq_sup_code UNIQUE (code),
  ADD CONSTRAINT uq_sup_tax UNIQUE (tax_code);                                       -- BR30

ALTER TABLE stock_receipts
  ADD CONSTRAINT uq_sr_code UNIQUE (code),
  ADD CONSTRAINT fk_sr_sup FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  ADD CONSTRAINT fk_sr_user FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE stock_receipt_details
  ADD CONSTRAINT ck_srd_qty CHECK (quantity > 0),                                    -- BR32
  ADD CONSTRAINT fk_srd_receipt FOREIGN KEY (receipt_id) REFERENCES stock_receipts(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_srd_ing FOREIGN KEY (ingredient_id) REFERENCES ingredients(id);

ALTER TABLE stock_lots
  ADD CONSTRAINT uq_lot_code UNIQUE (code),
  ADD CONSTRAINT uq_lot_detail UNIQUE (receipt_detail_id),                           -- GD6
  ADD CONSTRAINT ck_lot_qty CHECK (remaining_qty >= 0),                              -- BR35
  ADD CONSTRAINT fk_lot_ing FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
  ADD CONSTRAINT fk_lot_detail FOREIGN KEY (receipt_detail_id) REFERENCES stock_receipt_details(id),
  ADD INDEX ix_lot_fefo (ingredient_id, expiry_date, received_date);                 -- BR34

ALTER TABLE stock_issues
  ADD CONSTRAINT uq_si_code UNIQUE (code),
  ADD CONSTRAINT fk_si_user FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE stock_issue_plans
  ADD CONSTRAINT uq_sip UNIQUE (issue_id, menu_item_id),
  ADD CONSTRAINT ck_sip_portions CHECK (planned_portions > 0),
  ADD CONSTRAINT fk_sip_issue FOREIGN KEY (issue_id) REFERENCES stock_issues(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_sip_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id);

ALTER TABLE stock_issue_details
  ADD CONSTRAINT ck_sid_qty CHECK (issued_qty > 0),
  ADD CONSTRAINT fk_sid_issue FOREIGN KEY (issue_id) REFERENCES stock_issues(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_sid_lot FOREIGN KEY (lot_id) REFERENCES stock_lots(id);

ALTER TABLE stock_adjustments
  ADD CONSTRAINT uq_adj_code UNIQUE (code),
  ADD CONSTRAINT ck_adj_after CHECK (qty_after >= 0),
  ADD CONSTRAINT ck_adj_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),                -- BR37
  ADD CONSTRAINT fk_adj_lot FOREIGN KEY (lot_id) REFERENCES stock_lots(id),
  ADD CONSTRAINT fk_adj_user FOREIGN KEY (created_by) REFERENCES users(id);

-- ref_id la tham chieu da hinh (theo ref_type), khong co khoa ngoai
ALTER TABLE inventory_transactions
  ADD CONSTRAINT uq_itx_ref UNIQUE (ref_type, ref_id, lot_id),                       -- BR33: ghi kho mot lan
  ADD CONSTRAINT ck_itx_sign CHECK ((tx_type = 'IN' AND quantity > 0)
                                 OR (tx_type = 'OUT' AND quantity < 0)
                                 OR (tx_type = 'ADJUST' AND quantity <> 0 AND reason IS NOT NULL)),
  ADD CONSTRAINT fk_itx_lot FOREIGN KEY (lot_id) REFERENCES stock_lots(id),
  ADD CONSTRAINT fk_itx_user FOREIGN KEY (created_by) REFERENCES users(id),
  ADD INDEX ix_itx_lot_time (lot_id, created_at);

ALTER TABLE audit_logs
  ADD CONSTRAINT fk_audit_user FOREIGN KEY (actor_id) REFERENCES users(id),
  ADD INDEX ix_audit_entity (entity_type, entity_id),
  ADD INDEX ix_audit_time (created_at);
