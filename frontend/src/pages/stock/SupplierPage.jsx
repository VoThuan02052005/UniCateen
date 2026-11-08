import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { ErrorBox, Field, Page, Panel, StatusSelect, Table, useAction, useLoad } from '../../components/ui.jsx';

const EMPTY = { name: '', taxCode: '', contactName: '', phone: '', email: '', address: '', mainGoods: '' };

/** UC4.1 Quan ly nha cung cap (BR30, DV07). */
export default function SupplierPage() {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/stock/suppliers'));
  const [edit, setEdit] = useState(null);
  const { run, error } = useAction();
  const save = (e) => {
    e.preventDefault();
    run(async () => {
      await (edit.id ? api.put(`/api/stock/suppliers/${edit.id}`, edit) : api.post('/api/stock/suppliers', edit));
      setEdit(null);
      reload();
    });
  };
  const setStatus = (s, status) => run(async () => {
    await api.patch(`/api/stock/suppliers/${s.id}/status`, { status });
    reload();
  });
  const remove = (s) => window.confirm(`Xóa nhà cung cấp ${s.name}?`) && run(async () => {
    await api.del(`/api/stock/suppliers/${s.id}`);
    reload();
  });
  const f = (k, props = {}) => (
    <input value={edit[k] || ''} onChange={(e) => setEdit({ ...edit, [k]: e.target.value })} {...props} />
  );
  return (
    <Page title="Nhà cung cấp" actions={<button type="button" onClick={() => setEdit({ ...EMPTY })}>Thêm nhà cung cấp</button>}>
      <ErrorBox error={loadError || error} />
      {edit && (
        <Panel title={edit.id ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="form-grid">
            <Field label="Tên">{f('name', { required: true, minLength: 2 })}</Field>
            <Field label="Số điện thoại" hint="10 chữ số, bắt đầu bằng 0">{f('phone', { required: true, pattern: '0\\d{9}' })}</Field>
            <Field label="Mã số thuế">{f('taxCode', { maxLength: 14 })}</Field>
            <Field label="Người liên hệ">{f('contactName')}</Field>
            <Field label="Email">{f('email', { type: 'email' })}</Field>
            <Field label="Địa chỉ">{f('address')}</Field>
            <Field label="Mặt hàng chính">{f('mainGoods')}</Field>
            <button type="submit">Lưu</button>
          </form>
        </Panel>
      )}
      <Table rows={rows} columns={[
        { key: 'code', title: 'Mã' }, { key: 'name', title: 'Tên' }, { key: 'phone', title: 'Điện thoại' },
        { key: 'taxCode', title: 'MST' }, { key: 'mainGoods', title: 'Mặt hàng' },
        { title: 'Trạng thái', render: (s) => <StatusSelect value={s.status} options={['ACTIVE', 'INACTIVE']} onChange={(v) => setStatus(s, v)} /> },
        { title: '', render: (s) => (
          <span className="row">
            <button type="button" className="ghost" onClick={() => setEdit({ ...s })}>Sửa</button>
            <button type="button" className="ghost" onClick={() => remove(s)}>Xóa</button>
          </span>) },
      ]} />
    </Page>
  );
}
