import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { dateTime, label } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Panel, Table, useAction, useLoad } from '../../components/ui.jsx';

const ROLES = ['ADMIN', 'MANAGER', 'CASHIER', 'INVENTORY_STAFF', 'KITCHEN_STAFF', 'STUDENT', 'FACULTY'];

/** UC5.2 Quan ly tai khoan, UC5.3 Phan quyen (BR40, BR44, BR45). */
export default function AccountPage({ me }) {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/accounts'));
  const [edit, setEdit] = useState(null);
  const [secret, setSecret] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const { run, error } = useAction();

  const save = (e) => {
    e.preventDefault();
    run(async () => {
      if (edit.id) {
        await api.put(`/api/accounts/${edit.id}`, { fullName: edit.fullName, phone: edit.phone || '', cardCode: edit.cardCode || '' });
      } else {
        const created = await api.post('/api/accounts', { ...edit, phone: edit.phone || '', cardCode: edit.cardCode || '' });
        setSecret({ email: created.account.email, password: created.temporaryPassword });
      }
      setEdit(null);
      reload();
    });
  };
  const changeRole = (u, role) => window.confirm(`Đổi vai trò ${u.email} thành ${label(role)}?`) && run(async () => {
    await api.put(`/api/accounts/${u.id}/role`, { role });
    reload();
  });
  const toggleLock = (u) => run(async () => {
    await api.post(`/api/accounts/${u.id}/${u.status === 'ACTIVE' ? 'lock' : 'unlock'}`);
    reload();
  });
  const reset = (u) => window.confirm(`Đặt lại mật khẩu cho ${u.email}?`) && run(async () => {
    const r = await api.post(`/api/accounts/${u.id}/reset-password`);
    setSecret({ email: u.email, password: r.temporaryPassword });
  });
  const importCsv = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const fd = new FormData();
    fd.append('file', file);
    run(async () => {
      setImportResult(await api.post('/api/accounts/import', fd));
      reload();
    });
    e.target.value = '';
  };

  return (
    <Page title="Tài khoản và phân quyền" actions={(
      <span className="row">
        <label className="button ghost">Nhập CSV<input type="file" accept=".csv" hidden onChange={importCsv} /></label>
        <button type="button" onClick={() => setEdit({ email: '', fullName: '', phone: '', role: 'CASHIER', cardCode: '' })}>Thêm tài khoản</button>
      </span>)}>
      <ErrorBox error={loadError || error} />
      {secret && (
        <div className="ok">Mật khẩu tạm của {secret.email}: <code>{secret.password}</code> (chỉ hiện một lần, người dùng phải đổi khi đăng nhập).
          <button type="button" className="ghost" onClick={() => setSecret(null)}>Đã ghi lại</button></div>
      )}
      {importResult && (
        <div className="ok">Đã tạo {importResult.created}, bỏ qua {importResult.skipped} (email đã có).
          {importResult.errors.length > 0 && <ul>{importResult.errors.map((x) => <li key={x}>{x}</li>)}</ul>}</div>
      )}
      {edit && (
        <Panel title={edit.id ? 'Sửa tài khoản' : 'Thêm tài khoản'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="form-grid">
            <Field label="Email"><input type="email" value={edit.email} disabled={!!edit.id} onChange={(e) => setEdit({ ...edit, email: e.target.value })} required /></Field>
            <Field label="Họ tên"><input value={edit.fullName} onChange={(e) => setEdit({ ...edit, fullName: e.target.value })} required minLength={2} /></Field>
            <Field label="Số điện thoại"><input value={edit.phone || ''} pattern="0\d{9}" onChange={(e) => setEdit({ ...edit, phone: e.target.value })} /></Field>
            {!edit.id && (
              <Field label="Vai trò">
                <select value={edit.role} onChange={(e) => setEdit({ ...edit, role: e.target.value })}>
                  {ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
                </select>
              </Field>
            )}
            {['STUDENT', 'FACULTY'].includes(edit.role) && (
              <Field label="Mã thẻ"><input value={edit.cardCode || ''} onChange={(e) => setEdit({ ...edit, cardCode: e.target.value })} /></Field>
            )}
            <button type="submit">Lưu</button>
          </form>
        </Panel>
      )}
      <Table rows={rows} columns={[
        { key: 'email', title: 'Email' }, { key: 'fullName', title: 'Họ tên' },
        { title: 'Vai trò', render: (u) => (
          <select value={u.role} disabled={u.id === me.id} onChange={(e) => changeRole(u, e.target.value)}>
            {ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
          </select>) },
        { key: 'cardCode', title: 'Mã thẻ' },
        { title: 'Trạng thái', render: (u) => <Badge value={u.status} /> },
        { title: 'Đăng nhập gần nhất', render: (u) => dateTime(u.lastLoginAt) },
        { title: '', render: (u) => (
          <span className="row">
            <button type="button" className="ghost" onClick={() => setEdit({ ...u })}>Sửa</button>
            {u.id !== me.id && <button type="button" className="ghost" onClick={() => toggleLock(u)}>{u.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}</button>}
            <button type="button" className="ghost" onClick={() => reset(u)}>Đặt lại MK</button>
          </span>) },
      ]} />
    </Page>
  );
}
