import { useEffect, useState } from 'react';
import { getDashboard } from '../shared/api';
import { money } from '../shared/format';
import { useFinancialYears } from '../shared/useFinancialYears';
import FinancialYearSelect from '../shared/FinancialYearSelect';

function Breakdown({ title, rows }) {
  const max = rows.reduce((m, r) => Math.max(m, Number(r.total)), 0) || 1;
  return (
    <section className="panel">
      <h3>{title}</h3>
      {rows.length === 0 ? (
        <p className="muted">No data yet.</p>
      ) : (
        <ul className="bars">
          {rows.map((r) => (
            <li key={r.name}>
              <div className="bar-row">
                <span className="bar-label">{r.name}</span>
                <span className="bar-value">{money(r.total)}</span>
              </div>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${(Number(r.total) / max) * 100}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default function Dashboard() {
  const { years, fy, setFy, ready } = useFinancialYears();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!ready) return;
    setData(null);
    getDashboard(fy).then(setData).catch((e) => setError(e.message || 'Failed to load dashboard'));
  }, [fy, ready]);

  return (
    <>
      <div className="section-head">
        <h2>Dashboard</h2>
        <FinancialYearSelect years={years} value={fy} onChange={setFy} />
      </div>
      {error && <p className="error">{error}</p>}
      {!data ? (
        <p className="muted">Loading…</p>
      ) : (
        <>
          <p className="muted">Showing: {data.scopeLabel}</p>
          <div className="stat-row">
            <div className="stat">
              <div className="stat-label">Total spend</div>
              <div className="stat-value">{money(data.totalSpend)}</div>
            </div>
            <div className="stat">
              <div className="stat-label">Expenses logged</div>
              <div className="stat-value">{data.expenseCount}</div>
            </div>
          </div>
          <div className="grid-2">
            <Breakdown title="By category" rows={data.byCategory} />
            <Breakdown title="By project" rows={data.byProject} />
          </div>
          {data.byPhase && data.byPhase.length > 0 && (
            <Breakdown title="By phase" rows={data.byPhase} />
          )}
          <Breakdown title="By month" rows={data.byMonth} />
        </>
      )}
    </>
  );
}
