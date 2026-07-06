import { useEffect, useState } from 'react';
import {
  getUsers, createUser, updateUser, resetUserPassword, deleteUser,
  getProjects, getUserProjects, setUserProjects, getRoles,
} from '../shared/api';
import { useAuth } from '../shared/AuthContext';
import { TAB_OPTIONS } from '../shared/contractorTabs';

const OWNER = 'OWNER';
const tabLabel = (key) => TAB_OPTIONS.find((t) => t.key === key)?.label || key;

export default function Users() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [roleSel, setRoleSel] = useState(OWNER); // 'OWNER' or a role id (string)
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  // Project-assignment editor
  const [assignFor, setAssignFor] = useState(null);
  const [allProjects, setAllProjects] = useState([]);
  const [assigned, setAssigned] = useState([]);

  const load = () => Promise.all([getUsers(), getRoles()]).then(([u, r]) => { setUsers(u); setRoles(r); });
  const run = async (fn) => { setError(null); try { await fn(); } catch (e) { setError(e.message || 'Something went wrong'); } };
  useEffect(() => { run(load); }, []);

  // Build the payload for a chosen role selection.
  const rolePayload = (sel) => (sel === OWNER ? { role: 'ROLE_OWNER' } : { roleId: Number(sel) });

  const add = async (e) => {
    e.preventDefault();
    setSaving(true);
    await run(async () => {
      await createUser({ email, password, ...rolePayload(roleSel) });
      setEmail(''); setPassword(''); setRoleSel(OWNER);
      await load();
    });
    setSaving(false);
  };

  const changeRole = (u, sel) => run(async () => { await updateUser(u.id, rolePayload(sel)); await load(); });
  const toggleEnabled = (u) => run(async () => { await updateUser(u.id, { enabled: !u.enabled }); await load(); });
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
  const toggleAssigned = (pid) => setAssigned((a) => (a.includes(pid) ? a.filter((x) => x !== pid) : [...a, pid]));
  const saveAssign = () => run(async () => { await setUserProjects(assignFor.id, assigned); setAssignFor(null); });

  // Current dropdown value for a user row.
  const rowRoleValue = (u) => (u.role === 'ROLE_OWNER' ? OWNER : (u.roleId != null ? String(u.roleId) : ''));

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
          <select value={roleSel} onChange={(e) => setRoleSel(e.target.value)}>
            <option value={OWNER}>Owner (full access)</option>
            {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        </label>
        <div className="span-2">
          <button type="submit" disabled={saving}>{saving ? 'Adding…' : 'Add user'}</button>
        </div>
      </form>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Email</th><th>Role</th><th>Sees</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const self = u.id === me.userId;
              const owner = u.role === 'ROLE_OWNER';
              return (
                <tr key={u.id} className={u.enabled ? '' : 'archived'}>
                  <td>{u.email}{self && <span className="badge" style={{ marginLeft: '0.5rem' }}>you</span>}</td>
                  <td>
                    <select value={rowRoleValue(u)} disabled={self} onChange={(e) => changeRole(u, e.target.value)}>
                      {rowRoleValue(u) === '' && <option value="">— pick a role —</option>}
                      <option value={OWNER}>Owner</option>
                      {roles.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                    </select>
                  </td>
                  <td>
                    {owner ? <span className="muted">Everything</span>
                      : (u.tabs.length === 0 ? <span className="muted">—</span>
                        : <span className="tab-chip-row">{u.tabs.map((k) => <span key={k} className="tab-chip on">{tabLabel(k)}</span>)}</span>)}
                  </td>
                  <td>{u.enabled ? 'Active' : 'Disabled'}</td>
                  <td className="row-actions">
                    {!owner && <button className="link" onClick={() => openAssign(u)}>Projects</button>}
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
          <p className="muted">The user can add expenses only for the projects checked here (needs the Expenses tab in their role).</p>
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
        <strong>Owner</strong> sees everything. Other roles see only the tabs defined for that role
        (create/edit roles under <strong>Manage → Roles</strong>) and are scoped to their own data and assigned projects.
      </p>
    </>
  );
}
