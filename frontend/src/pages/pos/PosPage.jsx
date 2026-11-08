import React, { useMemo, useRef, useState } from 'react';
import { api, newIdempotencyKey } from '../../api/client.js';
import { label, money } from '../../format.js';
import { Badge, ErrorBox, Field, useAction, useLoad } from '../../components/ui.jsx';

/**
 * UC2.1-UC2.3, UC3.1-UC3.3: giao dien ban hang tai quay (POS).
 * Moi don chi thanh toan bang mot hinh thuc (BR18); quet the la tuy chon voi tien mat, chuyen khoan (BR23).
 */
export default function PosPage() {
  const [menu] = useLoad(() => api.get('/api/catalog/menu-items/on-sale'));
  const [order, setOrder] = useState(null);
  const [lastPayment, setLastPayment] = useState(null);
  const [category, setCategory] = useState('ALL');
  const { run, error, busy, clear } = useAction();

  const categories = useMemo(() => {
    const m = new Map();
    (menu || []).forEach((i) => m.set(i.categoryId, i.categoryName));
    return [...m.entries()];
  }, [menu]);
  const shown = (menu || []).filter((i) => category === 'ALL' || i.categoryId === category);

  const add = (item) => run(async () => {
    const o = order
      ? await api.post(`/api/orders/${order.code}/lines`, { menuItemId: item.id, quantity: 1 })
      : await api.post('/api/orders', { lines: [{ menuItemId: item.id, quantity: 1 }] });
    setOrder(o);
  });
  const setQty = (line, q) => run(async () => setOrder(q <= 0
    ? await api.del(`/api/orders/${order.code}/lines/${line.menuItemId}`)
    : await api.put(`/api/orders/${order.code}/lines/${line.menuItemId}`, { quantity: q })));
  const submit = () => run(async () => setOrder(await api.post(`/api/orders/${order.code}/submit`)));
  const reopen = () => run(async () => setOrder(await api.post(`/api/orders/${order.code}/reopen`)));
  const cancel = () => {
    const reason = order.status === 'PENDING_PAYMENT' ? window.prompt('Lý do hủy đơn (bắt buộc):') : '';
    if (order.status === 'PENDING_PAYMENT' && !reason) return;
    run(async () => {
      await api.post(`/api/orders/${order.code}/cancel`, { reason });
      setOrder(null);
    });
  };
  const newOrder = () => {
    setOrder(null);
    setLastPayment(null);
    clear();
  };

  const draft = order?.status === 'DRAFT';

  return (
    <div className="pos">
      <section className="pos-menu">
        <div className="tabs">
          <button type="button" className={category === 'ALL' ? 'on' : ''} onClick={() => setCategory('ALL')}>Tất cả</button>
          {categories.map(([id, name]) => (
            <button type="button" key={id} className={category === id ? 'on' : ''} onClick={() => setCategory(id)}>{name}</button>
          ))}
        </div>
        <div className="grid">
          {shown.map((i) => (
            <button type="button" key={i.id} className="tile" disabled={busy || (order && !draft)} onClick={() => add(i)}>
              <span className="tile-name">{i.name}</span>
              <span className="tile-price">{money(i.price)}</span>
            </button>
          ))}
        </div>
      </section>

      <aside className="pos-cart">
        <div className="cart-head">
          <h2>{order ? order.code : 'Đơn mới'}</h2>
          {order && <Badge value={order.status} />}
        </div>
        {!order && <p className="muted">Chọn món để tạo đơn.</p>}
        {order && (
          <>
            <table className="cart">
              <tbody>
                {order.lines.map((l) => (
                  <tr key={l.menuItemId}>
                    <td>{l.menuItemName}<br /><small className="muted">{money(l.unitPrice)}</small></td>
                    <td>
                      <div className="qty">
                        {draft && <button type="button" className="sq" onClick={() => setQty(l, l.quantity - 1)}>−</button>}
                        <span>{l.quantity}</span>
                        {draft && <button type="button" className="sq" disabled={l.quantity >= 99} onClick={() => setQty(l, l.quantity + 1)}>+</button>}
                      </div>
                    </td>
                    <td className="num">{money(l.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="total"><span>Tổng tiền</span><strong>{money(order.totalAmount)}</strong></div>
          </>
        )}
        <ErrorBox error={error} />
        {draft && (
          <div className="row">
            <button type="button" className="ghost" onClick={cancel} disabled={busy}>Hủy đơn</button>
            <button type="button" onClick={submit} disabled={busy || order.lines.length === 0}>Chốt đơn</button>
          </div>
        )}
        {order?.status === 'PENDING_PAYMENT' && (
          <PaymentBox order={order} onPaid={(o, p) => { setOrder(o); setLastPayment(p); }} onReopen={reopen} onCancel={cancel} />
        )}
        {order?.status === 'PAID' && lastPayment && (
          <div className="ok">
            Đã thu {label(lastPayment.method)} · {lastPayment.code}
            {lastPayment.changeAmount != null && <div className="change">Tiền thừa trả khách: <strong>{money(lastPayment.changeAmount)}</strong></div>}
          </div>
        )}
        {order && ['PAID', 'CANCELLED'].includes(order.status) && (
          <button type="button" className="big" onClick={newOrder}>Đơn mới</button>
        )}
      </aside>
    </div>
  );
}

function PaymentBox({ order, onPaid, onReopen, onCancel }) {
  const [method, setMethod] = useState('CASH');
  const [received, setReceived] = useState('');
  const [cardCode, setCardCode] = useState('');
  const [result, setResult] = useState(null);
  const [transfer, setTransfer] = useState(null);
  const keyRef = useRef(null);          // giu cung khoa khi bam lai sau loi mang (NFR06)
  const { run, error, busy } = useAction();

  const pay = (url, body) => run(async () => {
    keyRef.current = keyRef.current || newIdempotencyKey();
    try {
      const p = await api.post(url, body, { 'Idempotency-Key': keyRef.current });
      keyRef.current = null;
      return p;
    } catch (e) {
      if (e.status && e.status !== 0) keyRef.current = null;   // may chu da tra loi: lan sau la yeu cau moi
      throw e;
    }
  });

  const afterPayment = async (p) => {
    if (!p) return;
    setResult(p);
    if (p.status === 'SUCCESS') onPaid(await api.get(`/api/orders/${order.code}`), p);
  };

  const payCash = async () => afterPayment(await pay('/api/payments/cash',
    { orderCode: order.code, receivedAmount: Number(received), cardCode: cardCode || null }));
  const payCard = async () => afterPayment(await pay('/api/payments/card', { orderCode: order.code, cardCode }));
  const startTransfer = async () => {
    const p = await pay('/api/payments/transfer', { orderCode: order.code, cardCode: cardCode || null });
    if (p) setTransfer(p);
  };
  const confirmTransfer = () => run(async () => afterPayment(await api.post(`/api/payments/${transfer.code}/confirm`)));
  const failTransfer = () => {
    const reason = window.prompt('Lý do (ví dụ: không nhận được tiền, khách đổi hình thức):');
    if (!reason) return;
    run(async () => {
      setResult(await api.post(`/api/payments/${transfer.code}/fail`, { reason }));
      setTransfer(null);
    });
  };

  const change = Number(received) - order.totalAmount;
  const quick = [order.totalAmount, 50000, 100000, 200000, 500000].filter((v, i, a) => v >= order.totalAmount && a.indexOf(v) === i);

  if (transfer) {
    return (
      <div className="paybox">
        <h3>Chuyển khoản</h3>
        <p>Số tiền: <strong>{money(transfer.amount)}</strong></p>
        <p>Nội dung chuyển khoản: <code className="big-code">{transfer.referenceCode}</code></p>
        <ErrorBox error={error} />
        <div className="row">
          <button type="button" className="ghost" onClick={failTransfer} disabled={busy}>Không nhận được</button>
          <button type="button" onClick={confirmTransfer} disabled={busy}>Đã nhận đúng tiền</button>
        </div>
        <p className="muted small">Có thể xác nhận sau ở mục “Chờ chuyển khoản”.</p>
      </div>
    );
  }

  return (
    <div className="paybox">
      <div className="tabs">
        {['CASH', 'STUDENT_FACULTY_CARD', 'BANK_TRANSFER'].map((m) => (
          <button type="button" key={m} className={method === m ? 'on' : ''} onClick={() => setMethod(m)}>{label(m)}</button>
        ))}
      </div>
      <Field label={method === 'STUDENT_FACULTY_CARD' ? 'Mã thẻ (quét thẻ)' : 'Quét thẻ SV/GV (tùy chọn)'}
             hint={method === 'STUDENT_FACULTY_CARD' ? null : 'Để lưu đơn vào lịch sử mua hàng của người mua'}>
        <input value={cardCode} onChange={(e) => setCardCode(e.target.value.trim())} placeholder="SV..." />
      </Field>
      {method === 'CASH' && (
        <>
          <Field label="Tiền khách đưa">
            <input type="number" min="0" step="1000" value={received} onChange={(e) => setReceived(e.target.value)} />
          </Field>
          <div className="chips">
            {quick.map((v) => <button type="button" key={v} className="chip" onClick={() => setReceived(String(v))}>{money(v)}</button>)}
          </div>
          {received !== '' && <p>Tiền thừa: <strong>{change >= 0 ? money(change) : 'Chưa đủ'}</strong></p>}
          <button type="button" className="big" disabled={busy || received === '' || change < 0} onClick={payCash}>Thu tiền mặt</button>
        </>
      )}
      {method === 'STUDENT_FACULTY_CARD' && (
        <button type="button" className="big" disabled={busy || !cardCode} onClick={payCard}>
          {busy ? 'Đang chờ dịch vụ thẻ...' : 'Thanh toán thẻ'}
        </button>
      )}
      {method === 'BANK_TRANSFER' && (
        <button type="button" className="big" disabled={busy} onClick={startTransfer}>Tạo yêu cầu chuyển khoản</button>
      )}
      <ErrorBox error={error} />
      {result && result.status === 'FAILED' && (
        <div className="error">Giao dịch {result.code} thất bại: {result.failureReason}. Có thể thử lại hoặc đổi hình thức.</div>
      )}
      {result && result.status === 'SUCCESS' && result.changeAmount != null && (
        <div className="ok">Tiền thừa trả khách: {money(result.changeAmount)}</div>
      )}
      <div className="row">
        <button type="button" className="ghost" onClick={onReopen} disabled={busy}>Sửa đơn</button>
        <button type="button" className="ghost" onClick={onCancel} disabled={busy}>Hủy đơn</button>
      </div>
    </div>
  );
}
