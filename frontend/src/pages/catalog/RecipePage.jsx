import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { dateTime, qty } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Panel, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC1.5 Cong thuc va dinh muc (GD7); moi mon toi da mot cong thuc dang ap dung (BR09). */
export default function RecipePage({ me }) {
  const manager = me.role === 'MANAGER';
  const [items] = useLoad(() => api.get('/api/catalog/menu-items'));
  const [itemId, setItemId] = useState(null);
  return (
    <Page title="Công thức và định mức">
      <div className="split">
        <div>
          <Table rows={items} selected={itemId} onRowClick={(i) => setItemId(i.id)} columns={[
            { key: 'code', title: 'Mã' }, { key: 'name', title: 'Món' }, { key: 'categoryName', title: 'Danh mục' },
          ]} />
        </div>
        <div>{itemId ? <RecipeDetail key={itemId} itemId={itemId} manager={manager} /> : <p className="muted">Chọn một món.</p>}</div>
      </div>
    </Page>
  );
}

function RecipeDetail({ itemId, manager }) {
  const [versions, reload, loadError] = useLoad(() => api.get(`/api/catalog/recipes/${itemId}`), [itemId]);
  const [ingredients] = useLoad(() => (manager ? api.get('/api/catalog/ingredients') : Promise.resolve([])));
  const [draft, setDraft] = useState(null);
  const { run, error } = useAction();
  const startNew = () => {
    const active = versions?.find((v) => v.status === 'ACTIVE');
    setDraft({ note: active?.note || '', activate: true,
      lines: active ? active.lines.map((l) => ({ ingredientId: l.ingredientId, qtyPerPortion: l.qtyPerPortion })) : [] });
  };
  const save = (e) => {
    e.preventDefault();
    run(async () => {
      await api.post(`/api/catalog/recipes/${itemId}`, {
        ...draft, lines: draft.lines.map((l) => ({ ingredientId: Number(l.ingredientId), qtyPerPortion: Number(l.qtyPerPortion) })) });
      setDraft(null);
      reload();
    });
  };
  const activate = (v) => run(async () => {
    await api.post(`/api/catalog/recipes/${itemId}/versions/${v.version}/activate`);
    reload();
  });
  const setLine = (i, patch) => setDraft({ ...draft, lines: draft.lines.map((l, j) => (j === i ? { ...l, ...patch } : l)) });

  return (
    <div>
      <ErrorBox error={loadError || error} />
      {manager && !draft && <button type="button" onClick={startNew}>Lập phiên bản mới</button>}
      {draft && (
        <Panel title="Phiên bản mới" onClose={() => setDraft(null)}>
          <form onSubmit={save}>
            {draft.lines.map((l, i) => (
              <div className="row" key={i}>
                <select value={l.ingredientId} onChange={(e) => setLine(i, { ingredientId: e.target.value })} required>
                  <option value="">Chọn nguyên liệu</option>
                  {(ingredients || []).map((g) => <option key={g.id} value={g.id}>{g.name} ({g.unit})</option>)}
                </select>
                <input type="number" min="0.001" step="0.001" value={l.qtyPerPortion} placeholder="Định mức / phần"
                       onChange={(e) => setLine(i, { qtyPerPortion: e.target.value })} required />
                <button type="button" className="ghost" onClick={() => setDraft({ ...draft, lines: draft.lines.filter((_, j) => j !== i) })}>Bỏ</button>
              </div>
            ))}
            <button type="button" className="ghost" onClick={() => setDraft({ ...draft, lines: [...draft.lines, { ingredientId: '', qtyPerPortion: '' }] })}>Thêm nguyên liệu</button>
            <Field label="Ghi chú chế biến"><textarea value={draft.note} onChange={(e) => setDraft({ ...draft, note: e.target.value })} /></Field>
            <Field label="Áp dụng ngay"><input type="checkbox" checked={draft.activate} onChange={(e) => setDraft({ ...draft, activate: e.target.checked })} /></Field>
            <button type="submit" disabled={draft.lines.length === 0}>Lưu phiên bản</button>
          </form>
        </Panel>
      )}
      {(versions || []).map((v) => (
        <div className="card" key={v.id}>
          <div className="row between">
            <strong>Phiên bản {v.version}</strong>
            <span className="row"><Badge value={v.status} /><small className="muted">{dateTime(v.createdAt)}</small>
              {manager && v.status !== 'ACTIVE' && <button type="button" className="ghost" onClick={() => activate(v)}>Áp dụng</button>}</span>
          </div>
          <Table rowKey="ingredientId" rows={v.lines} columns={[
            { key: 'ingredientName', title: 'Nguyên liệu' },
            { title: 'Định mức / phần', num: true, render: (l) => `${qty(l.qtyPerPortion)} ${l.unit}` },
          ]} />
          {v.note && <p className="muted">{v.note}</p>}
        </div>
      ))}
      {versions && versions.length === 0 && <p className="muted">Món chưa có công thức (ví dụ đồ uống bán nguyên chai).</p>}
    </div>
  );
}
