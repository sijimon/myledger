import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../shared/AuthContext';

/** Owner shell: top bar + section nav, with the active section rendered via <Outlet/>. */
export default function OwnerHome() {
  const { user, signOut } = useAuth();
  return (
    <div className="page">
      <header className="topbar">
        <strong>MyLedger</strong>
        <nav className="nav">
          <NavLink to="/owner" end className={({ isActive }) => (isActive ? 'active' : '')}>
            Dashboard
          </NavLink>
          <NavLink to="/owner/expenses" className={({ isActive }) => (isActive ? 'active' : '')}>
            Expenses
          </NavLink>
          <NavLink to="/owner/fund-requests" className={({ isActive }) => (isActive ? 'active' : '')}>
            Fund Requests
          </NavLink>
          <NavLink to="/owner/reports" className={({ isActive }) => (isActive ? 'active' : '')}>
            Reports
          </NavLink>
          <NavLink to="/owner/manage" className={({ isActive }) => (isActive ? 'active' : '')}>
            Manage
          </NavLink>
          <NavLink to="/owner/users" className={({ isActive }) => (isActive ? 'active' : '')}>
            Users
          </NavLink>
          <NavLink to="/owner/docs" className={({ isActive }) => (isActive ? 'active' : '')}>
            Docs
          </NavLink>
        </nav>
        <span className="muted">{user.email}</span>
        <button className="link" onClick={signOut}>Sign out</button>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
