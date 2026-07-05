// Minimal API client.
//
// The access token lives only in memory (never localStorage) to limit XSS blast radius.
// The refresh token is an httpOnly cookie the browser sends automatically to
// /api/auth/refresh. On a 401 we transparently try one refresh, then retry the request.

let accessToken = null;

export function setAccessToken(token) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

async function rawRequest(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && accessToken) headers['Authorization'] = `Bearer ${accessToken}`;

  return fetch(path, {
    method,
    headers,
    credentials: 'include', // send/receive the refresh cookie
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/** Attempt a silent refresh. Returns the new access token, or null if not logged in. */
export async function tryRefresh() {
  const res = await rawRequest('/api/auth/refresh', { method: 'POST', auth: false });
  if (!res.ok) {
    setAccessToken(null);
    return null;
  }
  const data = await res.json();
  setAccessToken(data.accessToken);
  return data;
}

/**
 * JSON request with automatic one-shot refresh-and-retry on 401.
 * Throws { status, message } on non-OK responses.
 */
export async function api(path, options = {}) {
  let res = await rawRequest(path, options);

  if (res.status === 401 && options.auth !== false) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      res = await rawRequest(path, options);
    }
  }

  if (!res.ok) {
    let message = res.statusText;
    try {
      const err = await res.json();
      message = err.error || err.message || message;
    } catch {
      /* non-JSON error body */
    }
    throw { status: res.status, message };
  }

  if (res.status === 204) return null;
  return res.json();
}

export async function login(email, password) {
  const data = await api('/api/auth/login', {
    method: 'POST',
    body: { email, password },
    auth: false,
  });
  setAccessToken(data.accessToken);
  return data;
}

export async function logout() {
  try {
    await api('/api/auth/logout', { method: 'POST', auth: false });
  } finally {
    setAccessToken(null);
  }
}

export function fetchMe() {
  return api('/api/me');
}

// --- M2: expenses, reference data, dashboard ---

const fyParam = (fy) => (fy == null ? '' : `?fy=${fy}`);

export const getExpenses = (fy) => api(`/api/expenses${fyParam(fy)}`);
export const createExpense = (payload) => api('/api/expenses', { method: 'POST', body: payload });
export const updateExpense = (id, payload) => api(`/api/expenses/${id}`, { method: 'PUT', body: payload });
export const deleteExpense = (id) => api(`/api/expenses/${id}`, { method: 'DELETE' });

export const getProjects = () => api('/api/projects');
export const getCategories = (projectId) => api(`/api/projects/${projectId}/categories`);
export const getDashboard = (fy) => api(`/api/dashboard/summary${fyParam(fy)}`);
export const getFinancialYears = () => api('/api/finance/years');
export const getReports = (fy) => api(`/api/reports/projects${fyParam(fy)}`);

// Project management
export const createProject = (payload) => api('/api/projects', { method: 'POST', body: payload });
export const updateProject = (id, payload) => api(`/api/projects/${id}`, { method: 'PUT', body: payload });
export const deleteProject = (id) => api(`/api/projects/${id}`, { method: 'DELETE' });

// Category management (per project)
export const createCategory = (projectId, payload) => api(`/api/projects/${projectId}/categories`, { method: 'POST', body: payload });
export const updateCategory = (id, payload) => api(`/api/categories/${id}`, { method: 'PUT', body: payload });
export const deleteCategory = (id) => api(`/api/categories/${id}`, { method: 'DELETE' });

// Reference data (both roles; contractor sees only assigned projects)
export const getReferenceProjects = () => api('/api/reference/projects');
export const getReferenceCategories = (projectId) => api(`/api/reference/projects/${projectId}/categories`);
export const getReferencePhases = (projectId) => api(`/api/reference/projects/${projectId}/phases`);

// Contractor project assignments (owner-only)
export const getUserProjects = (id) => api(`/api/users/${id}/projects`);
export const setUserProjects = (id, projectIds) => api(`/api/users/${id}/projects`, { method: 'PUT', body: { projectIds } });

// Fund requests — contractor
export const getMyFundRequests = () => api('/api/fund-requests/mine');
export const createFundRequest = (payload) => api('/api/fund-requests', { method: 'POST', body: payload });
export const getFundRequest = (id) => api(`/api/fund-requests/${id}`);
export const addFundItem = (id, payload) => api(`/api/fund-requests/${id}/items`, { method: 'POST', body: payload });
export const updateFundItem = (id, itemId, payload) => api(`/api/fund-requests/${id}/items/${itemId}`, { method: 'PUT', body: payload });
export const deleteFundItem = (id, itemId) => api(`/api/fund-requests/${id}/items/${itemId}`, { method: 'DELETE' });
export const submitFundRequest = (id) => api(`/api/fund-requests/${id}/submit`, { method: 'POST' });
export const deleteFundRequest = (id) => api(`/api/fund-requests/${id}`, { method: 'DELETE' });

// Fund requests — owner
export const getAllFundRequests = () => api('/api/fund-requests');
export const approveFundRequest = (id) => api(`/api/fund-requests/${id}/approve`, { method: 'POST' });
export const rejectFundRequest = (id) => api(`/api/fund-requests/${id}/reject`, { method: 'POST' });
export const markFundPaid = (id) => api(`/api/fund-requests/${id}/mark-paid`, { method: 'POST' });

// User management
export const getUsers = () => api('/api/users');
export const createUser = (payload) => api('/api/users', { method: 'POST', body: payload });
export const updateUser = (id, payload) => api(`/api/users/${id}`, { method: 'PUT', body: payload });
export const resetUserPassword = (id, password) => api(`/api/users/${id}/password`, { method: 'POST', body: { password } });
export const deleteUser = (id) => api(`/api/users/${id}`, { method: 'DELETE' });

// Phase management (per project)
export const getPhases = (projectId) => api(`/api/projects/${projectId}/phases`);
export const createPhase = (projectId, payload) => api(`/api/projects/${projectId}/phases`, { method: 'POST', body: payload });
export const updatePhase = (id, payload) => api(`/api/phases/${id}`, { method: 'PUT', body: payload });
export const deletePhase = (id) => api(`/api/phases/${id}`, { method: 'DELETE' });

/** URL for viewing/downloading a stored receipt (auth is via the access token header,
 *  so this is used programmatically, not as a plain <a href>). */
export const fileUrl = (id) => `/api/files/${id}`;

/** Multipart upload with the same refresh-and-retry behaviour as api(). */
export async function uploadFile(file) {
  const doUpload = () => {
    const form = new FormData();
    form.append('file', file);
    const headers = {};
    if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`;
    return fetch('/api/files/upload', { method: 'POST', headers, credentials: 'include', body: form });
  };

  let res = await doUpload();
  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (refreshed) res = await doUpload();
  }
  if (!res.ok) {
    let message = res.statusText;
    try {
      const err = await res.json();
      message = err.error || err.message || message;
    } catch {
      /* ignore */
    }
    throw { status: res.status, message };
  }
  return res.json();
}
