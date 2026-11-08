import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { ErrorBox, Field, Page, Panel, StatusSelect, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC1.1 Quan ly danh muc mon; danh muc dang co mon chi Ngung su dung, khong xoa (BR01). */
export default function CategoryPage() {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/catalog/categories'));
  const [edit, setEdit] = useState(null);
  const { run, error } = useAction();

  const save = (e) => {
    e.preventDefault();
    const body = { name: edit.name, displayOrder: Number(edit.displayOrder), description: edit.description };
    run(async () => {
      await (edit.id ? api.put(`/api/catalog/categories/${edit.id}`, body) : api.post('/api/catalog/categories', body));
      setEdit(null);
      reload();
    });
  };
  const setStatus = (c, status) => run(async () => {
    await api.patch(`/api/catalog/categories/${c.id}/status`, { status });
    reload();
  });
  const remove = (c) => window.confirm(`Xóa danh mục ${c.name}?`) && run(async () => {
    await api.del(`/api/catalog/categories/${c.id}`);
    reload();
  });

  return (
    <Page title="Danh mục món" actions={<button type="button" onClick={() => setEdit({ name: '', displayOrder: (rows?.length || 0) + 1 })}>Thêm danh mục</button>}>
      <ErrorBox error={loadError || error} />
      {edit && (
        <Panel title={edit.id ? 'Sửa danh mục' : 'Thêm danh mục'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="form-grid">
            <Field label="Tên danh mục"><input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} required minLength={2} maxLength={100} /></Field>
            <Field label="Thứ tự hiển thị"><input type="number" min="0" value={edit.displayOrder} onChange={(e) => setEdit({ ...edit, displayOrder: e.target.value })} /></Field>
            <Field label="Mô tả"><input value={edit.description || ''} onChange={(e) => setEdit({ ...edit, description: e.target.value })} /></Field>
            <button type="submit">Lưu</button>
          </form>
        </Panel>
      )}
      <Table rows={rows} columns={[
        { key: 'code', title: 'Mã' },
        { key: 'name', title: 'Tên' },
        { key: 'displayOrder', title: 'Thứ tự', num: true },
        { key: 'itemCount', title: 'Số món', num: true },
        { title: 'Trạng thái', render: (c) => <StatusSelect value={c.status} options={['ACTIVE', 'INACTIVE']} onChange={(s) => setStatus(c, s)} /> },
        { title: '', render: (c) => (
          <span className="row">
            <button type="button" className="ghost" onClick={() => setEdit(c)}>Sửa</button>
            {c.itemCount === 0 && <button type="button" className="ghost" onClick={() => remove(c)}>Xóa</button>}
          </span>) },
      ]} />
    </Page>
  );
}
