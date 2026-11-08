import React, { useState } from 'react';
import { api, download } from '../../api/client.js';
import { date, money, today } from '../../format.js';
import { ErrorBox, Field, Page, Table, useLoad } from '../../components/ui.jsx';

const firstOfMonth = () => `${today().slice(0, 8)}01`;

/** UC5.4 Bao cao doanh thu: chi tinh giao dich thanh cong theo ngay thanh cong (BR29). */
export default function RevenuePage() {
  const [from, setFrom] = useState(firstOfMonth());
  const [to, setTo] = useState(today());
  const [r, , error] = useLoad(() => api.get(`/api/reports/revenue?from=${from}&to=${to}`), [from, to]);
  const max = r ? Math.max(1, ...r.byDay.map((d) => d.revenue)) : 1;
  return (
    <Page title="Báo cáo doanh thu" actions={(
      <span className="row">
        <button type="button" className="ghost" onClick={() => download(`/api/reports/revenue/export?from=${from}&to=${to}&format=XLSX`)}>Xuất Excel</button>
        <button type="button" className="ghost" onClick={() => download(`/api/reports/revenue/export?from=${from}&to=${to}&format=PDF`)}>Xuất PDF</button>
      </span>)}>
      <div className="filters">
        <Field label="Từ ngày"><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
        <Field label="Đến ngày"><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
      </div>
      <ErrorBox error={error} />
      {r && (
        <>
          <div className="stats">
            <div className="stat"><small>Doanh thu</small><strong>{money(r.totalRevenue)}</strong></div>
            <div className="stat"><small>Số đơn thanh toán</small><strong>{r.totalOrders}</strong></div>
            {r.byMethod.map((m) => <div className="stat" key={m.key}><small>{m.key}</small><strong>{money(m.revenue)}</strong></div>)}
          </div>
          <h2>Theo ngày</h2>
          <div className="bars">
            {r.byDay.map((d) => (
              <div className="bar-row" key={d.key}>
                <span>{date(d.key)}</span>
                <div className="bar"><div style={{ width: `${(d.revenue / max) * 100}%` }} /></div>
                <span className="num">{money(d.revenue)}</span>
              </div>
            ))}
          </div>
          <h2>Theo món</h2>
          <Table rowKey="item" rows={r.byItem} columns={[
            { key: 'item', title: 'Món' }, { key: 'category', title: 'Danh mục' },
            { key: 'quantity', title: 'Số lượng', num: true },
            { title: 'Doanh thu', num: true, render: (i) => money(i.revenue) },
          ]} />
        </>
      )}
    </Page>
  );
}
