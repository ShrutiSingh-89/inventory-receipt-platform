CREATE TABLE inventory_organizations (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  location VARCHAR(160) NOT NULL
);

CREATE TABLE transfer_orders (
  id UUID PRIMARY KEY,
  order_number VARCHAR(45) NOT NULL UNIQUE,
  source_organization_id BIGINT NOT NULL REFERENCES inventory_organizations(id),
  destination_organization_id BIGINT NOT NULL REFERENCES inventory_organizations(id),
  item_id BIGINT NOT NULL REFERENCES items(id),
  quantity INTEGER NOT NULL CHECK (quantity > 0 AND quantity <= 10000),
  status VARCHAR(30) NOT NULL,
  requested_by VARCHAR(100) NOT NULL,
  needed_by DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CHECK (source_organization_id <> destination_organization_id)
);

CREATE INDEX idx_transfer_orders_created_at ON transfer_orders (created_at DESC);
CREATE INDEX idx_transfer_orders_status ON transfer_orders (status);

INSERT INTO inventory_organizations (code, name, location) VALUES
  ('ORG-CENTRAL', 'Central Distribution Center', 'Columbus, OH'),
  ('ORG-EAST', 'East Regional Warehouse', 'Allentown, PA'),
  ('ORG-WEST', 'West Regional Warehouse', 'Reno, NV'),
  ('ORG-SOUTH', 'South Service Hub', 'Atlanta, GA');

INSERT INTO transfer_orders (
  id, order_number, source_organization_id, destination_organization_id, item_id,
  quantity, status, requested_by, needed_by, created_at, updated_at, version
) VALUES
  ('b430b623-a828-4b6a-bae1-52c588e69801', 'TO-20260915-091500-B430',
   (SELECT id FROM inventory_organizations WHERE code = 'ORG-CENTRAL'),
   (SELECT id FROM inventory_organizations WHERE code = 'ORG-EAST'),
   (SELECT id FROM items WHERE sku = 'ITM-1001'),
   12, 'REQUESTED', 'Shruti Singh', '2026-09-20', '2026-09-15T09:15:00Z', '2026-09-15T09:15:00Z', 0),
  ('83c43db0-3dbf-4ef8-8ebd-61166f54ab02', 'TO-20260914-143000-83C4',
   (SELECT id FROM inventory_organizations WHERE code = 'ORG-WEST'),
   (SELECT id FROM inventory_organizations WHERE code = 'ORG-SOUTH'),
   (SELECT id FROM items WHERE sku = 'ITM-1006'),
   8, 'APPROVED', 'Jordan Lee', '2026-09-22', '2026-09-14T14:30:00Z', '2026-09-15T08:05:00Z', 1),
  ('cd75da9d-1dbb-4bce-88f6-3644d02f6303', 'TO-20260913-110000-CD75',
   (SELECT id FROM inventory_organizations WHERE code = 'ORG-EAST'),
   (SELECT id FROM inventory_organizations WHERE code = 'ORG-CENTRAL'),
   (SELECT id FROM items WHERE sku = 'ITM-1003'),
   5, 'IN_TRANSIT', 'Morgan Diaz', '2026-09-18', '2026-09-13T11:00:00Z', '2026-09-15T07:30:00Z', 2);
