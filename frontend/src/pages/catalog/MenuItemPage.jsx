import React, { useState } from 'react';
import { api } from '../../api/client.js';
import { dateTime, money } from '../../format.js';
import { Badge, ErrorBox, Field, Page, Panel, StatusSelect, Table, useAction, useLoad } from '../../components/ui.jsx';

/**
 * UC1.2 Quan ly mon, UC1.3 Cap nhat gia. Moi mon co gia rieng (BR02); doi gia tao ban ghi moi (BR05),
 * chi quan ly (BR06). Nhan vien che bien chi doi Dang ban / Tam het.
 */
export default function MenuItemPage({ me }) {
  const manager = me.role === 'MANAGER';
  const [items, reload, loadError] = useLoad(() => api.get('/api/catalog/menu-items'));
  const [cats] = useLoad(() => (manager ? api.get('/api/catalog/categories') : Promise.resolve([])));
  const [edit, setEdit] = useState(null);
  const [priceOf, setPriceOf] = useState(null);
  const { run, error } = useAction();

  const save = (e) => {
    e.preventDefault();
    const body = { ...edit, categoryId: Number(edit.categoryId), initialPrice: edit.id ? null : Number(edit.initialPrice) };
    run(async () => {
      await (edit.id ? api.put(`/api/catalog/menu-items/${edit.id}`, body) : api.post('/api/catalog/menu-items', body));
      setEdit(null);
      reload();
    });
  };
  const setStatus = (i, status) => run(async () => {
    await api.patch(`/api/catalog/menu-items/${i.id}/status`, { status });
    reload();
  });
  const statuses = manager ? ['ON_SALE', 'OUT_OF_STOCK', 'DISCONTINUED'] : ['ON_SALE', 'OUT_OF_STOCK'];

  return (
    <Page title="Món ăn, đồ uống và giá" actions={manager && cats?.length > 0 && (
      <button type="button" onClick={() => setEdit({ categoryId: cats[0].id, name: '', unit: 'phần', initialPrice: '' })}>Thêm món</button>)}>
      <ErrorBox error={loadError || error} />
      {edit && (
        <Panel title={edit.id ? 'Sửa món' : 'Thêm món'} onClose={() => setEdit(null)}>
          <form onSubmit={save} className="form-grid">
            <Field label="Danh mục">
              <select value={edit.categoryId} onChange={(e) => setEdit({ ...edit, categoryId: e.target.value })}>
                {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="Tên món"><input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} required minLength={2} /></Field>
            <Field label="Đơn vị bán"><input value={edit.unit} onChange={(e) => setEdit({ ...edit, unit: e.target.value })} required /></Field>
            {!edit.id && (
              <Field label="Giá bán (đồng)"><input type="number" min="1" step="1000" value={edit.initialPrice} onChange={(e) => setEdit({ ...edit, initialPrice: e.target.value })} required /></Field>
            )}
            <Field label="Mô tả"><input value={edit.description || ''} onChange={(e) => setEdit({ ...edit, description: e.target.value })} /></Field>
            <button type="submit">Lưu</button>
          </form>
        </Panel>
      )}
      {priceOf && <PricePanel item={priceOf} onClose={() => setPriceOf(null)} onChanged={reload} />}
      <Table rows={items} columns={[
        { key: 'code', title: 'Mã' },
        { key: 'name', title: 'Tên món' },
        { key: 'categoryName', title: 'Danh mục' },
        { key: 'unit', title: 'ĐVT' },
        { title: 'Giá hiện hành', num: true, render: (i) => money(i.price) },
        { title: 'Trạng thái', render: (i) => <StatusSelect value={i.status} options={statuses.includes(i.status) ? statuses : [i.status, ...statuses]} onChange={(s) => setStatus(i, s)} /> },
        { title: '', render: (i) => manager && (
          <span className="row">
            <button type="button" className="ghost" onClick={() => setEdit({ ...i })}>Sửa</button>
            <button type="button" className="ghost" onClick={() => setPriceOf(i)}>Giá</button>
          </span>) },
      ]} />
    </Page>
  );
}

function PricePanel({ item, onClose, onChanged }) {
  const [history, reload] = useLoad(() => api.get(`/api/catalog/menu-items/${item.id}/prices`), [item.id]);
  const [price, setPrice] = useState('');
  const [from, setFrom] = useState('');
  const [reason, setReason] = useState('');
  const { run, error } = useAction();
  const save = (e) => {
    e.preventDefault();
    run(async () => {
      await api.post(`/api/catalog/menu-items/${item.id}/prices`, { price: Number(price), effectiveFrom: from || null, reason });
      setPrice('');
      reload();
      onChanged();
    });
  };
  return (
    <Panel title={`Giá bán: ${item.name}`} onClose={onClose}>
      <form onSubmit={save} className="form-grid">
        <Field label="Giá mới (đồng)"><input type="number" min="1" step="500" value={price} onChange={(e) => setPrice(e.target.value)} required /></Field>
        <Field label="Hiệu lực từ" hint="Bỏ trống: áp dụng ngay"><input type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
        <Field label="Lý do"><input value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
        <button type="submit">Đổi giá</button>
      </form>
      <ErrorBox error={error} />
      <Table rows={history} columns={[
        { title: 'Giá', num: true, render: (p) => money(p.price) },
        { title: 'Từ', render: (p) => dateTime(p.effectiveFrom) },
        { title: 'Đến', render: (p) => dateTime(p.effectiveTo) },
        { title: 'Trạng thái', render: (p) => <Badge value={p.status} /> },
        { key: 'reason', title: 'Lý do' },
        { key: 'createdBy', title: 'Người đổi' },
      ]} />
    </Panel>
  );
}
