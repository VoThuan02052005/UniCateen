import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { qty } from '../../format.js';
import { ErrorBox, Field, Page, Panel, StatusSelect, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC1.4 Quan ly nguyen lieu; don vi tinh co dinh khi da co giao dich kho (BR07). */
export default function IngredientPage({ me }) {
  const manager = me.role === 'MANAGER';
  const [rows, reload, loadError] = useLoad(() => api.get('/api/catalog/ingredients'));
  const [edit, setEdit] = useState(null);
  const { run, error } = useAction();
  const save = (e) => {
    e.preventDefault();
    const body = { ...edit, minStock: Number(edit.minStock) };
    run(async () => {
      await (edit.id ? api.put(`/api/catalog/ingredients/${edit.id}`, body) : api.post('/api/catalog/ingredients', body));
      setEdit(null);
      reload();
    });
  };
  const setStatus = (i, status) => run(async () => {
    await api.patch(`/api/catalog/ingredients/${i.id}/status`, { status });
    reload();
  });
  return (
    <Page title="Nguyên liệu" actions={manager && <button type="button" onClick={() => setEdit({ name: '', groupName: '', unit: 'kg', minStock: 0, trackExpiry: true })}>Thêm nguyên liệu</button>}>
      <ErrorBox error={loadError || error} />
      {edit && (
        <Panel title={edit.id ? 'Sửa nguyên liệu' : 'Thêm nguyên liệu'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="form-grid">
            <Field label="Tên"><input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} required minLength={2} /></Field>
            <Field label="Nhóm"><input value={edit.groupName} onChange={(e) => setEdit({ ...edit, groupName: e.target.value })} required /></Field>
            <Field label="Đơn vị tính" hint="Không đổi được khi đã có giao dịch kho"><input value={edit.unit} onChange={(e) => setEdit({ ...edit, unit: e.target.value })} required maxLength={10} /></Field>
            <Field label="Tồn tối thiểu"><input type="number" min="0" step="0.001" value={edit.minStock} onChange={(e) => setEdit({ ...edit, minStock: e.target.value })} /></Field>
            <Field label="Quản lý hạn sử dụng"><input type="checkbox" checked={edit.trackExpiry} onChange={(e) => setEdit({ ...edit, trackExpiry: e.target.checked })} /></Field>
            <Field label="Ghi chú bảo quản"><input value={edit.storageNote || ''} onChange={(e) => setEdit({ ...edit, storageNote: e.target.value })} /></Field>
            <button type="submit">Lưu</button>
          </form>
        </Panel>
      )}
      <Table rows={rows} columns={[
        { key: 'code', title: 'Mã' },
        { key: 'name', title: 'Tên' },
        { key: 'groupName', title: 'Nhóm' },
        { key: 'unit', title: 'ĐVT' },
        { title: 'Tồn tối thiểu', num: true, render: (i) => qty(i.minStock) },
        { title: 'Theo dõi HSD', render: (i) => (i.trackExpiry ? 'Có' : 'Không') },
        { title: 'Trạng thái', render: (i) => (manager ? <StatusSelect value={i.status} options={['ACTIVE', 'INACTIVE']} onChange={(s) => setStatus(i, s)} /> : i.status) },
        { title: '', render: (i) => manager && <button type="button" className="ghost" onClick={() => setEdit({ ...i })}>Sửa</button> },
      ]} />
    </Page>
  );
}
