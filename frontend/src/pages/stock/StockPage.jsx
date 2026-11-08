import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { date, dateTime, qty } from '../../format.js';
import { ErrorBox, Field, Page, Panel, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC4.3 Theo doi ton kho va han su dung; canh bao (BR38); dieu chinh ton chi quan ly (BR37). */
export default function StockPage({ me }) {
  const manager = me.role === 'MANAGER';
  const [levels, reloadLevels, e1] = useLoad(() => api.get('/api/stock/levels'));
  const [lots, reloadLots, e2] = useLoad(() => api.get('/api/stock/lots'));
  const [adjustments, reloadAdj] = useLoad(() => api.get('/api/stock/adjustments'));
  const [adjust, setAdjust] = useState(null);
  const { run, error } = useAction();

  const save = (e) => {
    e.preventDefault();
    run(async () => {
      await api.post('/api/stock/adjustments', { lotId: adjust.id, actualQty: Number(adjust.actualQty), reason: adjust.reason });
      setAdjust(null);
      reloadLevels();
      reloadLots();
      reloadAdj();
    });
  };

  return (
    <Page title="Tồn kho và hạn sử dụng">
      <ErrorBox error={e1 || e2 || error} />
      <h2>Tồn theo nguyên liệu</h2>
      <Table rows={levels} rowKey="ingredientId" columns={[
        { key: 'code', title: 'Mã' }, { key: 'name', title: 'Nguyên liệu' }, { key: 'groupName', title: 'Nhóm' },
        { title: 'Tồn khả dụng', num: true, render: (l) => `${qty(l.available)} ${l.unit}` },
        { title: 'Tồn tối thiểu', num: true, render: (l) => qty(l.minStock) },
        { title: 'Quá hạn', num: true, render: (l) => (Number(l.expiredQty) > 0 ? qty(l.expiredQty) : '') },
        { title: 'Cảnh báo', render: (l) => (
          <span className="row">
            {l.low && <span className="badge b-failed">Tồn thấp</span>}
            {l.nearExpiryLots > 0 && <span className="badge b-pending">{l.nearExpiryLots} lô sắp hết hạn</span>}
          </span>) },
      ]} />
      <h2>Lô đang còn hàng (FEFO)</h2>
      {adjust && (
        <Panel title={`Điều chỉnh tồn lô ${adjust.code}`} onClose={() => setAdjust(null)}>
          <form onSubmit={save} className="form-grid">
            <Field label="Tồn hệ thống"><input value={`${qty(adjust.remainingQty)} ${adjust.unit}`} disabled /></Field>
            <Field label="Tồn thực tế"><input type="number" min="0" step="0.001" value={adjust.actualQty} onChange={(e) => setAdjust({ ...adjust, actualQty: e.target.value })} required /></Field>
            <Field label="Lý do (bắt buộc)"><input value={adjust.reason} onChange={(e) => setAdjust({ ...adjust, reason: e.target.value })} required /></Field>
            <button type="submit">Lưu điều chỉnh</button>
          </form>
        </Panel>
      )}
      <Table rows={lots} columns={[
        { key: 'code', title: 'Lô' }, { key: 'ingredientName', title: 'Nguyên liệu' },
        { title: 'Ngày nhập', render: (l) => date(l.receivedDate) },
        { title: 'Hạn sử dụng', render: (l) => (
          <span className={l.expired ? 'text-bad' : l.nearExpiry ? 'text-warn' : ''}>{date(l.expiryDate) || 'Không'}{l.expired ? ' (quá hạn)' : ''}</span>) },
        { title: 'Còn lại', num: true, render: (l) => `${qty(l.remainingQty)} ${l.unit}` },
        { title: '', render: (l) => manager && <button type="button" className="ghost" onClick={() => setAdjust({ ...l, actualQty: l.remainingQty, reason: '' })}>Điều chỉnh</button> },
      ]} />
      <h2>Phiếu điều chỉnh</h2>
      <Table rows={adjustments} columns={[
        { key: 'code', title: 'Mã' }, { key: 'lotCode', title: 'Lô' }, { key: 'ingredientName', title: 'Nguyên liệu' },
        { title: 'Trước', num: true, render: (a) => qty(a.qtyBefore) }, { title: 'Sau', num: true, render: (a) => qty(a.qtyAfter) },
        { key: 'reason', title: 'Lý do' }, { key: 'createdBy', title: 'Người lập' },
        { title: 'Thời điểm', render: (a) => dateTime(a.createdAt) },
      ]} />
    </Page>
  );
}
