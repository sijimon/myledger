import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

/**
 * Route guard. Redirects unauthenticated users to /login and, when a role is
 * required, sends the wrong role to their own home. This is UX only — the server
 * independently enforces every permission.
 */
export default function RequireAuth({ role, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="center muted">Loading…</div>;
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  if (role && user.role !== role) {
    return <Navigate to={homeFor(user.role)} replace />;
  }
  return children;
}

export function homeFor(role) {
  return role === 'ROLE_OWNER' ? '/owner' : '/contractor';
}
