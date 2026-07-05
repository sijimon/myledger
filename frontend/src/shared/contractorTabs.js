// Grantable contractor tabs (keys match the backend Tabs constants) and their routes.
export const CONTRACTOR_TABS = [
  { key: 'FUND_REQUESTS', label: 'Fund Requests', path: '/contractor/fund-requests' },
  { key: 'EXPENSES', label: 'Expenses', path: '/contractor/expenses' },
  { key: 'DASHBOARD', label: 'Dashboard', path: '/contractor/dashboard' },
  { key: 'REPORTS', label: 'Reports', path: '/contractor/reports' },
];

// Tab options for the owner's user-management UI.
export const TAB_OPTIONS = CONTRACTOR_TABS.map(({ key, label }) => ({ key, label }));

export const firstAllowedTab = (tabs) => CONTRACTOR_TABS.find((t) => tabs?.includes(t.key));
