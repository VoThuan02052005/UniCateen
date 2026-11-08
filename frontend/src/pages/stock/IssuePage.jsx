import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { date, label, qty, today } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Panel, Table, useAction, useLoad } from '../../components/ui.jsx';

/**
 * UC4.4 Lap phieu xuat nguyen lieu theo ca: nhap so phan du kien, he thong goi y theo dinh muc va FEFO (BR34),
 * nhan vien sua luong thuc xuat roi xac nhan; ton bi tru khi xac nhan (GD5, BR36).
 */
export default function IssuePage() {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/stock/issues'));
  const [items] = useLoad(() => api.get('/api/catalog/menu-items'));
  const [draft, setDraft] = useState(null);
  const [current, setCurrent] = useState(null);
  const { run, error } = useAction();

  const create = (e) => {
    e.preventDefault();
    run(async () => {
      const issue = await api.post('/api/stock/issues', {
        issueDate: draft.issueDate, shift: draft.shift,
        plans: draft.plans.filter((p) => p.menuItemId && Number(p.plannedPortions) > 0)
          .map((p) => ({ menuItemId: Number(p.menuItemId), plannedPortions: Number(p.plannedPortions) })),
      });
      setDraft(null);
      setCurrent(issue);
      reload();
    });
  };
  const setPlan = (i, p) => setDraft({ ...draft, plans: draft.plans.map((x, j) => (j === i ? { ...x, ...p } : x)) });

  return (
    <Page title="Phiếu xuất nguyên liệu" actions={<button type="button" onClick={() => setDraft({ issueDate: today(), shift: 'LUNCH', plans: [{ menuItemId: '', plannedPortions: '' }] })}>Lập phiếu xuất</button>}>
      <ErrorBox error={loadError || error} />
      {draft && (
        <Panel title="Phiếu xuất mới: số phần dự kiến" onClose={() => setDraft(null)}>
          <form onSubmit={create}>
            <div className="form-grid">
              <Field label="Ngày"><input type="date" value={draft.issueDate} onChange={(e) => setDraft({ ...draft, issueDate: e.target.value })} /></Field>
              <Field label="Ca">
                <select value={draft.shift} onChange={(e) => setDraft({ ...draft, shift: e.target.value })}>
                  {['MORNING', 'LUNCH', 'EVENING'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
                </select>
              </Field>
            </div>
            {draft.plans.map((p, i) => (
              <div className="row" key={i}>
                <select value={p.menuItemId} onChange={(e) => setPlan(i, { menuItemId: e.target.value })}>
                  <option value="">Chọn món</option>
                  {(items || []).map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                </select>
                <input type="number" min="1" placeholder="Số phần" value={p.plannedPortions} onChange={(e) => setPlan(i, { plannedPortions: e.target.value })} />
              </div>
            ))}
            <div className="row">
              <button type="button" className="ghost" onClick={() => setDraft({ ...draft, plans: [...draft.plans, { menuItemId: '', plannedPortions: '' }] })}>Thêm món</button>
              <button type="submit">Tạo phiếu</button>
            </div>
          </form>
        </Panel>
      )}
      {current && <IssueDetail key={current.id} issueId={current.id} onClose={() => setCurrent(null)} onChanged={reload} />}
      <Table rows={rows} onRowClick={setCurrent} columns={[
        { key: 'code', title: 'Mã phiếu' },
        { title: 'Ngày', render: (r) => date(r.issueDate) },
        { title: 'Ca', render: (r) => label(r.shift) },
        { title: 'Món dự kiến', render: (r) => r.plans.map((p) => `${p.menuItemName} x${p.plannedPortions}`).join(', ') },
        { title: 'Trạng thái', render: (r) => <Badge value={r.status} /> },
      ]} />
    </Page>
  );
}

function IssueDetail({ issueId, onClose, onChanged }) {
  const [issue, reload] = useLoad(() => api.get(`/api/stock/issues/${issueId}`), [issueId]);
  const [lines, setLines] = useState(null);
  const [shortages, setShortages] = useState([]);
  const [noRecipe, setNoRecipe] = useState([]);
  const { run, error, busy } = useAction();
  if (!issue) return null;

  const suggest = () => run(async () => {
    const s = await api.get(`/api/stock/issues/${issueId}/suggestion`);
    setLines(s.lines.map((l) => ({ ...l, issuedQty: l.suggestedQty })));
    setShortages(s.shortages);
    setNoRecipe(s.itemsWithoutRecipe);
  });
  const confirm = () => run(async () => {
    await api.post(`/api/stock/issues/${issueId}/confirm`, {
      lines: lines.filter((l) => Number(l.issuedQty) > 0).map((l) => ({
        lotId: l.lotId, suggestedQty: l.suggestedQty, issuedQty: Number(l.issuedQty), outsideRecipe: false })),
    });
    setLines(null);
    reload();
    onChanged();
  });
  const cancel = () => run(async () => {
    await api.post(`/api/stock/issues/${issueId}/cancel`);
    reload();
    onChanged();
  });

  return (
    <Panel title={`Phiếu ${issue.code} · ${label(issue.shift)} ${date(issue.issueDate)}`} onClose={onClose}>
      <p><Badge value={issue.status} /> {issue.plans.map((p) => `${p.menuItemName} x${p.plannedPortions}`).join(', ')}</p>
      <ErrorBox error={error} />
      {issue.status === 'DRAFT' && !lines && (
        <div className="row">
          <button type="button" onClick={suggest} disabled={busy}>Gợi ý theo định mức (FEFO)</button>
          <button type="button" className="ghost" onClick={cancel} disabled={busy}>Hủy phiếu</button>
        </div>
      )}
      {lines && (
        <>
          {noRecipe.length > 0 && <div className="warn">Món chưa có công thức: {noRecipe.join(', ')}</div>}
          {shortages.map((s) => <div className="warn" key={s.ingredientId}>Thiếu {qty(s.missingQty)} {s.unit} {s.ingredientName}</div>)}
          <table className="lines">
            <thead><tr><th>Lô</th><th>Nguyên liệu</th><th>HSD</th><th>Còn</th><th>Gợi ý</th><th>Thực xuất</th></tr></thead>
            <tbody>
              {lines.map((l, i) => (
                <tr key={`${l.lotId}-${i}`}>
                  <td>{l.lotCode}</td><td>{l.ingredientName}</td><td>{date(l.expiryDate)}</td>
                  <td className="num">{qty(l.available)} {l.unit}</td><td className="num">{qty(l.suggestedQty)}</td>
                  <td><input type="number" min="0" step="0.001" max={l.available} value={l.issuedQty}
                             onChange={(e) => setLines(lines.map((x, j) => (j === i ? { ...x, issuedQty: e.target.value } : x)))} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          <button type="button" onClick={confirm} disabled={busy || lines.length === 0}>Xác nhận xuất (trừ tồn)</button>
        </>
      )}
      {issue.status === 'CONFIRMED' && (
        <Table rows={issue.lines} rowKey="lotId" columns={[
          { key: 'lotCode', title: 'Lô' }, { key: 'ingredientName', title: 'Nguyên liệu' },
          { title: 'Gợi ý', num: true, render: (l) => qty(l.suggestedQty) },
          { title: 'Thực xuất', num: true, render: (l) => `${qty(l.issuedQty)} ${l.unit}` },
        ]} />
      )}
    </Panel>
  );
}
