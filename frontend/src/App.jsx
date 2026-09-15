import { useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowRight, Box, Check, CheckCircle2, ChevronRight, CircleDot,
  Clock3, FileCheck2, LayoutGrid, PackageCheck, Plus, Radio, Search,
  RotateCcw, Save, ShieldCheck, Trash2, Truck, XCircle
} from 'lucide-react';
import {
  createReceipt, createTransferOrder, demoItems, isDemo, listOrganizations,
  listTransferOrders, searchItems, transferOrderStatuses, updateTransferOrder
} from './api';

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
        <Topbar view={view} />
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
          {view === 'transfers' && <TransferOrders />}
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
    ['transfers', Truck, 'Transfer orders'],
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

function Topbar({ view }) {
  return (
    <header className="topbar">
      <div><span className="crumb-muted">Central Distribution</span><ChevronRight size={15} /><strong>{view === 'transfers' ? 'Inventory transfers' : 'Receiving'}</strong></div>
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

function defaultNeededBy() {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  return date.toISOString().slice(0, 10);
}

function toTransferDraft(order) {
  return {
    sourceOrganizationId: String(order.sourceOrganizationId),
    destinationOrganizationId: String(order.destinationOrganizationId),
    itemId: String(order.itemId),
    quantity: String(order.quantity),
    requestedBy: order.requestedBy,
    neededBy: order.neededBy,
    status: order.status,
    version: order.version
  };
}

function TransferOrders() {
  const [organizations, setOrganizations] = useState([]);
  const [items, setItems] = useState([]);
  const [orders, setOrders] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [form, setForm] = useState({
    sourceOrganizationId: '1', destinationOrganizationId: '2', itemId: '1',
    quantity: '6', requestedBy: 'Sam Kim', neededBy: defaultNeededBy()
  });
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [savingId, setSavingId] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    Promise.all([listOrganizations(), searchItems(''), listTransferOrders()])
      .then(([organizationData, itemData, orderData]) => {
        if (!active) return;
        setOrganizations(organizationData);
        setItems(itemData);
        setOrders(orderData);
        setDrafts(Object.fromEntries(orderData.map(order => [order.id, toTransferDraft(order)])));
        if (organizationData.length > 1 && itemData.length) {
          setForm(current => ({ ...current,
            sourceOrganizationId: String(organizationData[0].id),
            destinationOrganizationId: String(organizationData[1].id),
            itemId: String(itemData[0].id)
          }));
        }
      })
      .catch(loadError => setError(loadError.message))
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  function payloadFrom(values, includeStatus = false) {
    const payload = {
      sourceOrganizationId: Number(values.sourceOrganizationId),
      destinationOrganizationId: Number(values.destinationOrganizationId),
      itemId: Number(values.itemId),
      quantity: Number(values.quantity),
      requestedBy: values.requestedBy.trim(),
      neededBy: values.neededBy
    };
    if (includeStatus) {
      payload.status = values.status;
      payload.version = Number(values.version);
    }
    return payload;
  }

  function validateTransfer(payload) {
    if (payload.sourceOrganizationId === payload.destinationOrganizationId) {
      return 'Source and destination organizations must be different.';
    }
    if (!payload.requestedBy) return 'Requested by is required.';
    if (!Number.isInteger(payload.quantity) || payload.quantity < 1 || payload.quantity > 10000) {
      return 'Quantity must be a whole number between 1 and 10,000.';
    }
    const item = items.find(candidate => candidate.id === payload.itemId);
    if (item && payload.quantity > item.availableQuantity) {
      return `Only ${item.availableQuantity} units of ${item.sku} are currently available.`;
    }
    if (!payload.neededBy) return 'Needed-by date is required.';
    if (payload.neededBy < new Date().toISOString().slice(0, 10)) return 'Needed-by date cannot be in the past.';
    return '';
  }

  async function submitTransfer(event) {
    event.preventDefault();
    const payload = payloadFrom(form);
    const validationError = validateTransfer(payload);
    if (validationError) { setError(validationError); setMessage(''); return; }
    setCreating(true); setError(''); setMessage('');
    try {
      const created = await createTransferOrder(payload);
      setOrders(current => [created, ...current]);
      setDrafts(current => ({ ...current, [created.id]: toTransferDraft(created) }));
      setForm(current => ({ ...current, quantity: '1', neededBy: defaultNeededBy() }));
      setMessage(`${created.orderNumber} was created and added to the editable table.`);
    } catch (submitError) { setError(submitError.message); }
    finally { setCreating(false); }
  }

  function updateDraft(id, field, value) {
    setDrafts(current => ({ ...current, [id]: { ...current[id], [field]: value } }));
    setMessage(''); setError('');
  }

  function resetDraft(order) {
    setDrafts(current => ({ ...current, [order.id]: toTransferDraft(order) }));
    setMessage(''); setError('');
  }

  async function saveOrder(order) {
    const payload = payloadFrom(drafts[order.id], true);
    const validationError = validateTransfer(payload);
    if (validationError) { setError(validationError); setMessage(''); return; }
    setSavingId(order.id); setError(''); setMessage('');
    try {
      const updated = await updateTransferOrder(order.id, payload);
      setOrders(current => current.map(candidate => candidate.id === updated.id ? updated : candidate));
      setDrafts(current => ({ ...current, [updated.id]: toTransferDraft(updated) }));
      setMessage(`${updated.orderNumber} was saved.`);
    } catch (saveError) { setError(saveError.message); }
    finally { setSavingId(null); }
  }

  const requestedCount = orders.filter(order => order.status === 'REQUESTED').length;
  const activeCount = orders.filter(order => ['APPROVED', 'IN_TRANSIT'].includes(order.status)).length;
  const totalUnits = orders.reduce((total, order) => total + order.quantity, 0);

  return (
    <>
      <PageHeader eyebrow="INTER-ORGANIZATION TRANSFER" title="Transfer order requests" description="Move fictional inventory between organizations and maintain every request inline." action={<span className="draft-pill"><Truck size={15}/> Controlled inventory movement</span>} />
      <div className="transfer-metrics">
        <Metric value={String(orders.length)} label="All requests" detail="Visible in the table below" />
        <Metric value={String(requestedCount)} label="Awaiting approval" detail={`${activeCount} approved or in transit`} />
        <Metric value={String(totalUnits)} label="Units requested" detail="Across all transfer orders" />
      </div>

      <form className="panel transfer-create" onSubmit={submitTransfer}>
        <div className="transfer-create-heading"><div><span className="eyebrow">NEW REQUEST</span><h2>Create a transfer order</h2><p>Select two different organizations, a product, and the required quantity.</p></div><button className="primary" disabled={creating || loading}><Plus size={16}/>{creating ? 'Creating…' : 'Create request'}</button></div>
        <div className="transfer-form-grid">
          <label><span>Source organization</span><select value={form.sourceOrganizationId} onChange={event => setForm({ ...form, sourceOrganizationId: event.target.value })}>{organizations.map(organization => <option value={organization.id} key={organization.id}>{organization.code} · {organization.name}</option>)}</select></label>
          <label><span>Destination organization</span><select value={form.destinationOrganizationId} onChange={event => setForm({ ...form, destinationOrganizationId: event.target.value })}>{organizations.map(organization => <option value={organization.id} key={organization.id}>{organization.code} · {organization.name}</option>)}</select></label>
          <label><span>Item / product</span><select value={form.itemId} onChange={event => setForm({ ...form, itemId: event.target.value })}>{items.map(item => <option value={item.id} key={item.id}>{item.sku} · {item.name}</option>)}</select></label>
          <label><span>Quantity</span><input type="number" min="1" max="10000" value={form.quantity} onChange={event => setForm({ ...form, quantity: event.target.value })}/></label>
          <label><span>Needed by</span><input type="date" min={new Date().toISOString().slice(0, 10)} value={form.neededBy} onChange={event => setForm({ ...form, neededBy: event.target.value })}/></label>
          <label><span>Requested by</span><input value={form.requestedBy} onChange={event => setForm({ ...form, requestedBy: event.target.value })}/></label>
        </div>
      </form>

      {error && <div className="error-banner transfer-banner"><XCircle size={18}/>{error}</div>}
      {message && <div className="success-banner"><CheckCircle2 size={18}/>{message}</div>}

      <section className="panel transfer-table-panel">
        <div className="table-title"><div><h2>All transfer requests</h2><p>Every business field is editable. Save commits one row through the REST API.</p></div><span>{loading ? 'Loading…' : `${orders.length} requests`}</span></div>
        <div className="transfer-table-scroll">
          <table className="transfer-table">
            <thead><tr><th>ORDER</th><th>SOURCE</th><th>DESTINATION</th><th>ITEM / PRODUCT</th><th>QTY</th><th>NEEDED BY</th><th>STATUS</th><th>REQUESTED BY</th><th>ACTIONS</th></tr></thead>
            <tbody>
              {orders.map(order => {
                const draft = drafts[order.id] || toTransferDraft(order);
                return (
                  <tr key={order.id}>
                    <td><strong className="order-number">{order.orderNumber}</strong><small>v{draft.version}</small></td>
                    <td><select aria-label={`Source for ${order.orderNumber}`} value={draft.sourceOrganizationId} onChange={event => updateDraft(order.id, 'sourceOrganizationId', event.target.value)}>{organizations.map(organization => <option value={organization.id} key={organization.id}>{organization.code}</option>)}</select></td>
                    <td><select aria-label={`Destination for ${order.orderNumber}`} value={draft.destinationOrganizationId} onChange={event => updateDraft(order.id, 'destinationOrganizationId', event.target.value)}>{organizations.map(organization => <option value={organization.id} key={organization.id}>{organization.code}</option>)}</select></td>
                    <td><select className="product-select" aria-label={`Item for ${order.orderNumber}`} value={draft.itemId} onChange={event => updateDraft(order.id, 'itemId', event.target.value)}>{items.map(item => <option value={item.id} key={item.id}>{item.sku} · {item.name}</option>)}</select></td>
                    <td><input className="quantity-input" aria-label={`Quantity for ${order.orderNumber}`} type="number" min="1" max="10000" value={draft.quantity} onChange={event => updateDraft(order.id, 'quantity', event.target.value)}/></td>
                    <td><input aria-label={`Needed by for ${order.orderNumber}`} type="date" min={new Date().toISOString().slice(0, 10)} value={draft.neededBy} onChange={event => updateDraft(order.id, 'neededBy', event.target.value)}/></td>
                    <td><select className={`status-select status-${draft.status.toLowerCase()}`} aria-label={`Status for ${order.orderNumber}`} value={draft.status} onChange={event => updateDraft(order.id, 'status', event.target.value)}>{transferOrderStatuses.map(status => <option value={status} key={status}>{status.replace('_', ' ')}</option>)}</select></td>
                    <td><input aria-label={`Requested by for ${order.orderNumber}`} value={draft.requestedBy} onChange={event => updateDraft(order.id, 'requestedBy', event.target.value)}/></td>
                    <td><div className="row-actions"><button type="button" className="save-row" onClick={() => saveOrder(order)} disabled={savingId === order.id}><Save size={14}/>{savingId === order.id ? 'Saving' : 'Save'}</button><button type="button" className="reset-row" aria-label={`Reset ${order.orderNumber}`} onClick={() => resetDraft(order)}><RotateCcw size={14}/></button></div></td>
                  </tr>
                );
              })}
              {!loading && !orders.length && <tr><td colSpan="9" className="table-empty">No transfer requests exist yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
    </>
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
