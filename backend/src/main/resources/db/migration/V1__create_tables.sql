-- V1: Create core table structure
-- QR Ordering Platform - Multi-tenant SaaS

-- ========================================
-- Platform-level tables (no tenant_id required)
-- ========================================

-- Restaurant table (multi-tenant entity)
CREATE TABLE restaurant (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    logo_url VARCHAR(255),
    address VARCHAR(255),
    phone VARCHAR(20),
    status INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_restaurant_status CHECK (status IN (1, 2, 3))
);

CREATE INDEX idx_restaurant_status ON restaurant(status);

COMMENT ON COLUMN restaurant.status IS 'Restaurant status: 1=ACTIVE, 2=SUSPENDED, 3=INACTIVE';

-- Platform administrator table
CREATE TABLE platform_admin (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ========================================
-- Tenant-level tables (all require tenant_id)
-- ========================================

-- Restaurant user/staff table
CREATE TABLE restaurant_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role INTEGER NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_role CHECK (role IN (1, 2, 3)),
    UNIQUE (tenant_id, username)
);

CREATE INDEX idx_user_tenant ON restaurant_user(tenant_id);
CREATE INDEX idx_user_username ON restaurant_user(tenant_id, username);

COMMENT ON COLUMN restaurant_user.role IS 'User role: 1=RESTAURANT_ADMIN, 2=WAITER, 3=KITCHEN';

-- Menu category table
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_category_tenant ON category(tenant_id);

-- Menu item table
CREATE TABLE menu_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES category(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(255),
    allergens VARCHAR(255),
    status INTEGER NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_menu_status CHECK (status IN (1, 2, 3)),
    CONSTRAINT chk_menu_price CHECK (price >= 0)
);

CREATE INDEX idx_menu_tenant ON menu_item(tenant_id);
CREATE INDEX idx_menu_category ON menu_item(tenant_id, category_id);
CREATE INDEX idx_menu_status ON menu_item(tenant_id, status);

COMMENT ON COLUMN menu_item.status IS 'Menu item status: 1=AVAILABLE, 2=SOLD_OUT, 3=INACTIVE';

-- Table (dining) information
CREATE TABLE table_info (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    table_number VARCHAR(20) NOT NULL,
    qr_code_url VARCHAR(255),
    seats INT DEFAULT 4,
    status INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_table_status CHECK (status IN (1, 2, 3)),
    UNIQUE (tenant_id, table_number)
);

CREATE INDEX idx_table_tenant ON table_info(tenant_id);

COMMENT ON COLUMN table_info.status IS 'Table status: 1=AVAILABLE, 2=OCCUPIED, 3=RESERVED';

-- Order master table
CREATE TABLE order_info (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    table_id BIGINT NOT NULL REFERENCES table_info(id) ON DELETE RESTRICT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status INTEGER NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    customer_notes TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7)),
    CONSTRAINT chk_order_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_order_tenant ON order_info(tenant_id);
CREATE INDEX idx_order_table ON order_info(tenant_id, table_id);
CREATE INDEX idx_order_status ON order_info(tenant_id, status);
CREATE INDEX idx_order_created ON order_info(tenant_id, created_at DESC);
CREATE INDEX idx_order_number ON order_info(order_number);

COMMENT ON COLUMN order_info.status IS 'Order status: 1=CREATED, 2=CONFIRMED, 3=PREPARING, 4=READY, 5=SERVED, 6=COMPLETED, 7=CANCELLED';

-- Order item detail table
CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES order_info(id) ON DELETE CASCADE,
    menu_item_id BIGINT NOT NULL REFERENCES menu_item(id) ON DELETE RESTRICT,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    notes TEXT,
    CONSTRAINT chk_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_item_subtotal CHECK (subtotal >= 0)
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_order_item_tenant ON order_item(tenant_id);

-- ========================================
-- Idempotency tables
-- ========================================

-- Idempotency record table (prevent duplicate submissions)
CREATE TABLE idempotency_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    order_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_idempotency_key ON idempotency_record(tenant_id, idempotency_key);

-- ========================================
-- Outbox Pattern tables
-- ========================================

-- Outbox events table (event publishing)
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    error_message TEXT,
    CONSTRAINT chk_outbox_status CHECK (status IN (1, 2, 3, 4))
);

CREATE INDEX idx_outbox_status_retry ON outbox_events(status, next_retry_at) WHERE status = 1;
CREATE INDEX idx_outbox_tenant ON outbox_events(tenant_id);
CREATE INDEX idx_outbox_event_id ON outbox_events(event_id);

COMMENT ON COLUMN outbox_events.status IS 'Outbox status: 1=NEW, 2=PROCESSING, 3=SENT, 4=DEAD';

-- Processed events table (consumer-side idempotency deduplication)
CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_event_id ON processed_events(event_id);

-- ========================================
-- Add table comments
-- ========================================

COMMENT ON TABLE restaurant IS 'Restaurant table (multi-tenant entity)';
COMMENT ON TABLE platform_admin IS 'Platform administrator table';
COMMENT ON TABLE restaurant_user IS 'Restaurant staff table';
COMMENT ON TABLE category IS 'Menu category table';
COMMENT ON TABLE menu_item IS 'Menu item table';
COMMENT ON TABLE table_info IS 'Dining table information table';
COMMENT ON TABLE order_info IS 'Order master table';
COMMENT ON TABLE order_item IS 'Order item detail table';
COMMENT ON TABLE idempotency_record IS 'Idempotency record table';
COMMENT ON TABLE outbox_events IS 'Outbox events table';
COMMENT ON TABLE processed_events IS 'Processed events table (consumer deduplication)';
