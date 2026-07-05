import { useEffect, useState } from 'react';
import {
  getAllFundRequests, getFundRequest,
  approveFundRequest, rejectFundRequest, markFundPaid,
} from '../shared/api';
import { money } from '../shared/format';
import StatusBadge from '../shared/StatusBadge';
import FundRequestComposer from '../shared/FundRequestComposer';

export default function FundRequests() {
  const [requests, setRequests] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);
  const [showComposer, setShowComposer] = useState(false);

  const run = async (fn) => { setError(null); try { await fn(); } catch (e) { setError(e.message || 'Something went wrong'); } };
  const loadList = () => getAllFundRequests().then(setRequests);

  useEffect(() => { run(loadList); }, []);

  const open = (id) => run(async () => { setSelected(await getFundRequest(id)); });
  const act = (fn) => run(async () => {
    await fn(selected.id);
    setSelected(await getFundRequest(selected.id));
    await loadList();
  });

  return (
    <>
      <div className="section-head">
        <h2>Fund Requests</h2>
        <button className="link" onClick={() => setShowComposer((s) => !s)}>
          {showComposer ? 'Hide request form' : '+ Raise a request'}
        </button>
      </div>
      <p className="muted">Requests raised by contractors (and you). Review submitted ones and approve, reject, or mark paid.</p>
      {error && <p className="error">{error}</p>}

      {showComposer && (
        <div className="composer-wrap">
          <h3>Raise a request</h3>
          <FundRequestComposer onChange={loadList} />
        </div>
      )}

      <h3>All requests</h3>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Title</th><th>Project</th><th>Requested by</th><th>Status</th><th className="num">Total</th><th></th></tr>
          </thead>
          <tbody>
            {requests.length === 0 && <tr><td colSpan="6" className="muted">No fund requests yet.</td></tr>}
            {requests.map((r) => (
              <tr key={r.id}>
                <td>{r.title}</td>
                <td>{r.projectName}</td>
                <td>{r.requesterEmail}</td>
                <td><StatusBadge status={r.status} /></td>
                <td className="num">{money(r.total, r.projectCurrency)}</td>
                <td><button className="link" onClick={() => open(r.id)}>View</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selected && (
        <section className="panel">
          <div className="section-head">
            <h3 style={{ margin: 0 }}>{selected.title} <StatusBadge status={selected.status} /></h3>
            <div className="row-actions">
              {selected.status === 'SUBMITTED' && (
                <>
                  <button className="link" onClick={() => act(approveFundRequest)}>Approve</button>
                  <button className="link danger" onClick={() => act(rejectFundRequest)}>Reject</button>
                </>
              )}
              {selected.status === 'APPROVED' && (
                <button className="link" onClick={() => act(markFundPaid)}>Mark paid</button>
              )}
              <button className="link" onClick={() => setSelected(null)}>Close</button>
            </div>
          </div>
          <p className="muted">
            {selected.projectName} · by {selected.requesterEmail}
            {selected.note ? ` · ${selected.note}` : ''} · Total <strong>{money(selected.total, selected.projectCurrency)}</strong>
          </p>
          {selected.status === 'DRAFT' && <p className="muted">This request is still a draft — not yet submitted.</p>}

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Description</th><th className="num">Qty</th><th className="num">Unit price</th><th className="num">Amount</th></tr>
              </thead>
              <tbody>
                {(selected.items || []).length === 0 && (
                  <tr><td colSpan="4" className="muted">No line items.</td></tr>
                )}
                {(selected.items || []).map((it) => (
                  <tr key={it.id}>
                    <td>{it.description}</td>
                    <td className="num">{it.qty}</td>
                    <td className="num">{money(it.unitPrice, selected.projectCurrency)}</td>
                    <td className="num">{money(it.amount, selected.projectCurrency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </>
  );
}
