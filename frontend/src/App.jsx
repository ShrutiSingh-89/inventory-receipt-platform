import { useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowRight, Box, Check, CheckCircle2, ChevronRight, CircleDot,
  Clock3, FileCheck2, LayoutGrid, PackageCheck, Plus, Radio, Search,
  ShieldCheck, Trash2, XCircle
} from 'lucide-react';
import { createReceipt, demoItems, isDemo, searchItems } from './api';

const sampleReceipt = {
  id: '59ca892e-9cbe-4e6c-92a0-d970848d336c',
  receiptNumber: 'RCV-20260915-104218-59CA',
  supplierName: 'Northwind Industrial Supply',
  status: 'RECEIVED',
  createdAt: '2026-09-15T10:42:18Z',
  totalUnits: 28,
  lines: [
    { itemId: 1, sku: 'ITM-1001', itemName: 'Industrial Barcode Scanner', quantity: 8, serialNumber: 'SCN-B2409-01' },
    { itemId: 2, sku: 'ITM-1002', itemName: 'Thermal Label Roll', quantity: 20, serialNumber: null }
  ],
  auditHistory: [
    { status: 'RECEIVED', message: 'Receipt validated and persisted', timestamp: '2026-09-15T10:42:18Z' },
    { status: 'EVENT_PUBLISHED', message: 'ReceiptCreated published to Kafka', timestamp: '2026-09-15T10:42:19Z' }
  ]
};

function App() {
  const params = new URLSearchParams(window.location.search);
  const initialView = params.get('view') || 'inventory';
  const validationDemo = params.get('validation') === '1';
  const [view, setView] = useState(initialView);
  const [receipt, setReceipt] = useState(initialView === 'status' && isDemo ? sampleReceipt : null);
  const [draftLines, setDraftLines] = useState([]);

  function navigate(next) {
    setView(next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function addItem(item) {
    setDraftLines(lines => lines.some(line => line.item.id === item.id)
      ? lines
      : [...lines, { item, quantity: 1, serialNumber: '' }]);
    navigate('create');
  }

  return (
    <div className="app-shell">
      <Sidebar view={view} navigate={navigate} />
      <main className="main">
        <Topbar />
        <div className="page-wrap">
          {view === 'inventory' && <InventorySearch onAdd={addItem} />}
          {view === 'create' && (
            <CreateReceipt
              initialLines={draftLines}
              validationDemo={validationDemo}
              onCreated={created => { setReceipt(created); navigate('status'); }}
            />
          )}
          {view === 'status' && <ReceiptStatus receipt={receipt || (isDemo ? sampleReceipt : null)} navigate={navigate} />}
          {view === 'events' && <EventMonitor receipt={receipt || sampleReceipt} />}
        </div>
      </main>
    </div>
  );
}

function Sidebar({ view, navigate }) {
  const links = [
    ['inventory', LayoutGrid, 'Inventory'],
    ['create', Plus, 'New receipt'],
    ['status', FileCheck2, 'Receipt status'],
    ['events', Radio, 'Event monitor']
  ];
  return (
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark"><Box size={22} /></span><span>DockFlow</span></div>
      <div className="workspace-label">OPERATIONS</div>
      <nav>
        {links.map(([id, Icon, label]) => (
          <button key={id} className={view === id ? 'nav-link active' : 'nav-link'} onClick={() => navigate(id)}>
            <Icon size={18} /> {label}
          </button>
        ))}
      </nav>
      <div className="system-card">
        <div className="system-row"><span className="pulse" /> All systems operational</div>
        <div className="system-detail">API · DB · Kafka</div>
      </div>
      <div className="profile"><div className="avatar">SK</div><div><strong>Sam Kim</strong><span>Warehouse lead</span></div></div>
    </aside>
  );
}

function Topbar() {
  return (
    <header className="topbar">
      <div><span className="crumb-muted">Central Distribution</span><ChevronRight size={15} /><strong>Receiving</strong></div>
      <span className="environment"><CircleDot size={13} /> Local environment</span>
    </header>
  );
}

function PageHeader({ eyebrow, title, description, action }) {
  return <div className="page-header"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</div>;
}

function InventorySearch({ onAdd }) {
  const [query, setQuery] = useState(isDemo ? 'scanner' : '');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => {
      setLoading(true);
      searchItems(query).then(setItems).catch(error => setError(error.message)).finally(() => setLoading(false));
    }, 180);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <>
      <PageHeader eyebrow="INVENTORY CATALOG" title="Find inventory items" description="Search by item name or SKU, then add a line to a new receipt." action={<button className="primary" onClick={() => onAdd(demoItems[0])}><Plus size={17}/> Create receipt</button>} />
      <section className="panel search-panel">
        <label className="search-box"><Search size={20}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search by SKU or item name"/><kbd>⌘ K</kbd></label>
        <div className="result-meta"><span>{loading ? 'Searching…' : `${items.length} items found`}</span><span>Availability updated just now</span></div>
        {error && <div className="error-banner"><XCircle size={18}/>{error}</div>}
        <div className="item-list">
          {items.map((item, index) => (
            <article className="item-row" key={item.id}>
              <div className={`item-icon color-${index % 3}`}><Box size={23}/></div>
              <div className="item-copy"><div><span className="sku">{item.sku}</span><span className="category-tag">{item.productCategory}</span><span className="stock"><i/> {item.availableQuantity} available</span></div><h3>{item.name}</h3><p>{item.description}</p></div>
              <button className="secondary" onClick={() => onAdd(item)}>Add to receipt <ArrowRight size={16}/></button>
            </article>
          ))}
        </div>
      </section>
      <div className="stats-grid">
        <Metric value="575" label="Units available" detail="Across active locations" />
        <Metric value="24" label="Receipts this week" detail="↑ 12% from last week" />
        <Metric value="99.8%" label="Validation rate" detail="Clean receipt submissions" />
      </div>
    </>
  );
}

function Metric({ value, label, detail }) {
  return <div className="metric"><span>{label}</span><strong>{value}</strong><small>{detail}</small></div>;
}

function CreateReceipt({ initialLines, validationDemo, onCreated }) {
  const startingLines = useMemo(() => {
    if (initialLines.length) return initialLines;
    return validationDemo
      ? [
          { item: demoItems[0], quantity: 8, serialNumber: 'SCN-B2409-01' },
          { item: demoItems[2], quantity: 4, serialNumber: 'SCN-B2409-01' }
        ]
      : [
          { item: demoItems[0], quantity: 8, serialNumber: 'SCN-B2409-01' },
          { item: demoItems[1], quantity: 20, serialNumber: '' }
        ];
  }, []);
  const [supplier, setSupplier] = useState('Northwind Industrial Supply');
  const [lines, setLines] = useState(startingLines);
  const [errors, setErrors] = useState(validationDemo ? { serial: 'Serial number must be unique within this receipt.' } : {});
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState('');

  function updateLine(index, field, value) {
    setLines(current => current.map((line, i) => i === index ? { ...line, [field]: value } : line));
    setErrors({});
  }
  function removeLine(index) { setLines(current => current.filter((_, i) => i !== index)); }

  async function submit(event) {
    event.preventDefault();
    const serials = lines.map(line => line.serialNumber.trim().toLowerCase()).filter(Boolean);
    const nextErrors = {};
    if (!supplier.trim()) nextErrors.supplier = 'Supplier name is required.';
    if (!lines.length) nextErrors.lines = 'Add at least one receipt line.';
    if (lines.some(line => Number(line.quantity) < 1)) nextErrors.quantity = 'Quantity must be greater than zero.';
    if (new Set(serials).size !== serials.length) nextErrors.serial = 'Serial number must be unique within this receipt.';
    if (Object.keys(nextErrors).length) { setErrors(nextErrors); return; }
    setSubmitting(true); setApiError('');
    try {
      const created = await createReceipt({ supplierName: supplier, lines: lines.map(line => ({ itemId: line.item.id, quantity: Number(line.quantity), serialNumber: line.serialNumber || null })) });
      onCreated(created);
    } catch (error) { setApiError(error.message); }
    finally { setSubmitting(false); }
  }

  return (
    <form onSubmit={submit}>
      <PageHeader eyebrow="INBOUND RECEIPT" title="Create inventory receipt" description="Record incoming stock and validate every line before submission." action={<span className="draft-pill"><Clock3 size={15}/> Draft · autosaved</span>} />
      <div className="form-layout">
        <div>
          <section className="panel form-section">
            <div className="section-heading"><span>1</span><div><h2>Receipt details</h2><p>Identify the supplier for this shipment.</p></div></div>
            <label className="field"><span>Supplier name <b>*</b></span><input value={supplier} onChange={e => setSupplier(e.target.value)} className={errors.supplier ? 'invalid' : ''}/>{errors.supplier && <small className="field-error">{errors.supplier}</small>}</label>
          </section>
          <section className="panel form-section lines-section">
            <div className="section-heading"><span>2</span><div><h2>Receipt lines</h2><p>Add items, quantities, and optional serial numbers.</p></div><button type="button" className="text-button"><Plus size={16}/> Add item</button></div>
            <div className="line-table-head"><span>ITEM</span><span>QUANTITY</span><span>SERIAL NUMBER</span><span/></div>
            {lines.map((line, index) => (
              <div className="line-row" key={`${line.item.id}-${index}`}>
                <div className="line-item"><div className="mini-icon"><Box size={18}/></div><div><strong>{line.item.name}</strong><span>{line.item.sku}</span></div></div>
                <label><input type="number" min="1" max="10000" value={line.quantity} onChange={e => updateLine(index, 'quantity', e.target.value)} className={errors.quantity ? 'invalid' : ''}/></label>
                <label><input value={line.serialNumber} onChange={e => updateLine(index, 'serialNumber', e.target.value)} placeholder="Optional" className={errors.serial && line.serialNumber ? 'invalid' : ''}/>{errors.serial && index === lines.length - 1 && <small className="field-error inline-error">{errors.serial}</small>}</label>
                <button type="button" className="icon-button" onClick={() => removeLine(index)}><Trash2 size={17}/></button>
              </div>
            ))}
            {errors.lines && <div className="error-banner"><XCircle size={18}/>{errors.lines}</div>}
          </section>
          {apiError && <div className="error-banner"><XCircle size={18}/>{apiError}</div>}
        </div>
        <aside className="summary-card">
          <span className="eyebrow">RECEIPT SUMMARY</span>
          <div className="summary-line"><span>Line items</span><strong>{lines.length}</strong></div>
          <div className="summary-line"><span>Total units</span><strong>{lines.reduce((sum, line) => sum + (Number(line.quantity) || 0), 0)}</strong></div>
          <div className="divider"/>
          <div className="check-list"><span><Check size={15}/> Supplier identified</span><span><Check size={15}/> Quantities validated</span><span className={errors.serial ? 'check-bad' : ''}>{errors.serial ? <XCircle size={15}/> : <Check size={15}/>} Serial numbers unique</span></div>
          <button className="primary submit-button" disabled={submitting}>{submitting ? 'Creating receipt…' : 'Create receipt'}<ArrowRight size={17}/></button>
          <p className="submit-note"><ShieldCheck size={14}/> Validation runs before any data is saved.</p>
        </aside>
      </div>
    </form>
  );
}

function ReceiptStatus({ receipt, navigate }) {
  if (!receipt) return <section className="empty-state"><PackageCheck size={36}/><h2>No receipt selected</h2><p>Create a receipt to see its processing status.</p><button className="primary" onClick={() => navigate('create')}>Create receipt</button></section>;
  return (
    <>
      <PageHeader eyebrow="RECEIPT STATUS" title="Receipt confirmed" description="The receipt is stored and its event is available to downstream consumers." action={<button className="secondary" onClick={() => navigate('create')}><Plus size={17}/> New receipt</button>} />
      <section className="success-hero">
        <div className="success-icon"><CheckCircle2 size={34}/></div>
        <div><span className="status-badge">{receipt.status}</span><h2>{receipt.receiptNumber}</h2><p>Created for {receipt.supplierName}</p></div>
        <div className="hero-stat"><span>Total units</span><strong>{receipt.totalUnits}</strong></div>
        <div className="hero-stat"><span>Line items</span><strong>{receipt.lines.length}</strong></div>
      </section>
      <div className="status-layout">
        <section className="panel status-panel">
          <div className="panel-title"><div><h2>Receipt lines</h2><p>Validated inventory included in this receipt.</p></div><span>{receipt.lines.length} lines</span></div>
          {receipt.lines.map(line => <div className="confirmed-line" key={line.itemId}><div className="mini-icon"><Box size={18}/></div><div><strong>{line.itemName}</strong><span>{line.sku}{line.serialNumber ? ` · ${line.serialNumber}` : ''}</span></div><b>{line.quantity} units</b></div>)}
        </section>
        <section className="panel status-panel">
          <div className="panel-title"><div><h2>Lifecycle & audit</h2><p>Traceable processing history.</p></div><Activity size={19}/></div>
          <div className="timeline">
            {receipt.auditHistory.map((entry, index) => <div className="timeline-entry" key={entry.status}><span className="timeline-dot"><Check size={13}/></span><div><strong>{entry.status.replace('_', ' ')}</strong><p>{entry.message}</p><small>{new Date(entry.timestamp).toLocaleString()}</small></div>{index === 1 && <span className="kafka-badge"><Radio size={12}/> Kafka</span>}</div>)}
          </div>
        </section>
      </div>
      <button className="event-link" onClick={() => navigate('events')}>View event payload and consumer activity <ArrowRight size={16}/></button>
    </>
  );
}

function EventMonitor({ receipt }) {
  const payload = {
    eventId: '2c02bb8b-c64a-4d76-b61f-081267bab20d',
    receiptId: receipt.id,
    receiptNumber: receipt.receiptNumber,
    totalUnits: receipt.totalUnits,
    occurredAt: receipt.createdAt
  };
  return (
    <>
      <PageHeader eyebrow="EVENT STREAM" title="Kafka event monitor" description="Observe receipt events published by the API and processed by the audit consumer." action={<span className="live-pill"><span className="pulse"/> LIVE</span>} />
      <div className="event-metrics"><Metric value="receipt-created" label="Topic" detail="3 partitions · 1 replica"/><Metric value="0" label="Consumer lag" detail="receipt-audit group"/><Metric value="12 ms" label="Last processing time" detail="Healthy throughput"/></div>
      <section className="terminal-panel">
        <div className="terminal-head"><div><span/><span/><span/></div><strong>ReceiptAuditConsumer</strong><small>localhost:9092</small></div>
        <div className="terminal-body">
          <div className="log-line"><span>10:42:18.901</span><b>INFO</b><p>Received message from <em>receipt-created</em> · partition 1 · offset 24</p></div>
          <div className="payload"><span>ReceiptCreated</span><pre>{JSON.stringify(payload, null, 2)}</pre></div>
          <div className="log-line"><span>10:42:18.913</span><b>INFO</b><p>Audit event processed successfully in 12 ms</p></div>
          <div className="terminal-cursor">_<i/></div>
        </div>
      </section>
    </>
  );
}

export default App;
