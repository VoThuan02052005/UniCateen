import React, { useState } from 'react';
import { api } from '../api/client.js';
import { ErrorBox, Field, useAction } from '../components/ui.jsx';

/** UC5.1 Dang nhap bang email va mat khau ung dung (GD1). */
export default function LoginPage({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { run, error, busy } = useAction();

  const submit = async (e) => {
    e.preventDefault();
    const me = await run(() => api.post('/api/auth/login', { email, password }));
    if (me) onLogin(me);
  };

  return (
    <div className="login">
      <form onSubmit={submit} className="card">
        <h1>UniCateen</h1>
        <p className="muted">Hệ thống quản lý canteen trường học</p>
        <Field label="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus required />
        </Field>
        <Field label="Mật khẩu">
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        <ErrorBox error={error} />
        <button type="submit" disabled={busy}>Đăng nhập</button>
        <p className="muted small">Không dùng mật khẩu email trường; mật khẩu ứng dụng do quản trị viên cấp.</p>
      </form>
    </div>
  );
}
