import React, { useEffect } from 'react';
import { api } from '../../api/client.js';
import { dateTime, label } from '../../format.js';
import { ErrorBox, Page, useAction, useLoad } from '../../components/ui.jsx';

const NEXT = { NOT_STARTED: 'PREPARING', PREPARING: 'READY', READY: 'SERVED' };
const ACTION = { NOT_STARTED: 'Nhận làm', PREPARING: 'Đã xong', READY: 'Đã giao' };

/** UC4.5 Theo doi chuan bi mon: chi di tien NOT_STARTED -> PREPARING -> READY -> SERVED (BR39). */
export default function KitchenBoardPage() {
  const [rows, reload, loadError] = useLoad(() => api.get('/api/kitchen/preparations'));
  const { run, error, busy } = useAction();
  useEffect(() => {
    const t = setInterval(reload, 10000);          // tu tai lai moi 10 giay
    return () => clearInterval(t);
  }, [reload]);
  const advance = (p) => run(async () => {
    await api.post(`/api/kitchen/preparations/${p.id}/status`, { status: NEXT[p.status] });
    reload();
  });
  const columns = ['NOT_STARTED', 'PREPARING', 'READY'];
  return (
    <Page title="Bảng chuẩn bị món" actions={<button type="button" className="ghost" onClick={reload}>Tải lại</button>}>
      <ErrorBox error={loadError || error} />
      <div className="board">
        {columns.map((c) => (
          <div className="board-col" key={c}>
            <h2>{label(c)}</h2>
            {(rows || []).filter((p) => p.status === c).map((p) => (
              <div className="card ticket" key={p.id}>
                <div className="row between"><strong>{p.orderCode}</strong><small>{dateTime(p.paidAt)}</small></div>
                <ul>{p.lines.map((l, i) => <li key={i}>{l.quantity} x {l.menuItemName}</li>)}</ul>
                {p.customerName && <small className="muted">{p.customerName}</small>}
                <button type="button" disabled={busy} onClick={() => advance(p)}>{ACTION[p.status]}</button>
              </div>
            ))}
          </div>
        ))}
      </div>
    </Page>
  );
}
