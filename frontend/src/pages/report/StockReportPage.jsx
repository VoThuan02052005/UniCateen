import React, { useState } from 'react';
import { api, download } from '../../api/client.js';
import { qty, today } from '../../format.js';
import { ErrorBox, Field, Page, Table, useLoad } from '../../components/ui.jsx';

/** UC5.5 Bao cao nhap - xuat - ton theo nguyen lieu. */
export default function StockReportPage() {
  const [from, setFrom] = useState(`${today().slice(0, 8)}01`);
  const [to, setTo] = useState(today());
  const [rows, , error] = useLoad(() => api.get(`/api/reports/stock?from=${from}&to=${to}`), [from, to]);
  return (
    <Page title="Báo cáo nhập - xuất - tồn" actions={(
      <span className="row">
        <button type="button" className="ghost" onClick={() => download(`/api/reports/stock/export?from=${from}&to=${to}&format=XLSX`)}>Xuất Excel</button>
        <button type="button" className="ghost" onClick={() => download(`/api/reports/stock/export?from=${from}&to=${to}&format=PDF`)}>Xuất PDF</button>
      </span>)}>
      <div className="filters">
        <Field label="Từ ngày"><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
        <Field label="Đến ngày"><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
      </div>
      <ErrorBox error={error} />
      <Table rows={rows} rowKey="code" columns={[
        { key: 'code', title: 'Mã' }, { key: 'name', title: 'Nguyên liệu' }, { key: 'unit', title: 'ĐVT' },
        { title: 'Tồn đầu', num: true, render: (r) => qty(r.opening) },
        { title: 'Nhập', num: true, render: (r) => qty(r.in) },
        { title: 'Xuất', num: true, render: (r) => qty(r.out) },
        { title: 'Điều chỉnh', num: true, render: (r) => qty(r.adjust) },
        { title: 'Tồn cuối', num: true, render: (r) => qty(r.closing) },
      ]} />
    </Page>
  );
}
