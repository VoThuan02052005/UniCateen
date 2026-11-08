import React, { useEffect, useState } from 'react';
import { api, setUnauthorizedHandler } from './api/client.js';
import { label } from './format.js';
import LoginPage from './pages/LoginPage.jsx';
import ChangePasswordPage from './pages/ChangePasswordPage.jsx';
import MyHistoryPage from './pages/MyHistoryPage.jsx';
import PosPage from './pages/pos/PosPage.jsx';
import OrdersPage from './pages/pos/OrdersPage.jsx';
import TransferQueuePage from './pages/pos/TransferQueuePage.jsx';
import PaymentsPage from './pages/pos/PaymentsPage.jsx';
import CategoryPage from './pages/catalog/CategoryPage.jsx';
import MenuItemPage from './pages/catalog/MenuItemPage.jsx';
import IngredientPage from './pages/catalog/IngredientPage.jsx';
import RecipePage from './pages/catalog/RecipePage.jsx';
import SupplierPage from './pages/stock/SupplierPage.jsx';
import ReceiptPage from './pages/stock/ReceiptPage.jsx';
import StockPage from './pages/stock/StockPage.jsx';
import IssuePage from './pages/stock/IssuePage.jsx';
import KitchenBoardPage from './pages/kitchen/KitchenBoardPage.jsx';
import AccountPage from './pages/admin/AccountPage.jsx';
import AuditLogPage from './pages/admin/AuditLogPage.jsx';
import RevenuePage from './pages/report/RevenuePage.jsx';
import StockReportPage from './pages/report/StockReportPage.jsx';

/**
 * Menu theo vai tro (ma tran quyen, Chuong 1). Day chi la lop hien thi: may chu van kiem tra quyen
 * cho tung API (NFR04), an nut khong thay the kiem tra o may chu.
 */
const ROUTES = [
  { path: 'pos', title: 'Bán hàng', roles: ['CASHIER', 'MANAGER'], el: PosPage },
  { path: 'orders', title: 'Đơn hàng', roles: ['CASHIER', 'MANAGER'], el: OrdersPage },
  { path: 'transfers', title: 'Chờ chuyển khoản', roles: ['CASHIER', 'MANAGER'], el: TransferQueuePage },
  { path: 'payments', title: 'Giao dịch', roles: ['CASHIER', 'MANAGER'], el: PaymentsPage },
  { path: 'kitchen', title: 'Bếp', roles: ['KITCHEN_STAFF'], el: KitchenBoardPage },
  { path: 'categories', title: 'Danh mục', roles: ['MANAGER'], el: CategoryPage },
  { path: 'menu', title: 'Món và giá', roles: ['MANAGER', 'KITCHEN_STAFF'], el: MenuItemPage },
  { path: 'ingredients', title: 'Nguyên liệu', roles: ['MANAGER', 'INVENTORY_STAFF'], el: IngredientPage },
  { path: 'recipes', title: 'Công thức', roles: ['MANAGER', 'KITCHEN_STAFF'], el: RecipePage },
  { path: 'suppliers', title: 'Nhà cung cấp', roles: ['INVENTORY_STAFF', 'MANAGER'], el: SupplierPage },
  { path: 'receipts', title: 'Nhập kho', roles: ['INVENTORY_STAFF', 'MANAGER'], el: ReceiptPage },
  { path: 'stock', title: 'Tồn kho', roles: ['INVENTORY_STAFF', 'MANAGER'], el: StockPage },
  { path: 'issues', title: 'Xuất nguyên liệu', roles: ['INVENTORY_STAFF', 'MANAGER'], el: IssuePage },
  { path: 'revenue', title: 'Báo cáo doanh thu', roles: ['MANAGER'], el: RevenuePage },
  { path: 'stock-report', title: 'Báo cáo tồn kho', roles: ['MANAGER', 'INVENTORY_STAFF'], el: StockReportPage },
  { path: 'accounts', title: 'Tài khoản', roles: ['ADMIN'], el: AccountPage },
  { path: 'audit', title: 'Nhật ký', roles: ['ADMIN'], el: AuditLogPage },
  { path: 'my-history', title: 'Lịch sử mua hàng', roles: ['STUDENT', 'FACULTY'], el: MyHistoryPage },
  { path: 'password', title: 'Đổi mật khẩu', roles: null, el: ChangePasswordPage },
];

function useHash() {
  const [hash, setHash] = useState(window.location.hash.slice(2));
  useEffect(() => {
    const h = () => setHash(window.location.hash.slice(2));
    window.addEventListener('hashchange', h);
    return () => window.removeEventListener('hashchange', h);
  }, []);
  return hash;
}

export default function App() {
  const [me, setMe] = useState(undefined);
  const hash = useHash();

  useEffect(() => {
    setUnauthorizedHandler(() => setMe(null));
    api.get('/api/auth/me').then(setMe).catch(() => setMe(null));
  }, []);

  if (me === undefined) return <p className="center muted">Đang tải...</p>;
  if (me === null) return <LoginPage onLogin={setMe} />;
  if (me.mustChangePassword) {
    return <ChangePasswordPage forced me={me} onDone={() => setMe({ ...me, mustChangePassword: false })} />;
  }

  const allowed = ROUTES.filter((r) => !r.roles || r.roles.includes(me.role));
  const current = allowed.find((r) => r.path === hash) || allowed[0];
  const Current = current.el;

  const logout = async () => {
    await api.post('/api/auth/logout').catch(() => {});
    setMe(null);
    window.location.hash = '';
  };

  return (
    <div className="layout">
      <nav className="sidebar">
        <div className="brand">UniCateen</div>
        {allowed.map((r) => (
          <a key={r.path} href={`#/${r.path}`} className={r === current ? 'active' : ''}>{r.title}</a>
        ))}
        <div className="me">
          <div>{me.fullName}</div>
          <small>{label(me.role)}</small>
          <button type="button" className="ghost" onClick={logout}>Đăng xuất</button>
        </div>
      </nav>
      <main className="content">
        <Current me={me} />
      </main>
    </div>
  );
}
