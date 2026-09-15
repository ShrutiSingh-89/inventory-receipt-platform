CREATE TABLE items (
  id BIGSERIAL PRIMARY KEY,
  sku VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  product_category VARCHAR(80) NOT NULL,
  description VARCHAR(500),
  available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0)
);

CREATE TABLE receipts (
  id UUID PRIMARY KEY,
  receipt_number VARCHAR(40) NOT NULL UNIQUE,
  supplier_name VARCHAR(160) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE receipt_lines (
  id BIGSERIAL PRIMARY KEY,
  receipt_id UUID NOT NULL REFERENCES receipts(id),
  item_id BIGINT NOT NULL REFERENCES items(id),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  serial_number VARCHAR(100),
  UNIQUE (receipt_id, serial_number)
);

CREATE INDEX idx_items_name_lower ON items (LOWER(name));
CREATE INDEX idx_receipt_lines_receipt_id ON receipt_lines (receipt_id);
