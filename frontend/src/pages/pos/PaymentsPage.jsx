import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { dateTime, label, money, today } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Table, useLoad } from '../../components/ui.jsx';

/** UC3.5 Tra cuu giao dich thanh toan. */
export default function PaymentsPage() {
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [method, setMethod] = useState('');
  const [status, setStatus] = useState('');
  const q = `from=${from}&to=${to}${method ? `&method=${method}` : ''}${status ? `&status=${status}` : ''}`;
  const [rows, , error] = useLoad(() => api.get(`/api/payments?${q}`), [q]);
  return (
    <Page title="Giao dịch thanh toán">
      <div className="filters">
        <Field label="Từ ngày"><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
        <Field label="Đến ngày"><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
        <Field label="Hình thức">
          <select value={method} onChange={(e) => setMethod(e.target.value)}>
            <option value="">Tất cả</option>
            {['STUDENT_FACULTY_CARD', 'CASH', 'BANK_TRANSFER'].map((m) => <option key={m} value={m}>{label(m)}</option>)}
          </select>
        </Field>
        <Field label="Trạng thái">
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả</option>
            {['PENDING', 'SUCCESS', 'FAILED'].map((m) => <option key={m} value={m}>{label(m)}</option>)}
          </select>
        </Field>
      </div>
      <ErrorBox error={error} />
      <Table rows={rows} rowKey="code" columns={[
        { key: 'code', title: 'Mã GD' },
        { key: 'orderCode', title: 'Đơn' },
        { title: 'Hình thức', render: (p) => label(p.method) },
        { title: 'Số tiền', num: true, render: (p) => money(p.amount) },
        { title: 'Trạng thái', render: (p) => <Badge value={p.status} /> },
        { key: 'payerName', title: 'Người mua' },
        { key: 'confirmedByName', title: 'Người ghi nhận' },
        { title: 'Thời điểm', render: (p) => dateTime(p.transactionTime || p.createdAt) },
        { key: 'failureReason', title: 'Lý do' },
      ]} />
    </Page>
  );
}
