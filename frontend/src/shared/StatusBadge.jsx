/** Coloured badge for a fund-request status. */
export default function StatusBadge({ status }) {
  return <span className={`status status-${status.toLowerCase()}`}>{status}</span>;
}
