import React, { useCallback, useEffect, useState } from 'react';
import { label } from '../format.js';

/** Nap du lieu khi mo trang; tra ve [data, reload, error, loading]. */
export function useLoad(fn, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const reload = useCallback(async () => {
    setLoading(true);
    try {
      setData(await fn());
      setError(null);
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => {
    reload();
  }, [reload]);
  return [data, reload, error, loading];
}

/** Bao mot thao tac: hien loi nghiep vu (ma BR) neu that bai. */
export function useAction() {
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const run = async (fn) => {
    setBusy(true);
    setError(null);
    try {
      return await fn();
    } catch (e) {
      setError(e);
      return undefined;
    } finally {
      setBusy(false);
    }
  };
  return { run, error, busy, clear: () => setError(null) };
}

export function ErrorBox({ error }) {
  if (!error) return null;
  return (
    <div className="error" role="alert">
      {error.code && error.code !== 'INVALID' ? <strong>[{error.code}] </strong> : null}
      {error.message}
      {error.fields && (
        <ul>
          {Object.entries(error.fields).map(([f, m]) => (
            <li key={f}>{f}: {m}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function Badge({ value }) {
  return <span className={`badge b-${String(value).toLowerCase()}`}>{label(value)}</span>;
}

export function Page({ title, actions, children }) {
  return (
    <section className="page">
      <header className="page-head">
        <h1>{title}</h1>
        <div className="actions">{actions}</div>
      </header>
      {children}
    </section>
  );
}

export function Field({ label: text, children, hint }) {
  return (
    <label className="field">
      <span>{text}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

/** Bang du lieu don gian: columns = [{key, title, render?}] */
export function Table({ columns, rows, rowKey = 'id', empty = 'Không có dữ liệu', onRowClick, selected }) {
  if (!rows) return <p className="muted">Đang tải...</p>;
  if (rows.length === 0) return <p className="muted">{empty}</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((c) => <th key={c.key || c.title} className={c.num ? 'num' : ''}>{c.title}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r[rowKey] ?? i} onClick={onRowClick ? () => onRowClick(r) : undefined}
                className={(onRowClick ? 'clickable ' : '') + (selected && selected === r[rowKey] ? 'selected' : '')}>
              {columns.map((c) => (
                <td key={c.key || c.title} className={c.num ? 'num' : ''}>{c.render ? c.render(r) : r[c.key]}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Form nho dang hop thoai tren cung trang. */
export function Panel({ title, onClose, children }) {
  return (
    <div className="panel">
      <div className="panel-head">
        <h2>{title}</h2>
        {onClose && <button type="button" className="ghost" onClick={onClose}>Đóng</button>}
      </div>
      {children}
    </div>
  );
}

export function StatusSelect({ value, options, onChange, disabled }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)} disabled={disabled}>
      {options.map((o) => <option key={o} value={o}>{label(o)}</option>)}
    </select>
  );
}
