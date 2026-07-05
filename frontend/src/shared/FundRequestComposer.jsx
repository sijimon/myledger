import { useEffect, useState } from 'react';
import {
  getMyFundRequests, createFundRequest, getFundRequest,
  addFundItem, deleteFundItem, submitFundRequest, deleteFundRequest,
  getReferenceProjects,
} from './api';
import { money } from './format';
import StatusBadge from './StatusBadge';

const emptyRow = () => ({ description: '', qty: '', unitPrice: '' });
const rowComplete = (r) => r.description.trim() && Number(r.qty) > 0 && r.unitPrice !== '' && Number(r.unitPrice) >= 0;
const rowAmount = (r) => (Number(r.qty) > 0 && r.unitPrice !== '' ? Number(r.qty) * Number(r.unitPrice) : 0);

/**
 * Raise-a-request UI: the full new-request form (header + line items), the caller's own
 * requests, and an editable draft detail. Used by both contractors and the owner.
 * Reports created/changed requests via onChange so a host page can refresh its own views.
 */
export default function FundRequestComposer({ onChange }) {
  const [requests, setRequests] = useState([]);
  const [projects, setProjects] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const [projectId, setProjectId] = useState('');
  const [title, setTitle] = useState('');
  const [note, setNote] = useState('');
  const [rows, setRows] = useState([emptyRow()]);

  const [desc, setDesc] = useState('');
  const [qty, setQty] = useState('');
  const [unitPrice, setUnitPrice] = useState('');

  const notify = () => { if (onChange) onChange(); };
  const run = async (fn) => { setError(null); try { await fn(); } catch (e) { setError(e.message || 'Something went wrong'); } };
  const loadList = () => getMyFundRequests().then(setRequests);

  useEffect(() => {
    run(async () => {
      const [reqs, projs] = await Promise.all([getMyFundRequests(), getReferenceProjects()]);
      setRequests(reqs);
      setProjects(projs);
      setProjectId(projs[0]?.id || '');
    });
  }, []);

  const openRequest = (id) => run(async () => { setSelected(await getFundRequest(id)); });

  const setRow = (i, field, value) => setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, [field]: value } : r)));
  const addRow = () => setRows((rs) => [...rs, emptyRow()]);
  const removeRow = (i) => setRows((rs) => (rs.length === 1 ? rs : rs.filter((_, idx) => idx !== i)));
  const formTotal = rows.reduce((sum, r) => sum + rowAmount(r), 0);
  const formCurrency = projects.find((p) => String(p.id) === String(projectId))?.currency;
  const completeItems = rows.filter(rowComplete);
  const resetForm = () => { setTitle(''); setNote(''); setRows([emptyRow()]); };

  const create = (submit) => run(async () => {
    if (submit && completeItems.length === 0) { setError('Add at least one complete line item to submit.'); return; }
    setSaving(true);
    try {
      const created = await createFundRequest({
        projectId: Number(projectId),
        title,
        note: note || null,
        items: completeItems.map((r) => ({ description: r.description, qty: Number(r.qty), unitPrice: Number(r.unitPrice) })),
        submit,
      });
      resetForm();
      await loadList();
      setSelected(created);
      notify();
    } finally {
      setSaving(false);
    }
  });

  const isDraft = selected?.status === 'DRAFT';
  const addItem = (e) => {
    e.preventDefault();
    run(async () => {
      await addFundItem(selected.id, { description: desc, qty: Number(qty), unitPrice: Number(unitPrice) });
      setDesc(''); setQty(''); setUnitPrice('');
      setSelected(await getFundRequest(selected.id));
      await loadList(); notify();
    });
  };
  const removeItem = (itemId) => run(async () => {
    await deleteFundItem(selected.id, itemId);
    setSelected(await getFundRequest(selected.id));
    await loadList(); notify();
  });
  const submit = () => run(async () => { await submitFundRequest(selected.id); setSelected(await getFundRequest(selected.id)); await loadList(); notify(); });
  const removeRequest = () => run(async () => {
    if (!window.confirm('Delete this draft request?')) return;
    await deleteFundRequest(selected.id); setSelected(null); await loadList(); notify();
  });

  return (
    <>
      {error && <p className="error">{error}</p>}

      <section className="panel">
        <h3>New request</h3>
        <div className="form-grid">
          <label>
            Project
            <select required value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {projects.length === 0 && <option value="">No projects available</option>}
              {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </label>
          <label>
            Title
            <input type="text" value={title} placeholder="e.g. Materials advance"
                   onChange={(e) => setTitle(e.target.value)} />
          </label>
          <label className="span-2">
            Note
            <input type="text" value={note} onChange={(e) => setNote(e.target.value)} />
          </label>
        </div>

        <div className="section-head" style={{ marginTop: '1rem' }}>
          <h4 style={{ margin: 0 }}>Line items</h4>
          <button type="button" className="link" onClick={addRow}>+ Add row</button>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Description</th><th className="num">Qty</th><th className="num">Unit price</th><th className="num">Amount</th><th></th></tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={i}>
                  <td><input type="text" value={r.description} placeholder="Description"
                             onChange={(e) => setRow(i, 'description', e.target.value)} /></td>
                  <td className="num"><input type="number" step="0.001" min="0" value={r.qty} className="num-input"
                             onChange={(e) => setRow(i, 'qty', e.target.value)} /></td>
                  <td className="num"><input type="number" step="0.01" min="0" value={r.unitPrice} className="num-input"
                             onChange={(e) => setRow(i, 'unitPrice', e.target.value)} /></td>
                  <td className="num">{money(rowAmount(r), formCurrency)}</td>
                  <td><button type="button" className="link danger" onClick={() => removeRow(i)}>✕</button></td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr><td colSpan="3" className="num"><strong>Total</strong></td><td className="num"><strong>{money(formTotal, formCurrency)}</strong></td><td></td></tr>
            </tfoot>
          </table>
        </div>

        <div className="row-actions" style={{ marginTop: '1rem' }}>
          <button type="button" onClick={() => create(false)} disabled={saving || !projectId || !title.trim()}>
            {saving ? 'Saving…' : 'Save draft'}
          </button>
          <button type="button" onClick={() => create(true)}
                  disabled={saving || !projectId || !title.trim() || completeItems.length === 0}>
            Create & submit
          </button>
        </div>
      </section>

      <section className="panel">
        <h3>My requests</h3>
        <ul className="manage-list">
          {requests.length === 0 && <li className="muted">No requests yet.</li>}
          {requests.map((r) => (
            <li key={r.id}>
              <span className="ml-name">{r.title} <span className="muted">· {r.projectName}</span></span>
              <StatusBadge status={r.status} />
              <span className="bar-value">{money(r.total, r.projectCurrency)}</span>
              <span className="ml-actions"><button className="link" onClick={() => openRequest(r.id)}>Open</button></span>
            </li>
          ))}
        </ul>
      </section>

      {selected && (
        <section className="panel">
          <div className="section-head">
            <h3 style={{ margin: 0 }}>{selected.title} <StatusBadge status={selected.status} /></h3>
            <div className="row-actions">
              {isDraft && <button className="link" onClick={submit}>Submit for review</button>}
              {isDraft && <button className="link danger" onClick={removeRequest}>Delete draft</button>}
              <button className="link" onClick={() => setSelected(null)}>Close</button>
            </div>
          </div>
          <p className="muted">
            {selected.projectName}{selected.note ? ` · ${selected.note}` : ''} · Total <strong>{money(selected.total, selected.projectCurrency)}</strong>
          </p>

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Description</th><th className="num">Qty</th><th className="num">Unit price</th><th className="num">Amount</th>{isDraft && <th></th>}</tr>
              </thead>
              <tbody>
                {(selected.items || []).length === 0 && (
                  <tr><td colSpan={isDraft ? 5 : 4} className="muted">No line items yet.</td></tr>
                )}
                {(selected.items || []).map((it) => (
                  <tr key={it.id}>
                    <td>{it.description}</td>
                    <td className="num">{it.qty}</td>
                    <td className="num">{money(it.unitPrice, selected.projectCurrency)}</td>
                    <td className="num">{money(it.amount, selected.projectCurrency)}</td>
                    {isDraft && <td><button className="link danger" onClick={() => removeItem(it.id)}>Remove</button></td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {isDraft ? (
            <form className="inline-form" onSubmit={addItem} style={{ marginTop: '1rem' }}>
              <input placeholder="Description" required value={desc} onChange={(e) => setDesc(e.target.value)} />
              <input type="number" step="0.001" min="0.001" placeholder="Qty" required value={qty}
                     onChange={(e) => setQty(e.target.value)} style={{ maxWidth: '90px' }} />
              <input type="number" step="0.01" min="0" placeholder="Unit price" required value={unitPrice}
                     onChange={(e) => setUnitPrice(e.target.value)} style={{ maxWidth: '120px' }} />
              <button type="submit">Add item</button>
            </form>
          ) : (
            <p className="muted">This request has been submitted and is locked.</p>
          )}
        </section>
      )}
    </>
  );
}
