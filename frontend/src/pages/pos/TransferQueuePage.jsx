import React from 'react';
import { api } from '../../api/client.js';
import { dateTime, money } from '../../format.js';
import { ErrorBox, Page, Table, useAction, useLoad } from '../../components/ui.jsx';

/** UC3.4 Xac nhan giao dich chuyen khoan; qua 15 phut duoc danh dau Can xu ly (BR27). */
export default function TransferQueuePage() {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/payments/transfers/pending'));
  const { run, error, busy } = useAction();
  const confirm = (p) => run(async () => {
    await api.post(`/api/payments/${p.code}/confirm`);
    reload();
  });
  const fail = (p) => {
    const reason = window.prompt('Lý do chuyển sang thất bại:');
    if (!reason) return;
    run(async () => {
      await api.post(`/api/payments/${p.code}/fail`, { reason });
      reload();
    });
  };
  return (
    <Page title="Chờ xác nhận chuyển khoản" actions={<button type="button" className="ghost" onClick={reload}>Tải lại</button>}>
      <ErrorBox error={loadError || error} />
      <Table rows={rows} rowKey="code" empty="Không có giao dịch chờ" columns={[
        { key: 'code', title: 'Mã GD' },
        { key: 'orderCode', title: 'Đơn' },
        { title: 'Nội dung CK', render: (p) => <code>{p.referenceCode}</code> },
        { title: 'Số tiền', num: true, render: (p) => money(p.amount) },
        { title: 'Tạo lúc', render: (p) => dateTime(p.createdAt) },
        { title: '', render: (p) => (p.needsAttention ? <span className="badge b-failed">Cần xử lý</span> : '') },
        { title: '', render: (p) => (
          <span className="row">
            <button type="button" disabled={busy} onClick={() => confirm(p)}>Đã nhận</button>
            <button type="button" className="ghost" disabled={busy} onClick={() => fail(p)}>Thất bại</button>
          </span>) },
      ]} />
    </Page>
  );
}
