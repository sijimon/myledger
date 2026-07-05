import { useEffect, useState } from 'react';
import {
  getExpenses, createExpense, deleteExpense,
  getCategories, getProjects, getPhases, uploadFile, fileUrl, getAccessToken,
} from '../shared/api';
import { money } from '../shared/format';
import { useFinancialYears } from '../shared/useFinancialYears';
import FinancialYearSelect from '../shared/FinancialYearSelect';

const todayIso = () => new Date().toISOString().slice(0, 10);

const emptyForm = () => ({
  amount: '', expenseDate: todayIso(), categoryId: '', projectId: '', phaseId: '', vendor: '', notes: '',
});

export default function Expenses() {
  const { years, fy, setFy, ready } = useFinancialYears();
  const [expenses, setExpenses] = useState([]);
  const [categories, setCategories] = useState([]); // active only
  const [projects, setProjects] = useState([]);     // active only
  const [phases, setPhases] = useState([]);         // active phases of the selected project
  const [form, setForm] = useState(emptyForm());
  const [receipt, setReceipt] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Active projects (mount).
  useEffect(() => {
    getProjects()
      .then((projs) => {
        const activeProjs = projs.filter((p) => p.status === 'ACTIVE');
        setProjects(activeProjs);
        setForm((f) => ({ ...f, projectId: f.projectId || activeProjs[0]?.id || '' }));
      })
      .catch((e) => setError(e.message || 'Failed to load'));
  }, []);

  // Categories and phases are scoped to the selected project — reload on project change,
  // and reset the category/phase selection to that project's options.
  useEffect(() => {
    if (!form.projectId) { setCategories([]); setPhases([]); return; }
    Promise.all([getCategories(form.projectId), getPhases(form.projectId)])
      .then(([cats, phs]) => {
        const activeCats = cats.filter((c) => c.active);
        setCategories(activeCats);
        setPhases(phs.filter((p) => p.active));
        setForm((f) => ({ ...f, categoryId: activeCats[0]?.id || '', phaseId: '' }));
      })
      .catch((e) => setError(e.message || 'Failed to load'));
  }, [form.projectId]);

  // Expense list, refetched when the FY filter changes.
  useEffect(() => {
    if (!ready) return;
    getExpenses(fy).then(setExpenses).catch((e) => setError(e.message || 'Failed to load'));
  }, [fy, ready]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function onReceiptChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const res = await uploadFile(file);
      setReceipt({ fileId: res.fileId, name: res.name });
    } catch (err) {
      setError(err.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  }

  async function onSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createExpense({
        amount: form.amount,
        expenseDate: form.expenseDate,
        categoryId: Number(form.categoryId),
        projectId: Number(form.projectId),
        phaseId: form.phaseId ? Number(form.phaseId) : null,
        vendor: form.vendor || null,
        notes: form.notes || null,
        fileId: receipt?.fileId ?? null,
      });
      setForm({ ...emptyForm(), categoryId: form.categoryId, projectId: form.projectId });
      setReceipt(null);
      const list = await getExpenses(fy);
      setExpenses(list);
    } catch (err) {
      setError(err.message || 'Could not save expense');
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(id) {
    if (!window.confirm('Delete this expense?')) return;
    setError(null);
    try {
      await deleteExpense(id);
      setExpenses((list) => list.filter((x) => x.id !== id));
    } catch (err) {
      setError(err.message || 'Delete failed');
    }
  }

  async function openReceipt(id) {
    const res = await fetch(fileUrl(id), {
      headers: { Authorization: `Bearer ${getAccessToken()}` },
      credentials: 'include',
    });
    if (!res.ok) {
      setError('Could not open receipt');
      return;
    }
    const blob = await res.blob();
    window.open(URL.createObjectURL(blob), '_blank', 'noopener');
  }

  const noRefData = categories.length === 0 || projects.length === 0;

  return (
    <>
      <div className="section-head">
        <h2>Expenses</h2>
        <FinancialYearSelect years={years} value={fy} onChange={setFy} />
      </div>
      {error && <p className="error">{error}</p>}
      {noRefData && (
        <p className="muted">
          You need at least one active project and category. Add them under <strong>Manage</strong>.
        </p>
      )}

      <form className="panel form-grid" onSubmit={onSubmit}>
        <label>
          Amount
          <input type="number" step="0.01" min="0" required
                 value={form.amount} onChange={(e) => set('amount', e.target.value)} />
        </label>
        <label>
          Date
          <input type="date" required max={todayIso()}
                 value={form.expenseDate} onChange={(e) => set('expenseDate', e.target.value)} />
        </label>
        <label>
          Project
          <select required value={form.projectId} onChange={(e) => set('projectId', e.target.value)}>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </label>
        <label>
          Category
          <select required value={form.categoryId} onChange={(e) => set('categoryId', e.target.value)}>
            {categories.length === 0 && <option value="">— none for this project —</option>}
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        {phases.length > 0 && (
          <label>
            Phase
            <select value={form.phaseId} onChange={(e) => set('phaseId', e.target.value)}>
              <option value="">— None —</option>
              {phases.map((ph) => <option key={ph.id} value={ph.id}>{ph.name}</option>)}
            </select>
          </label>
        )}
        <label>
          Vendor
          <input type="text" value={form.vendor} onChange={(e) => set('vendor', e.target.value)} />
        </label>
        <label>
          Receipt
          <input type="file" accept="image/*,application/pdf" onChange={onReceiptChange} />
          {uploading && <small className="muted">Uploading…</small>}
          {receipt && <small className="muted">Attached: {receipt.name}</small>}
        </label>
        <label className="span-2">
          Notes
          <input type="text" value={form.notes} onChange={(e) => set('notes', e.target.value)} />
        </label>
        <div className="span-2">
          <button type="submit" disabled={saving || uploading || noRefData}>
            {saving ? 'Saving…' : 'Add expense'}
          </button>
        </div>
      </form>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Date</th><th>Category</th><th>Project</th><th>Phase</th><th>Vendor</th>
              <th className="num">Amount</th><th>Receipt</th><th></th>
            </tr>
          </thead>
          <tbody>
            {expenses.length === 0 && (
              <tr><td colSpan="8" className="muted">No expenses in this period.</td></tr>
            )}
            {expenses.map((x) => (
              <tr key={x.id}>
                <td>{x.expenseDate}</td>
                <td>{x.categoryName}</td>
                <td>{x.projectName}</td>
                <td>{x.phaseName || '—'}</td>
                <td>{x.vendor || '—'}</td>
                <td className="num">{money(x.amount, x.projectCurrency)}</td>
                <td>
                  {x.fileId
                    ? <button className="link" onClick={() => openReceipt(x.fileId)}>View</button>
                    : <span className="muted">—</span>}
                </td>
                <td>
                  <button className="link danger" onClick={() => onDelete(x.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
