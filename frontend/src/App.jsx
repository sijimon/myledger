import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './shared/AuthContext';
import RequireAuth, { homeFor } from './shared/RequireAuth';
import Login from './pages/Login';
import OwnerHome from './owner/OwnerHome';
import Dashboard from './owner/Dashboard';
import Expenses from './owner/Expenses';
import Manage from './owner/Manage';
import Users from './owner/Users';
import Docs from './owner/Docs';
import OwnerFundRequests from './owner/FundRequests';
import ReportsView from './shared/ReportsView';
import ContractorHome from './contractor/ContractorHome';
import ContractorFundRequests from './contractor/FundRequests';
import ContractorExpenses from './contractor/ExpensesView';
import { CONTRACTOR_TABS, firstAllowedTab } from './shared/contractorTabs';

function RootRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <div className="center muted">Loading…</div>;
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />;
}

/** Contractor landing: go to the first tab they're allowed, or a message if none. */
function ContractorIndex() {
  const { user } = useAuth();
  const tab = firstAllowedTab(user.tabs);
  if (!tab) return <p className="muted">No sections have been enabled for your account yet. Please contact the owner.</p>;
  return <Navigate to={tab.path} replace />;
}

/** Guards a contractor tab route; bounces to their landing if the tab isn't granted. */
function TabGuard({ tab, children }) {
  const { user } = useAuth();
  if (!user.tabs?.includes(tab)) return <Navigate to="/contractor" replace />;
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/owner"
            element={
              <RequireAuth role="ROLE_OWNER">
                <OwnerHome />
              </RequireAuth>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="expenses" element={<Expenses />} />
            <Route path="fund-requests" element={<OwnerFundRequests />} />
            <Route path="reports" element={<ReportsView />} />
            <Route path="manage" element={<Manage />} />
            <Route path="users" element={<Users />} />
            <Route path="docs" element={<Docs />} />
          </Route>
          <Route
            path="/contractor"
            element={
              <RequireAuth role="ROLE_CONTRACTOR">
                <ContractorHome />
              </RequireAuth>
            }
          >
            <Route index element={<ContractorIndex />} />
            <Route path="fund-requests" element={<TabGuard tab="FUND_REQUESTS"><ContractorFundRequests /></TabGuard>} />
            <Route path="expenses" element={<TabGuard tab="EXPENSES"><ContractorExpenses /></TabGuard>} />
            <Route path="dashboard" element={<TabGuard tab="DASHBOARD"><Dashboard /></TabGuard>} />
            <Route path="reports" element={<TabGuard tab="REPORTS"><ReportsView /></TabGuard>} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
