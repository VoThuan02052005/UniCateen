// DV02: so tien co dau cham phan cach hang nghin, vi du 35.000 đ
export const money = (n) => (n == null ? '' : `${Number(n).toLocaleString('vi-VN')} đ`);
// DV05: ngay dd/mm/yyyy, gio hh:mm
const pad = (x) => String(x).padStart(2, '0');
export function date(v) {
  if (!v) return '';
  const d = new Date(v);
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}
export function dateTime(v) {
  if (!v) return '';
  const d = new Date(v);
  return `${date(v)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
export const today = () => new Date().toISOString().slice(0, 10);
export const qty = (n) => (n == null ? '' : Number(n).toLocaleString('vi-VN', { maximumFractionDigits: 3 }));

export const LABEL = {
  // vai tro
  ADMIN: 'Quản trị viên', MANAGER: 'Quản lý canteen', CASHIER: 'Nhân viên bán hàng',
  INVENTORY_STAFF: 'Nhân viên kho', KITCHEN_STAFF: 'Nhân viên chế biến', STUDENT: 'Sinh viên', FACULTY: 'Giảng viên',
  // trang thai don, thanh toan
  DRAFT: 'Nháp', PENDING_PAYMENT: 'Chờ thanh toán', PAID: 'Đã thanh toán', CANCELLED: 'Đã hủy',
  PENDING: 'Đang chờ', SUCCESS: 'Thành công', FAILED: 'Thất bại', REFUNDED: 'Hoàn tiền',
  STUDENT_FACULTY_CARD: 'Thẻ SV/GV', CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản',
  // mon, danh muc, kho
  ON_SALE: 'Đang bán', OUT_OF_STOCK: 'Tạm hết', DISCONTINUED: 'Ngừng bán',
  ACTIVE: 'Đang sử dụng', INACTIVE: 'Ngừng sử dụng', LOCKED: 'Đã khóa', RETIRED: 'Ngừng áp dụng',
  SCHEDULED: 'Hẹn giờ', CONFIRMED: 'Đã xác nhận',
  NOT_STARTED: 'Chưa làm', PREPARING: 'Đang làm', READY: 'Đã xong', SERVED: 'Đã giao',
  MORNING: 'Sáng', LUNCH: 'Trưa', EVENING: 'Tối',
};
export const label = (k) => LABEL[k] || k;
