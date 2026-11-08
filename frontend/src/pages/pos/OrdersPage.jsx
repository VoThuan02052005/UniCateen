import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { dateTime, label, money, today } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Panel, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC2.4 Tra cuu lich su ban hang; nhan vien ban hang chi thay don minh tao (BR17). */
export default function OrdersPage() {
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [status, setStatus] = useState('');
  const [orders, reload, error] = useLoad(
    () => api.get(`/api/orders?from=${from}&to=${to}${status ? `&status=${status}` : ''}`), [from, to, status]);
  const [selected, setSelected] = useState(null);

  return (
    <Page title="Đơn hàng">
      <div className="filters">
        <Field label="Từ ngày"><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
        <Field label="Đến ngày"><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
        <Field label="Trạng thái">
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả</option>
            {['DRAFT', 'PENDING_PAYMENT', 'PAID', 'CANCELLED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </Field>
      </div>
      <ErrorBox error={error} />
      <Table rows={orders} rowKey="code" selected={selected?.code} onRowClick={setSelected} columns={[
        { key: 'code', title: 'Mã đơn' },
        { title: 'Tạo lúc', render: (o) => dateTime(o.createdAt) },
        { key: 'cashierName', title: 'Nhân viên' },
        { title: 'Tổng tiền', num: true, render: (o) => money(o.totalAmount) },
        { title: 'Hình thức', render: (o) => (o.paymentMethod ? label(o.paymentMethod) : '') },
        { title: 'Trạng thái', render: (o) => <Badge value={o.status} /> },
      ]} />
      {selected && <OrderDetail code={selected.code} onClose={() => setSelected(null)} onChanged={reload} />}
    </Page>
  );
}

function OrderDetail({ code, onClose, onChanged }) {
  const [order, reload] = useLoad(() => api.get(`/api/orders/${code}`), [code]);
  const [payments] = useLoad(() => api.get(`/api/payments/by-order/${code}`), [code]);
  const { run, error, busy } = useAction();
  if (!order) return null;
  const cancel = () => {
    const reason = order.status === 'PENDING_PAYMENT' ? window.prompt('Lý do hủy đơn (bắt buộc):') : '';
    if (order.status === 'PENDING_PAYMENT' && !reason) return;
    run(async () => {
      await api.post(`/api/orders/${code}/cancel`, { reason });
      reload();
      onChanged();
    });
  };
  return (
    <Panel title={`Đơn ${code}`} onClose={onClose}>
      <Table rowKey="menuItemId" rows={order.lines} columns={[
        { key: 'menuItemName', title: 'Món' },
        { key: 'quantity', title: 'SL', num: true },
        { title: 'Đơn giá', num: true, render: (l) => money(l.unitPrice) },
        { title: 'Thành tiền', num: true, render: (l) => money(l.lineTotal) },
      ]} />
      <p>Tổng tiền: <strong>{money(order.totalAmount)}</strong>{order.customerName && <> · Người mua: {order.customerName}</>}</p>
      {order.cancelReason && <p>Lý do hủy: {order.cancelReason}</p>}
      <h3>Giao dịch thanh toán</h3>
      <Table rows={payments} empty="Chưa có giao dịch" columns={[
        { key: 'code', title: 'Mã GD' },
        { title: 'Hình thức', render: (p) => label(p.method) },
        { title: 'Trạng thái', render: (p) => <Badge value={p.status} /> },
        { title: 'Thời điểm', render: (p) => dateTime(p.transactionTime || p.createdAt) },
        { key: 'failureReason', title: 'Ghi chú' },
      ]} />
      <ErrorBox error={error} />
      {['DRAFT', 'PENDING_PAYMENT'].includes(order.status) && (
        <button type="button" className="ghost" onClick={cancel} disabled={busy}>Hủy đơn</button>
      )}
    </Panel>
  );
}
