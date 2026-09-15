const demoItems = [
  { id: 1, sku: 'ITM-1001', name: 'Industrial Barcode Scanner', productCategory: 'Scanning & Mobility', description: 'Rugged handheld scanner with charging dock', availableQuantity: 42 },
  { id: 2, sku: 'ITM-1002', name: 'Thermal Label Roll', productCategory: 'Packaging Supplies', description: 'Weather-resistant 4 × 6 inch labels', availableQuantity: 380 },
  { id: 3, sku: 'ITM-1003', name: 'Warehouse Tablet', productCategory: 'Mobile Computing', description: '10-inch inventory floor tablet', availableQuantity: 18 },
  { id: 4, sku: 'ITM-1004', name: 'RFID Reader Gateway', productCategory: 'Identification Systems', description: 'Fixed reader for dock-door inventory tracking', availableQuantity: 12 },
  { id: 5, sku: 'ITM-1005', name: 'Protective Scanner Case', productCategory: 'Equipment Accessories', description: 'Impact-resistant scanner sleeve', availableQuantity: 96 },
  { id: 6, sku: 'ITM-1006', name: 'Mobile Receipt Printer', productCategory: 'Printing & Labeling', description: 'Bluetooth thermal printer', availableQuantity: 27 }
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

export { demoItems };
