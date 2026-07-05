import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../shared/AuthContext';
import { CONTRACTOR_TABS } from '../shared/contractorTabs';

/** Contractor shell: nav is driven by the tabs the owner granted this account. */
export default function ContractorHome() {
  const { user, signOut } = useAuth();
  const tabs = CONTRACTOR_TABS.filter((t) => user.tabs?.includes(t.key));
  return (
    <div className="page">
      <header className="topbar">
        <strong>MyLedger</strong>
        <nav className="nav">
          {tabs.map((t) => (
            <NavLink key={t.key} to={t.path} className={({ isActive }) => (isActive ? 'active' : '')}>
              {t.label}
            </NavLink>
          ))}
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
