import { useEffect, useState } from 'react';
import {
  getProjects, createProject, updateProject, deleteProject,
  getCategories, createCategory, updateCategory, deleteCategory,
  getPhases, createPhase, updatePhase, deletePhase,
} from '../shared/api';
import { CURRENCIES } from '../shared/format';

function useError() {
  const [error, setError] = useState(null);
  const run = async (fn) => {
    setError(null);
    try { await fn(); } catch (e) { setError(e.message || 'Something went wrong'); }
  };
  return { error, run };
}

function ProjectsPanel({ projects, reload }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [currency, setCurrency] = useState('USD');
  const { error, run } = useError();

  const add = (e) => {
    e.preventDefault();
    run(async () => {
      await createProject({ name, description: description || null, currency });
      setName(''); setDescription(''); setCurrency('USD');
      await reload();
    });
  };
  const patch = (p, changes) =>
    run(async () => {
      await updateProject(p.id, {
        name: p.name, description: p.description, startDate: p.startDate,
        status: p.status, currency: p.currency, ...changes,
      });
      await reload();
    });
  const remove = (p) => run(async () => { await deleteProject(p.id); await reload(); });

  return (
    <section className="panel">
      <h3>Projects</h3>
      <p className="muted">Group spending by area — e.g. House Build, Healthcare, Home Maintenance. Each has its own currency.</p>
      {error && <p className="error">{error}</p>}
      <form className="inline-form" onSubmit={add}>
        <input placeholder="New project name" value={name} required onChange={(e) => setName(e.target.value)} />
        <input placeholder="Description" value={description} onChange={(e) => setDescription(e.target.value)} />
        <select value={currency} onChange={(e) => setCurrency(e.target.value)} title="Currency">
          {CURRENCIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <button type="submit">Add</button>
      </form>
      <ul className="manage-list">
        {projects.map((p) => (
          <li key={p.id} className={p.status === 'ARCHIVED' ? 'archived' : ''}>
            <span className="ml-name">{p.name}</span>
            <select value={p.currency} title="Currency"
                    onChange={(e) => patch(p, { currency: e.target.value })}>
              {(CURRENCIES.includes(p.currency) ? CURRENCIES : [p.currency, ...CURRENCIES])
                .map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            {p.status === 'ARCHIVED' && <span className="badge">archived</span>}
            <span className="ml-actions">
              {p.status === 'ACTIVE'
                ? <button className="link" onClick={() => patch(p, { status: 'ARCHIVED' })}>Archive</button>
                : <button className="link" onClick={() => patch(p, { status: 'ACTIVE' })}>Restore</button>}
              <button className="link danger" onClick={() => remove(p)}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function CategoriesPanel({ projectId }) {
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const [taxRelevant, setTaxRelevant] = useState(false);
  const { error, run } = useError();

  const load = () => (projectId ? getCategories(projectId).then(setItems) : Promise.resolve(setItems([])));
  useEffect(() => { run(load); }, [projectId]);

  const add = (e) => {
    e.preventDefault();
    run(async () => { await createCategory(projectId, { name, taxRelevant }); setName(''); setTaxRelevant(false); await load(); });
  };
  const setActive = (c, active) =>
    run(async () => { await updateCategory(c.id, { name: c.name, taxRelevant: c.taxRelevant, active }); await load(); });
  const remove = (c) => run(async () => { await deleteCategory(c.id); await load(); });

  return (
    <section className="panel">
      <h3>Categories</h3>
      <p className="muted">Labels for this project's expenses — e.g. Materials, Medicines. Mark tax-relevant ones for later reports.</p>
      {error && <p className="error">{error}</p>}
      <form className="inline-form" onSubmit={add}>
        <input placeholder="New category name" value={name} required onChange={(e) => setName(e.target.value)} disabled={!projectId} />
        <label className="chk">
          <input type="checkbox" checked={taxRelevant} onChange={(e) => setTaxRelevant(e.target.checked)} />
          Tax-relevant
        </label>
        <button type="submit" disabled={!projectId}>Add</button>
      </form>
      <ul className="manage-list">
        {items.length === 0 && <li className="muted">No categories for this project.</li>}
        {items.map((c) => (
          <li key={c.id} className={c.active ? '' : 'archived'}>
            <span className="ml-name">{c.name}</span>
            {c.taxRelevant && <span className="badge tax">tax</span>}
            {!c.active && <span className="badge">archived</span>}
            <span className="ml-actions">
              {c.active
                ? <button className="link" onClick={() => setActive(c, false)}>Archive</button>
                : <button className="link" onClick={() => setActive(c, true)}>Restore</button>}
              <button className="link danger" onClick={() => remove(c)}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function PhasesPanel({ projectId }) {
  const [items, setItems] = useState([]);
  const [name, setName] = useState('');
  const { error, run } = useError();

  const load = () => (projectId ? getPhases(projectId).then(setItems) : Promise.resolve(setItems([])));
  useEffect(() => { run(load); }, [projectId]);

  const add = (e) => {
    e.preventDefault();
    run(async () => { await createPhase(projectId, { name }); setName(''); await load(); });
  };
  const setActive = (ph, active) =>
    run(async () => { await updatePhase(ph.id, { name: ph.name, active }); await load(); });
  const remove = (ph) => run(async () => { await deletePhase(ph.id); await load(); });

  return (
    <section className="panel">
      <h3>Phases</h3>
      <p className="muted">Optional sub-divisions within this project — e.g. Foundation, Framing, Finishing.</p>
      {error && <p className="error">{error}</p>}
      <form className="inline-form" onSubmit={add}>
        <input placeholder="New phase name" value={name} required onChange={(e) => setName(e.target.value)} disabled={!projectId} />
        <button type="submit" disabled={!projectId}>Add</button>
      </form>
      <ul className="manage-list">
        {items.length === 0 && <li className="muted">No phases for this project.</li>}
        {items.map((ph) => (
          <li key={ph.id} className={ph.active ? '' : 'archived'}>
            <span className="ml-name">{ph.name}</span>
            {!ph.active && <span className="badge">archived</span>}
            <span className="ml-actions">
              {ph.active
                ? <button className="link" onClick={() => setActive(ph, false)}>Archive</button>
                : <button className="link" onClick={() => setActive(ph, true)}>Restore</button>}
              <button className="link danger" onClick={() => remove(ph)}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export default function Manage() {
  const [projects, setProjects] = useState([]);
  const [selectedPid, setSelectedPid] = useState('');

  const reloadProjects = () => getProjects().then(setProjects);
  useEffect(() => { reloadProjects(); }, []);

  const activeProjects = projects.filter((p) => p.status === 'ACTIVE');

  // Keep a valid project selected as the list changes.
  useEffect(() => {
    if (activeProjects.length === 0) { setSelectedPid(''); return; }
    if (!activeProjects.some((p) => p.id === selectedPid)) {
      setSelectedPid(activeProjects[0].id);
    }
  }, [projects]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <h2>Manage</h2>
      <ProjectsPanel projects={projects} reload={reloadProjects} />

      <section className="panel">
        <div className="section-head" style={{ marginBottom: '0.5rem' }}>
          <h3 style={{ margin: 0 }}>Project setup</h3>
          <label className="fy-select">
            <span>Project</span>
            <select value={selectedPid} onChange={(e) => setSelectedPid(Number(e.target.value))}>
              {activeProjects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </label>
        </div>
        <p className="muted">Categories and phases below belong to the selected project.</p>
      </section>

      <div className="grid-2">
        <CategoriesPanel projectId={selectedPid} />
        <PhasesPanel projectId={selectedPid} />
      </div>
    </>
  );
}
