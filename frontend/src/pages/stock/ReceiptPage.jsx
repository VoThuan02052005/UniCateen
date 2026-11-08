import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { date, money, qty, today } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Panel, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC4.2 Lap phieu nhap kho; xac nhan thi moi dong tao mot lo (GD6), sau xac nhan khong sua (BR31). */
export default function ReceiptPage() {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/stock/receipts'));
  const [suppliers] = useLoad(() => api.get('/api/stock/suppliers'));
  const [ingredients] = useLoad(() => api.get('/api/catalog/ingredients'));
  const [edit, setEdit] = useState(null);
  const [view, setView] = useState(null);
  const { run, error } = useAction();

  const blankLine = { ingredientId: '', quantity: '', unitCost: '', expiryDate: '' };
  const startNew = () => setEdit({ supplierId: suppliers?.find((s) => s.status === 'ACTIVE')?.id || '', receiptDate: today(), lines: [{ ...blankLine }] });
  const body = () => ({
    supplierId: Number(edit.supplierId), receiptDate: edit.receiptDate,
    lines: edit.lines.map((l) => ({ ingredientId: Number(l.ingredientId), quantity: Number(l.quantity),
      unitCost: Number(l.unitCost), expiryDate: l.expiryDate || null })),
  });
  const save = (e) => {
    e.preventDefault();
    run(async () => {
      await (edit.id ? api.put(`/api/stock/receipts/${edit.id}`, body()) : api.post('/api/stock/receipts', body()));
      setEdit(null);
      reload();
    });
  };
  const act = (r, what) => run(async () => {
    setView(await api.post(`/api/stock/receipts/${r.id}/${what}`));
    reload();
  });
  const setLine = (i, p) => setEdit({ ...edit, lines: edit.lines.map((l, j) => (j === i ? { ...l, ...p } : l)) });
  const total = edit ? edit.lines.reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitCost) || 0), 0) : 0;

  return (
    <Page title="Phiếu nhập kho" actions={<button type="button" onClick={startNew}>Lập phiếu nhập</button>}>
      <ErrorBox error={loadError || error} />
      {edit && (
        <Panel title={edit.id ? `Sửa phiếu ${edit.code}` : 'Phiếu nhập mới'} onClose={() => setEdit(null)}>
          <form onSubmit={save}>
            <div className="form-grid">
              <Field label="Nhà cung cấp">
                <select value={edit.supplierId} onChange={(e) => setEdit({ ...edit, supplierId: e.target.value })} required>
                  {(suppliers || []).filter((s) => s.status === 'ACTIVE').map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </Field>
              <Field label="Ngày nhập"><input type="date" value={edit.receiptDate} onChange={(e) => setEdit({ ...edit, receiptDate: e.target.value })} /></Field>
            </div>
            <table className="lines">
              <thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Đơn giá</th><th>Hạn sử dụng</th><th /></tr></thead>
              <tbody>
                {edit.lines.map((l, i) => {
                  const ing = (ingredients || []).find((g) => String(g.id) === String(l.ingredientId));
                  return (
                    <tr key={i}>
                      <td><select value={l.ingredientId} onChange={(e) => setLine(i, { ingredientId: e.target.value })} required>
                        <option value="">Chọn</option>
                        {(ingredients || []).filter((g) => g.status === 'ACTIVE').map((g) => <option key={g.id} value={g.id}>{g.name} ({g.unit})</option>)}
                      </select></td>
                      <td><input type="number" min="0.001" step="0.001" value={l.quantity} onChange={(e) => setLine(i, { quantity: e.target.value })} required /></td>
                      <td><input type="number" min="0" step="500" value={l.unitCost} onChange={(e) => setLine(i, { unitCost: e.target.value })} required /></td>
                      <td><input type="date" min={edit.receiptDate} value={l.expiryDate} required={!!ing?.trackExpiry} onChange={(e) => setLine(i, { expiryDate: e.target.value })} /></td>
                      <td><button type="button" className="ghost" onClick={() => setEdit({ ...edit, lines: edit.lines.filter((_, j) => j !== i) })}>Bỏ</button></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <div className="row between">
              <button type="button" className="ghost" onClick={() => setEdit({ ...edit, lines: [...edit.lines, { ...blankLine }] })}>Thêm dòng</button>
              <span>Tổng giá trị: <strong>{money(Math.round(total))}</strong></span>
              <button type="submit" disabled={edit.lines.length === 0}>Lưu nháp</button>
            </div>
          </form>
        </Panel>
      )}
      {view && (
        <Panel title={`Phiếu ${view.code}`} onClose={() => setView(null)}>
          <p>{view.supplierName} · {date(view.receiptDate)} · <Badge value={view.status} /></p>
          <Table rowKey="ingredientId" rows={view.lines} columns={[
            { key: 'ingredientName', title: 'Nguyên liệu' },
            { title: 'Số lượng', num: true, render: (l) => `${qty(l.quantity)} ${l.unit}` },
            { title: 'Đơn giá', num: true, render: (l) => money(l.unitCost) },
            { title: 'HSD', render: (l) => date(l.expiryDate) },
            { title: 'Thành tiền', num: true, render: (l) => money(l.lineTotal) },
          ]} />
        </Panel>
      )}
      <Table rows={rows} onRowClick={setView} columns={[
        { key: 'code', title: 'Mã phiếu' },
        { key: 'supplierName', title: 'Nhà cung cấp' },
        { title: 'Ngày nhập', render: (r) => date(r.receiptDate) },
        { title: 'Giá trị', num: true, render: (r) => money(r.totalValue) },
        { title: 'Trạng thái', render: (r) => <Badge value={r.status} /> },
        { title: '', render: (r) => r.status === 'DRAFT' && (
          <span className="row" onClick={(e) => e.stopPropagation()}>
            <button type="button" className="ghost" onClick={() => setEdit({ ...r, lines: r.lines.map((l) => ({ ...l, expiryDate: l.expiryDate || '' })) })}>Sửa</button>
            <button type="button" onClick={() => window.confirm('Xác nhận phiếu? Sau khi xác nhận không sửa được.') && act(r, 'confirm')}>Xác nhận</button>
            <button type="button" className="ghost" onClick={() => act(r, 'cancel')}>Hủy</button>
          </span>) },
      ]} />
    </Page>
  );
}
