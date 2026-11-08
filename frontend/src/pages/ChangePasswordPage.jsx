import React, { useState } from 'react';
import { api } from '../api/client.js';
import { ErrorBox, Field, Page, useAction } from '../components/ui.jsx';

/** Doi mat khau; bat buoc o lan dang nhap dau voi mat khau tam. */
export default function ChangePasswordPage({ forced, onDone }) {
  const [oldPassword, setOld] = useState('');
  const [newPassword, setNew] = useState('');
  const [confirm, setConfirm] = useState('');
  const [done, setDone] = useState(false);
  const { run, error, busy } = useAction();

  const submit = async (e) => {
    e.preventDefault();
    if (newPassword !== confirm) return;
    const ok = await run(() => api.post('/api/auth/password', { oldPassword, newPassword }));
    if (ok !== undefined) {
      setDone(true);
      onDone && onDone();
    }
  };

  return (
    <div className={forced ? 'login' : ''}>
      <Page title={forced ? 'Đổi mật khẩu tạm' : 'Đổi mật khẩu'}>
        <form onSubmit={submit} className="card narrow">
          <Field label="Mật khẩu hiện tại">
            <input type="password" value={oldPassword} onChange={(e) => setOld(e.target.value)} required />
          </Field>
          <Field label="Mật khẩu mới" hint="Tối thiểu 8 ký tự, gồm cả chữ và số">
            <input type="password" value={newPassword} onChange={(e) => setNew(e.target.value)} required />
          </Field>
          <Field label="Nhập lại mật khẩu mới">
            <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
          </Field>
          {confirm && confirm !== newPassword && <div className="error">Mật khẩu nhập lại không khớp</div>}
          <ErrorBox error={error} />
          {done && <div className="ok">Đã đổi mật khẩu.</div>}
          <button type="submit" disabled={busy}>Lưu</button>
        </form>
      </Page>
    </div>
  );
}
