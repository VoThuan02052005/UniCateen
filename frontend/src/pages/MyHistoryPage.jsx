import React from 'react';
import { api } from '../api/client.js';
import { dateTime, money } from '../format.js';
import { Badge, ErrorBox, Page, Table, useLoad } from '../components/ui.jsx';

/** UC2.5 Lich su mua hang ca nhan: don gan voi tai khoan khi quet the, ca ba hinh thuc thanh toan. */
export default function MyHistoryPage() {
  const [orders, , error] = useLoad(() => api.get('/api/orders/mine'));
  return (
    <Page title="Lịch sử mua hàng">
      <p className="muted">Đơn hiện ở đây khi nhân viên quét thẻ sinh viên/giảng viên của bạn lúc thanh toán.</p>
      <ErrorBox error={error} />
      <Table rows={orders} columns={[
        { key: 'code', title: 'Mã đơn' },
        { title: 'Thời điểm', render: (o) => dateTime(o.paidAt || o.createdAt) },
        { title: 'Món', render: (o) => o.lines.map((l) => `${l.menuItemName} x${l.quantity}`).join(', ') },
        { title: 'Hình thức', render: (o) => (o.paymentMethod ? <Badge value={o.paymentMethod} /> : '') },
        { title: 'Tổng tiền', num: true, render: (o) => money(o.totalAmount) },
        { title: 'Trạng thái', render: (o) => <Badge value={o.status} /> },
      ]} />
    </Page>
  );
}
