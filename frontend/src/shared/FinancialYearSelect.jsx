/**
 * Financial-year picker. Value is the FY start-year, or the string 'all' for all time.
 * Presentational — the caller owns the years list and selected value.
 */
export default function FinancialYearSelect({ years, value, onChange }) {
  const current = value == null ? 'all' : String(value);
  return (
    <label className="fy-select">
      <span>Financial year</span>
      <select
        value={current}
        onChange={(e) => onChange(e.target.value === 'all' ? null : Number(e.target.value))}
      >
        {years.map((y) => (
          <option key={y.value} value={y.value}>
            {y.label}{y.current ? ' (current)' : ''}
          </option>
        ))}
        <option value="all">All time</option>
      </select>
    </label>
  );
}
