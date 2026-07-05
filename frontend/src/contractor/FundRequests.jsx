import FundRequestComposer from '../shared/FundRequestComposer';

export default function FundRequests() {
  return (
    <>
      <h2>Fund Requests</h2>
      <p className="muted">Raise a request for a project, itemise it, and submit for the owner to review.</p>
      <FundRequestComposer />
    </>
  );
}
