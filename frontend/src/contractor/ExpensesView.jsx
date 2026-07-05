import { useEffect, useState } from 'react';
import {
  getExpenses, createExpense, deleteExpense,
  getReferenceProjects, getReferenceCategories, getReferencePhases,
} from '../shared/api';
import { money } from '../shared/format';
import { useFinancialYears } from '../shared/useFinancialYears';
import FinancialYearSelect from '../shared/FinancialYearSelect';

const todayIso = () => new Date().toISOString().slice(0, 10);
const emptyForm = () => ({ amount: '', expenseDate: todayIso(), projectId: '', categoryId: '', phaseId: '', vendor: '', notes: '' });

/** Contractor Expenses: add/manage your own expenses, scoped to assigned projects. */
export default function ExpensesView() {
  const { years, fy, setFy, ready } = useFinancialYears();
  const [projects, setProjects] = useState([]);
  const [categories, setCategories] = useState([]);
  const [phases, setPhases] = useState([]);
  const [form, setForm] = useState(emptyForm());
  const [expenses, setExpenses] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const run = async (fn) => { setError(null); try { await fn(); } catch (e) { setError(e.message || 'Something went wrong'); } };

  // Assigned projects (mount)
  useEffect(() => {
    run(async () => {
      const projs = await getReferenceProjects();
      setProjects(projs);
      setForm((f) => ({ ...f, projectId: f.projectId || projs[0]?.id || '' }));
    });
  }, []);

  // Categories + phases for the selected project
  useEffect(() => {
    if (!form.projectId) { setCategories([]); setPhases([]); return; }
    run(async () => {
      const [cats, phs] = await Promise.all([
        getReferenceCategories(form.projectId), getReferencePhases(form.projectId),
      ]);
      setCategories(cats);
      setPhases(phs);
      setForm((f) => ({ ...f, categoryId: cats[0]?.id || '', phaseId: '' }));
    });
  }, [form.projectId]);

  // Own expenses (server returns only the caller's for a contractor)
  useEffect(() => {
    if (!ready) return;
    getExpenses(fy).then(setExpenses).catch((e) => setError(e.message || 'Failed to load'));
  }, [fy, ready]);

  const reloadList = () => getExpenses(fy).then(setExpenses);

  const submit = (e) => {
    e.preventDefault();
    setSaving(true);
    run(async () => {
      await createExpense({
        amount: form.amount,
        expenseDate: form.expenseDate,
        projectId: Number(form.projectId),
        categoryId: Number(form.categoryId),
        phaseId: form.phaseId ? Number(form.phaseId) : null,
        vendor: form.vendor || null,
        notes: form.notes || null,
      });
      setForm({ ...emptyForm(), projectId: form.projectId, categoryId: form.categoryId });
      await reloadList();
    }).finally(() => setSaving(false));
  };

  const remove = (id) => run(async () => {
    if (!window.confirm('Delete this expense?')) return;
    await deleteExpense(id);
    setExpenses((l) => l.filter((x) => x.id !== id));
  });

  const noProjects = projects.length === 0;
  const noCats = categories.length === 0;

  return (
    <>
      <div className="section-head">
        <h2>Expenses</h2>
        <FinancialYearSelect years={years} value={fy} onChange={setFy} />
      </div>
      <p className="muted">Add expenses for the projects you're assigned to. You can see and manage only your own entries.</p>
      {error && <p className="error">{error}</p>}
      {noProjects && <p className="muted">No projects are assigned to you yet. Please contact the owner.</p>}

      {!noProjects && (
        <form className="panel form-grid" onSubmit={submit}>
          <label>
            Project
            <select required value={form.projectId} onChange={(e) => set('projectId', e.target.value)}>
              {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </label>
          <label>
            Category
            <select required value={form.categoryId} onChange={(e) => set('categoryId', e.target.value)}>
              {noCats && <option value="">— none for this project —</option>}
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
            Amount
            <input type="number" step="0.01" min="0" required value={form.amount} onChange={(e) => set('amount', e.target.value)} />
          </label>
          <label>
            Date
            <input type="date" required max={todayIso()} value={form.expenseDate} onChange={(e) => set('expenseDate', e.target.value)} />
          </label>
          <label>
            Vendor
            <input type="text" value={form.vendor} onChange={(e) => set('vendor', e.target.value)} />
          </label>
          <label className="span-2">
            Notes
            <input type="text" value={form.notes} onChange={(e) => set('notes', e.target.value)} />
          </label>
          <div className="span-2">
            <button type="submit" disabled={saving || noCats}>{saving ? 'Saving…' : 'Add expense'}</button>
          </div>
        </form>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Date</th><th>Project</th><th>Category</th><th>Phase</th><th>Vendor</th><th className="num">Amount</th><th></th></tr>
          </thead>
          <tbody>
            {expenses.length === 0 && <tr><td colSpan="7" className="muted">No expenses in this period.</td></tr>}
            {expenses.map((x) => (
              <tr key={x.id}>
                <td>{x.expenseDate}</td>
                <td>{x.projectName}</td>
                <td>{x.categoryName}</td>
                <td>{x.phaseName || '—'}</td>
                <td>{x.vendor || '—'}</td>
                <td className="num">{money(x.amount, x.projectCurrency)}</td>
                <td><button className="link danger" onClick={() => remove(x.id)}>Delete</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
