import { useEffect, useState } from 'react';
import { getReports } from './api';
import { money } from './format';
import StatusBadge from './StatusBadge';
import { useFinancialYears } from './useFinancialYears';
import FinancialYearSelect from './FinancialYearSelect';

function Breakdown({ title, rows, currency }) {
  const max = rows.reduce((m, r) => Math.max(m, Number(r.total)), 0) || 1;
  return (
    <section className="panel">
      <h4 style={{ marginTop: 0 }}>{title}</h4>
      {rows.length === 0 ? (
        <p className="muted">No data.</p>
      ) : (
        <ul className="bars">
          {rows.map((r) => (
            <li key={r.name}>
              <div className="bar-row"><span className="bar-label">{r.name}</span><span className="bar-value">{money(r.total, currency)}</span></div>
              <div className="bar-track"><div className="bar-fill" style={{ width: `${(Number(r.total) / max) * 100}%` }} /></div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/** Per-project reports. The server scopes data by role (owner: all; contractor: own). */
export default function ReportsView() {
  const { years, fy, setFy, ready } = useFinancialYears();
  const [reports, setReports] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!ready) return;
    setReports(null);
    getReports(fy).then(setReports).catch((e) => setError(e.message || 'Failed to load reports'));
  }, [fy, ready]);

  return (
    <>
      <div className="section-head">
        <h2>Reports</h2>
        <FinancialYearSelect years={years} value={fy} onChange={setFy} />
      </div>
      {error && <p className="error">{error}</p>}
      {!reports ? (
        <p className="muted">Loading…</p>
      ) : reports.length === 0 ? (
        <p className="muted">No projects to report on.</p>
      ) : (
        reports.map((r) => (
          <div key={r.projectId} className="report-block">
            <div className="section-head">
              <h3 style={{ margin: 0 }}>{r.projectName} <span className="chip">{r.currency}</span></h3>
            </div>
            <div className="stat-row">
              <div className="stat">
                <div className="stat-label">Expenses</div>
                <div className="stat-value">{money(r.expenseTotal, r.currency)}</div>
                <small className="muted">{r.expenseCount} {r.expenseCount === 1 ? 'entry' : 'entries'}</small>
              </div>
              <div className="stat">
                <div className="stat-label">Fund requests</div>
                <div className="stat-value">{money(r.fundRequestTotal, r.currency)}</div>
                <small className="muted">{r.fundRequestCount} request{r.fundRequestCount === 1 ? '' : 's'}</small>
              </div>
            </div>
            <div className="grid-2">
              <Breakdown title="Expenses by category" rows={r.expenseByCategory} currency={r.currency} />
              <Breakdown title="Expenses by phase" rows={r.expenseByPhase} currency={r.currency} />
            </div>
            {r.fundRequestByStatus.length > 0 && (
              <div className="flow" style={{ marginTop: '0.5rem' }}>
                {r.fundRequestByStatus.map((s) => (
                  <span key={s.status} className="flow-step">
                    <StatusBadge status={s.status} /> {s.count} · {money(s.total, r.currency)}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))
      )}
    </>
  );
}
