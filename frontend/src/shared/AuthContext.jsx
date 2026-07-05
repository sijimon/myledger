import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import * as apiClient from './api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);   // { email, role, userId } | null
  const [loading, setLoading] = useState(true);

  // On mount, try to restore a session via the refresh cookie.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const refreshed = await apiClient.tryRefresh();
      if (!cancelled) {
        if (refreshed) {
          try {
            const me = await apiClient.fetchMe();
            setUser(me);
          } catch {
            setUser(null);
          }
        }
        setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  async function signIn(email, password) {
    await apiClient.login(email, password);
    const me = await apiClient.fetchMe();
    setUser(me);
    return me;
  }

  async function signOut() {
    await apiClient.logout();
    setUser(null);
  }

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
