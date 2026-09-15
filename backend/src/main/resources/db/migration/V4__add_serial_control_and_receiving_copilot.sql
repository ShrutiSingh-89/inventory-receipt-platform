ALTER TABLE items
  ADD COLUMN serial_controlled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN serial_prefix VARCHAR(30),
  ADD COLUMN search_aliases VARCHAR(500);

UPDATE items
SET serial_controlled = TRUE,
    serial_prefix = 'SCN',
    search_aliases = 'handheld scanner,barcode gun,business scanner,receiving scanner'
WHERE sku = 'ITM-1001';

UPDATE items
SET search_aliases = '4x6 labels,shipping labels,thermal warehouse labels'
WHERE sku = 'ITM-1002';

UPDATE items
SET serial_controlled = TRUE,
    serial_prefix = 'TAB',
    search_aliases = 'floor tablet,mobile warehouse computer,10 inch tablet'
WHERE sku = 'ITM-1003';

UPDATE items
SET serial_controlled = TRUE,
    serial_prefix = 'RFID',
    search_aliases = 'dock door reader,fixed rfid reader,identification gateway'
WHERE sku = 'ITM-1004';

UPDATE items
SET search_aliases = 'scanner sleeve,rugged scanner cover,protective case'
WHERE sku = 'ITM-1005';

UPDATE items
SET serial_controlled = TRUE,
    serial_prefix = 'PRN',
    search_aliases = 'portable label printer,bluetooth receipt printer,mobile thermal printer'
WHERE sku = 'ITM-1006';

CREATE TABLE receipt_line_serials (
  id BIGSERIAL PRIMARY KEY,
  receipt_line_id BIGINT NOT NULL REFERENCES receipt_lines(id) ON DELETE CASCADE,
  serial_number VARCHAR(100) NOT NULL,
  assignment_source VARCHAR(30) NOT NULL,
  UNIQUE (receipt_line_id, serial_number)
);

CREATE UNIQUE INDEX uq_receipt_serial_number_lower
  ON receipt_line_serials (LOWER(serial_number));

INSERT INTO receipt_line_serials (receipt_line_id, serial_number, assignment_source)
SELECT id, serial_number, 'LEGACY'
FROM receipt_lines
WHERE serial_number IS NOT NULL;

ALTER TABLE receipt_lines DROP COLUMN serial_number;

CREATE INDEX idx_receipt_line_serials_line_id
  ON receipt_line_serials (receipt_line_id);
