const demoItems = [
  { id: 1, sku: 'ITM-1001', name: 'Industrial Barcode Scanner', productCategory: 'Scanning & Mobility', description: 'Rugged handheld scanner with charging dock', availableQuantity: 42 },
  { id: 2, sku: 'ITM-1002', name: 'Thermal Label Roll', productCategory: 'Packaging Supplies', description: 'Weather-resistant 4 × 6 inch labels', availableQuantity: 380 },
  { id: 3, sku: 'ITM-1003', name: 'Warehouse Tablet', productCategory: 'Mobile Computing', description: '10-inch inventory floor tablet', availableQuantity: 18 },
  { id: 4, sku: 'ITM-1004', name: 'RFID Reader Gateway', productCategory: 'Identification Systems', description: 'Fixed reader for dock-door inventory tracking', availableQuantity: 12 },
  { id: 5, sku: 'ITM-1005', name: 'Protective Scanner Case', productCategory: 'Equipment Accessories', description: 'Impact-resistant scanner sleeve', availableQuantity: 96 },
  { id: 6, sku: 'ITM-1006', name: 'Mobile Receipt Printer', productCategory: 'Printing & Labeling', description: 'Bluetooth thermal printer', availableQuantity: 27 }
];

const demoOrganizations = [
  { id: 1, code: 'ORG-CENTRAL', name: 'Central Distribution Center', location: 'Columbus, OH' },
  { id: 2, code: 'ORG-EAST', name: 'East Regional Warehouse', location: 'Allentown, PA' },
  { id: 3, code: 'ORG-WEST', name: 'West Regional Warehouse', location: 'Reno, NV' },
  { id: 4, code: 'ORG-SOUTH', name: 'South Service Hub', location: 'Atlanta, GA' }
];

export const transferOrderStatuses = ['REQUESTED', 'APPROVED', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'];

let demoTransferOrders = [
  {
    id: 'b430b623-a828-4b6a-bae1-52c588e69801', orderNumber: 'TO-20260915-091500-B430',
    sourceOrganizationId: 1, sourceOrganizationCode: 'ORG-CENTRAL', sourceOrganizationName: 'Central Distribution Center',
    destinationOrganizationId: 2, destinationOrganizationCode: 'ORG-EAST', destinationOrganizationName: 'East Regional Warehouse',
    itemId: 1, sku: 'ITM-1001', itemName: 'Industrial Barcode Scanner', quantity: 12, status: 'REQUESTED',
    requestedBy: 'Sam Kim', neededBy: '2026-09-20', createdAt: '2026-09-15T09:15:00Z', updatedAt: '2026-09-15T09:15:00Z', version: 0
  },
  {
    id: '83c43db0-3dbf-4ef8-8ebd-61166f54ab02', orderNumber: 'TO-20260914-143000-83C4',
    sourceOrganizationId: 3, sourceOrganizationCode: 'ORG-WEST', sourceOrganizationName: 'West Regional Warehouse',
    destinationOrganizationId: 4, destinationOrganizationCode: 'ORG-SOUTH', destinationOrganizationName: 'South Service Hub',
    itemId: 6, sku: 'ITM-1006', itemName: 'Mobile Receipt Printer', quantity: 8, status: 'APPROVED',
    requestedBy: 'Jordan Lee', neededBy: '2026-09-22', createdAt: '2026-09-14T14:30:00Z', updatedAt: '2026-09-15T08:05:00Z', version: 1
  },
  {
    id: 'cd75da9d-1dbb-4bce-88f6-3644d02f6303', orderNumber: 'TO-20260913-110000-CD75',
    sourceOrganizationId: 2, sourceOrganizationCode: 'ORG-EAST', sourceOrganizationName: 'East Regional Warehouse',
    destinationOrganizationId: 1, destinationOrganizationCode: 'ORG-CENTRAL', destinationOrganizationName: 'Central Distribution Center',
    itemId: 3, sku: 'ITM-1003', itemName: 'Warehouse Tablet', quantity: 5, status: 'IN_TRANSIT',
    requestedBy: 'Morgan Diaz', neededBy: '2026-09-18', createdAt: '2026-09-13T11:00:00Z', updatedAt: '2026-09-15T07:30:00Z', version: 2
  }
];

export const isDemo = import.meta.env.VITE_DEMO_MODE === 'true';

export async function searchItems(query = '') {
  if (isDemo) {
    const term = query.toLowerCase();
    return demoItems.filter(item => `${item.sku} ${item.name} ${item.productCategory}`.toLowerCase().includes(term));
  }
  const response = await fetch(`/api/items?query=${encodeURIComponent(query)}`);
  if (!response.ok) throw new Error('Could not load inventory');
  return response.json();
}

export async function createReceipt(payload) {
  if (isDemo) {
    await new Promise(resolve => setTimeout(resolve, 450));
    const now = new Date();
    return {
      id: '59ca892e-9cbe-4e6c-92a0-d970848d336c',
      receiptNumber: 'RCV-20260915-104218-59CA',
      supplierName: payload.supplierName,
      status: 'RECEIVED',
      createdAt: now.toISOString(),
      totalUnits: payload.lines.reduce((total, line) => total + Number(line.quantity), 0),
      lines: payload.lines.map(line => {
        const item = demoItems.find(candidate => candidate.id === line.itemId);
        return { itemId: item.id, sku: item.sku, itemName: item.name, quantity: Number(line.quantity), serialNumber: line.serialNumber };
      }),
      auditHistory: [
        { status: 'RECEIVED', message: 'Receipt validated and persisted', timestamp: now.toISOString() },
        { status: 'EVENT_PUBLISHED', message: 'ReceiptCreated published to Kafka', timestamp: now.toISOString() }
      ]
    };
  }
  const response = await fetch('/api/receipts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Could not create receipt');
  return data;
}

async function readJson(response, fallbackMessage) {
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || fallbackMessage);
  return data;
}

function enrichTransferOrder(order) {
  const source = demoOrganizations.find(organization => organization.id === Number(order.sourceOrganizationId));
  const destination = demoOrganizations.find(organization => organization.id === Number(order.destinationOrganizationId));
  const item = demoItems.find(candidate => candidate.id === Number(order.itemId));
  if (!source || !destination || !item) throw new Error('Select valid organizations and an inventory item');
  if (source.id === destination.id) throw new Error('Source and destination organizations must be different');
  if (Number(order.quantity) > item.availableQuantity) throw new Error(`Requested quantity exceeds available inventory for ${item.sku}`);
  if (order.neededBy < new Date().toISOString().slice(0, 10)) throw new Error('Needed-by date cannot be in the past');
  return {
    ...order,
    sourceOrganizationId: source.id,
    sourceOrganizationCode: source.code,
    sourceOrganizationName: source.name,
    destinationOrganizationId: destination.id,
    destinationOrganizationCode: destination.code,
    destinationOrganizationName: destination.name,
    itemId: item.id,
    sku: item.sku,
    itemName: item.name,
    quantity: Number(order.quantity),
    requestedBy: order.requestedBy.trim()
  };
}

export async function listOrganizations() {
  if (isDemo) return demoOrganizations;
  return readJson(await fetch('/api/inventory-organizations'), 'Could not load inventory organizations');
}

export async function listTransferOrders() {
  if (isDemo) return demoTransferOrders.map(order => ({ ...order }));
  return readJson(await fetch('/api/transfer-orders'), 'Could not load transfer orders');
}

export async function createTransferOrder(payload) {
  if (isDemo) {
    await new Promise(resolve => setTimeout(resolve, 250));
    const now = new Date();
    const id = crypto.randomUUID();
    const timestamp = now.toISOString().replace(/\D/g, '').slice(0, 14);
    const created = enrichTransferOrder({
      ...payload,
      id,
      orderNumber: `TO-${timestamp.slice(0, 8)}-${timestamp.slice(8)}-${id.slice(0, 4).toUpperCase()}`,
      status: 'REQUESTED',
      createdAt: now.toISOString(),
      updatedAt: now.toISOString(),
      version: 0
    });
    demoTransferOrders = [created, ...demoTransferOrders];
    return { ...created };
  }
  return readJson(await fetch('/api/transfer-orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }), 'Could not create transfer order');
}

export async function updateTransferOrder(id, payload) {
  if (isDemo) {
    await new Promise(resolve => setTimeout(resolve, 200));
    const index = demoTransferOrders.findIndex(order => order.id === id);
    if (index < 0) throw new Error('Transfer order was not found');
    if (demoTransferOrders[index].version !== Number(payload.version)) {
      throw new Error('This transfer order was changed. Refresh the table and try again.');
    }
    const updated = enrichTransferOrder({
      ...demoTransferOrders[index],
      ...payload,
      id,
      orderNumber: demoTransferOrders[index].orderNumber,
      createdAt: demoTransferOrders[index].createdAt,
      updatedAt: new Date().toISOString(),
      version: demoTransferOrders[index].version + 1
    });
    demoTransferOrders[index] = updated;
    return { ...updated };
  }
  return readJson(await fetch(`/api/transfer-orders/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }), 'Could not update transfer order');
}

export { demoItems, demoOrganizations };
