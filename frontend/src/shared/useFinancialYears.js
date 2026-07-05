import { useEffect, useState } from 'react';
import { getFinancialYears } from './api';

/**
 * Loads the available financial years and tracks the selected one (defaults to the
 * current FY). `fy` is the selected start-year, or null for "All time".
 */
export function useFinancialYears() {
  const [years, setYears] = useState([]);
  const [fy, setFy] = useState(undefined); // undefined = not yet initialised

  useEffect(() => {
    getFinancialYears()
      .then((res) => {
        setYears(res.years);
        setFy(res.current.value);
      })
      .catch(() => setFy(null));
  }, []);

  return { years, fy, setFy, ready: fy !== undefined };
}
