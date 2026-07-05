import { useEffect, useState } from 'react';
import {
  getUsers, createUser, updateUser, resetUserPassword, deleteUser,
  getProjects, getUserProjects, setUserProjects,
} from '../shared/api';
import { useAuth } from '../shared/AuthContext';
import { TAB_OPTIONS } from '../shared/contractorTabs';

const ROLES = [
  { value: 'ROLE_OWNER', label: 'Owner' },
  { value: 'ROLE_CONTRACTOR', label: 'Contractor' },
];

export default function Users() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('ROLE_CONTRACTOR');
  const [tabs, setTabs] = useState(['FUND_REQUESTS']);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  // Project-assignment editor for a contractor
  const [assignFor, setAssignFor] = useState(null); // { id, email }
  const [allProjects, setAllProjects] = useState([]);
  const [assigned, setAssigned] = useState([]);

  const load = () => getUsers().then(setUsers);
  const run = async (fn) => { setError(null); try { await fn(); } catch (e) { setError(e.message || 'Something went wrong'); } };

  useEffect(() => { run(load); }, []);

  const toggleNew = (key) =>
    setTabs((t) => (t.includes(key) ? t.filter((k) => k !== key) : [...t, key]));

  const add = async (e) => {
    e.preventDefault();
    setSaving(true);
    await run(async () => {
      await createUser({ email, password, role, tabs: role === 'ROLE_CONTRACTOR' ? tabs : undefined });
      setEmail(''); setPassword(''); setRole('ROLE_CONTRACTOR'); setTabs(['FUND_REQUESTS']);
      await load();
    });
    setSaving(false);
  };

  const changeRole = (u, newRole) => run(async () => { await updateUser(u.id, { role: newRole }); await load(); });
  const toggleEnabled = (u) => run(async () => { await updateUser(u.id, { enabled: !u.enabled }); await load(); });
  const toggleTab = (u, key) => run(async () => {
    const next = u.tabs.includes(key) ? u.tabs.filter((k) => k !== key) : [...u.tabs, key];
    await updateUser(u.id, { tabs: next });
    await load();
  });
  const remove = (u) => run(async () => {
    if (!window.confirm(`Delete user ${u.email}?`)) return;
    await deleteUser(u.id); await load();
  });
  const resetPw = (u) => run(async () => {
    const pw = window.prompt(`New password for ${u.email} (min 8 chars):`);
    if (!pw) return;
    await resetUserPassword(u.id, pw);
    window.alert('Password updated. The user must sign in again.');
  });

  const openAssign = (u) => run(async () => {
    const [projs, ids] = await Promise.all([getProjects(), getUserProjects(u.id)]);
    setAllProjects(projs.filter((p) => p.status === 'ACTIVE'));
    setAssigned(ids);
    setAssignFor({ id: u.id, email: u.email });
  });
  const toggleAssigned = (pid) =>
    setAssigned((a) => (a.includes(pid) ? a.filter((x) => x !== pid) : [...a, pid]));
  const saveAssign = () => run(async () => {
    await setUserProjects(assignFor.id, assigned);
    setAssignFor(null);
  });

  return (
    <>
      <h2>Users</h2>
      {error && <p className="error">{error}</p>}

      <form className="panel form-grid" onSubmit={add}>
        <label>
          Email
          <input type="email" required value={email} autoComplete="off" onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Temporary password
          <input type="text" required minLength={8} value={password} placeholder="min 8 characters"
                 onChange={(e) => setPassword(e.target.value)} />
        </label>
        <label>
          Role
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
          </select>
        </label>
        {role === 'ROLE_CONTRACTOR' && (
          <label>
            Visible tabs
            <div className="tab-check-row">
              {TAB_OPTIONS.map((t) => (
                <label key={t.key} className="chk">
                  <input type="checkbox" checked={tabs.includes(t.key)} onChange={() => toggleNew(t.key)} />
                  {t.label}
                </label>
              ))}
            </div>
          </label>
        )}
        <div className="span-2">
          <button type="submit" disabled={saving}>{saving ? 'Adding…' : 'Add user'}</button>
        </div>
      </form>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Email</th><th>Role</th><th>Visible tabs</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const self = u.id === me.userId;
              const contractor = u.role === 'ROLE_CONTRACTOR';
              return (
                <tr key={u.id} className={u.enabled ? '' : 'archived'}>
                  <td>{u.email}{self && <span className="badge" style={{ marginLeft: '0.5rem' }}>you</span>}</td>
                  <td>
                    <select value={u.role} disabled={self} onChange={(e) => changeRole(u, e.target.value)}>
                      {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                    </select>
                  </td>
                  <td>
                    {contractor ? (
                      <div className="tab-chip-row">
                        {TAB_OPTIONS.map((t) => (
                          <button key={t.key} type="button"
                                  className={`tab-chip${u.tabs.includes(t.key) ? ' on' : ''}`}
                                  onClick={() => toggleTab(u, t.key)}>
                            {t.label}
                          </button>
                        ))}
                      </div>
                    ) : <span className="muted">All</span>}
                  </td>
                  <td>{u.enabled ? 'Active' : 'Disabled'}</td>
                  <td className="row-actions">
                    {contractor && <button className="link" onClick={() => openAssign(u)}>Projects</button>}
                    <button className="link" onClick={() => resetPw(u)}>Reset password</button>
                    {!self && (
                      <>
                        <button className="link" onClick={() => toggleEnabled(u)}>{u.enabled ? 'Disable' : 'Enable'}</button>
                        <button className="link danger" onClick={() => remove(u)}>Delete</button>
                      </>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {assignFor && (
        <section className="panel">
          <div className="section-head">
            <h3 style={{ margin: 0 }}>Assign projects — {assignFor.email}</h3>
            <div className="row-actions">
              <button className="link" onClick={saveAssign}>Save</button>
              <button className="link" onClick={() => setAssignFor(null)}>Cancel</button>
            </div>
          </div>
          <p className="muted">The contractor can add expenses only for the projects checked here (needs the Expenses tab).</p>
          <div className="tab-check-row">
            {allProjects.length === 0 && <span className="muted">No active projects.</span>}
            {allProjects.map((p) => (
              <label key={p.id} className="chk">
                <input type="checkbox" checked={assigned.includes(p.id)} onChange={() => toggleAssigned(p.id)} />
                {p.name}
              </label>
            ))}
          </div>
        </section>
      )}

      <p className="muted">
        <strong>Owner</strong> sees everything. <strong>Contractor</strong> sees only the tabs you enable.
        With the <strong>Expenses</strong> tab + assigned projects, a contractor can add their own expenses
        for those projects (Dashboard is read-only).
      </p>
    </>
  );
}
