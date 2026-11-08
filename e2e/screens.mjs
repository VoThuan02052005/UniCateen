// Kiem thu chap nhan (end-to-end) tren he thong dang chay: di theo kich ban ban hang mau o Bang 4.x cua
// bao cao, kiem tra ket qua hien thi va chup man hinh that. Ket qua ghi vao out/results.json.
import { chromium } from 'playwright';
import fs from 'node:fs';

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const OUT = 'out';
fs.mkdirSync(OUT, { recursive: true });
const results = [];
const PASSWORD = 'Demo@2026';

function check(id, name, ok, detail = '') {
  results.push({ id, name, ok: !!ok, detail });
  console.log(`${ok ? 'DAT ' : 'LOI '} ${id} ${name} ${detail}`);
}

const browser = await chromium.launch();

async function session(email) {
  const ctx = await browser.newContext({ viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1.5, locale: 'vi-VN' });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => check('JS', 'Loi JavaScript', false, e.message));
  await page.goto(BASE + '/');
  await page.fill('input[type=email]', email);
  await page.fill('input[type=password]', PASSWORD);
  await page.click('button:has-text("Đăng nhập")');
  await page.waitForSelector('.sidebar', { timeout: 15000 });
  return { ctx, page };
}

async function go(page, path) {
  await page.goto(`${BASE}/#/${path}`);
  await page.waitForSelector('main.content > *', { timeout: 15000 });
  await page.waitForTimeout(800);
}

const shot = (page, name) => page.screenshot({ path: `${OUT}/${name}.png` });

async function step(id, name, fn) {
  try {
    await fn();
  } catch (e) {
    check(id, name, false, String(e.message).split('\n')[0]);
  }
}

// ---------------------------------------------------------------- dang nhap
{
  const ctx = await browser.newContext({ viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1.5 });
  const page = await ctx.newPage();
  await page.goto(BASE + '/');
  await page.waitForSelector('input[type=email]');
  await shot(page, '01_dang_nhap');
  await page.fill('input[type=email]', 'banhang1@phenikaa-uni.edu.vn');
  await page.fill('input[type=password]', 'sai-mat-khau');
  await page.click('button:has-text("Đăng nhập")');
  await page.waitForSelector('.error');
  check('E2E-01', 'Dang nhap sai bi tu choi', (await page.textContent('.error')).includes('không đúng'));
  await ctx.close();
}

// ---------------------------------------------------------------- ban hang tai quay
let paidCode = '';
await step('E2E-02', 'Ban hang tien mat', async () => {
  const { ctx, page } = await session('banhang1@phenikaa-uni.edu.vn');
  await go(page, 'pos');
  check('E2E-02', 'Dang nhap nhan vien ban hang vao POS', page.url().includes('#/pos') || (await page.isVisible('.pos')));
  const tile = (name) => page.click(`.tile:has-text("${name}")`);
  await tile('Cơm gà xối mỡ'); await page.waitForTimeout(400);
  await tile('Cơm gà xối mỡ'); await page.waitForTimeout(400);
  await tile('Cà phê sữa đá'); await page.waitForTimeout(400);
  await tile('Trà đá'); await page.waitForTimeout(400);
  await tile('Trà đá'); await page.waitForTimeout(600);
  const total = await page.textContent('.total strong');
  await shot(page, '02_pos_tao_don');
  check('E2E-03', 'Tong tien don 2 com ga + 1 ca phe + 2 tra da = 96.000 d (BR11, BR12)', total.includes('96.000'), total);
  paidCode = (await page.textContent('.cart-head h2')).trim();
  await page.click('button:has-text("Chốt đơn")');
  await page.waitForSelector('.paybox');
  await page.fill('.paybox input[placeholder="SV..."]', 'SV23010427');
  await page.fill('.paybox input[type=number]', '100000');
  await page.waitForTimeout(300);
  await shot(page, '03_thanh_toan_tien_mat');
  await page.click('button:has-text("Thu tiền mặt")');
  await page.waitForSelector('.change', { timeout: 15000 });
  const change = await page.textContent('.change');
  await shot(page, '04_da_thanh_toan');
  check('E2E-04', 'Thu 100.000 d, tien thua 4.000 d, don PAID (BR25, BR22)', change.includes('4.000') && (await page.textContent('.cart-head .badge')).includes('Đã thanh toán'), change);

  // the bi tu choi roi chuyen khoan
  await page.click('button:has-text("Đơn mới")');
  await tile('Cơm sườn nướng'); await page.waitForTimeout(500);
  await page.click('button:has-text("Chốt đơn")');
  await page.waitForSelector('.paybox');
  await page.click('.paybox .tabs button:has-text("Thẻ SV/GV")');
  await page.fill('.paybox input[placeholder="SV..."]', 'SV23010062DECLINE');
  await page.click('button:has-text("Thanh toán thẻ")');
  await page.waitForSelector('.paybox .error', { timeout: 20000 });
  await page.waitForTimeout(300);
  await shot(page, '05_the_bi_tu_choi');
  check('E2E-05', 'The bi tu choi: giao dich FAILED, don van cho thanh toan (BR21)',
    (await page.textContent('.paybox')).includes('thất bại') && (await page.textContent('.cart-head .badge')).includes('Chờ thanh toán'));
  await page.click('.paybox .tabs button:has-text("Chuyển khoản")');
  await page.fill('.paybox input[placeholder="SV..."]', '');
  await page.click('button:has-text("Tạo yêu cầu chuyển khoản")');
  await page.waitForSelector('.big-code', { timeout: 15000 });
  const ref = await page.textContent('.big-code');
  await shot(page, '06_chuyen_khoan');
  check('E2E-06', 'Chuyen khoan co noi dung la ma don viet lien (BR26)', /^DH\d{10}$/.test(ref.trim()), ref);

  await go(page, 'transfers');
  await shot(page, '07_cho_chuyen_khoan');
  await page.click('button:has-text("Đã nhận")');
  await page.waitForTimeout(1500);
  check('E2E-07', 'Xac nhan chuyen khoan, hang doi trong (UC3.4)', (await page.textContent('.page')).includes('Không có giao dịch chờ'));

  await go(page, 'orders');
  await shot(page, '08_tra_cuu_don');
  check('E2E-08', 'Tra cuu don hien don vua thanh toan (UC2.4)', (await page.textContent('table')).includes(paidCode));
  await go(page, 'payments');
  await shot(page, '09_giao_dich');
  await ctx.close();
});

// ---------------------------------------------------------------- bep
await step('E2E-09', 'Bep', async () => {
  const { ctx, page } = await session('bep@phenikaa-uni.edu.vn');
  await go(page, 'kitchen');
  await shot(page, '10_bep');
  const ticket = page.locator(`.ticket:has-text("${paidCode}")`);
  check('E2E-09', 'Don da thanh toan xuat hien o cot Chua lam (BR22)', (await ticket.count()) === 1);
  await ticket.locator('button').click();
  await page.waitForTimeout(1200);
  const col = await page.locator('.board-col').nth(1).textContent();
  check('E2E-10', 'Nhan lam: don chuyen sang Dang lam (BR39)', col.includes(paidCode));
  await shot(page, '11_bep_dang_lam');
  await ctx.close();
});

// ---------------------------------------------------------------- kho
await step('E2E-11', 'Kho', async () => {
  const { ctx, page } = await session('kho@phenikaa-uni.edu.vn');
  await go(page, 'stock');
  await shot(page, '12_ton_kho');
  const txt = await page.textContent('.page');
  check('E2E-11', 'Canh bao ton thap va lo sap het han (BR38)', txt.includes('Tồn thấp') && txt.includes('sắp hết hạn'));
  await go(page, 'receipts');
  await shot(page, '13_phieu_nhap');
  await go(page, 'issues');
  await page.click('button:has-text("Lập phiếu xuất")');
  await page.locator('.panel select').nth(1).selectOption({ label: 'Cơm gà xối mỡ' });
  await page.locator('.panel input[type=number]').first().fill('20');
  await page.click('button:has-text("Tạo phiếu")');
  await page.click('button:has-text("Gợi ý theo định mức")');
  await page.waitForSelector('table.lines', { timeout: 15000 });
  await page.waitForTimeout(500);
  await shot(page, '14_goi_y_xuat_fefo');
  check('E2E-12', 'Goi y phieu xuat theo dinh muc va FEFO (BR34)', (await page.locator('table.lines tbody tr').count()) >= 2);
  await page.click('button:has-text("Xác nhận xuất")');
  await page.waitForTimeout(1500);
  check('E2E-13', 'Xac nhan phieu xuat, tru ton (BR36)', (await page.textContent('.panel')).includes('Đã xác nhận'));
  await shot(page, '15_phieu_xuat_da_xac_nhan');
  await ctx.close();
});

// ---------------------------------------------------------------- quan ly
await step('E2E-14', 'Quan ly', async () => {
  const { ctx, page } = await session('quanly@phenikaa-uni.edu.vn');
  await go(page, 'menu');
  await shot(page, '16_mon_va_gia');
  await go(page, 'recipes');
  await page.click('tr:has-text("Cơm gà xối mỡ")');
  await page.waitForTimeout(800);
  await shot(page, '17_cong_thuc');
  await go(page, 'revenue');
  await page.waitForSelector('.stat');
  await shot(page, '18_bao_cao_doanh_thu');
  const stats = await page.textContent('.stats');
  check('E2E-14', 'Bao cao doanh thu co tien mat va chuyen khoan, khong tinh giao dich the that bai (BR29)',
    stats.includes('Tiền mặt') && stats.includes('Chuyển khoản') && !stats.includes('Thẻ SV/GV'), stats);
  await go(page, 'stock-report');
  await shot(page, '19_bao_cao_ton_kho');
  await ctx.close();
});

// ---------------------------------------------------------------- quan tri, sinh vien
await step('E2E-15', 'Quan tri vien', async () => {
  const { ctx, page } = await session('admin@phenikaa-uni.edu.vn');
  await go(page, 'accounts');
  await shot(page, '20_tai_khoan');
  await go(page, 'audit');
  await shot(page, '21_nhat_ky');
  check('E2E-15', 'Nhat ky ghi xac nhan chuyen khoan (NFR09)', (await page.textContent('.page')).includes('TRANSFER_CONFIRM'));
  await ctx.close();
});

await step('E2E-16', 'Sinh vien', async () => {
  const { ctx, page } = await session('23010427@st.phenikaa-uni.edu.vn');
  await go(page, 'my-history');
  await shot(page, '22_lich_su_sinh_vien');
  const t = await page.textContent('.page');
  check('E2E-16', 'Sinh vien thay don da quet the, hinh thuc Tien mat (UC2.5, BR16)', t.includes(paidCode) && t.includes('Tiền mặt'));
  await ctx.close();
});

await browser.close();
fs.writeFileSync(`${OUT}/results.json`, JSON.stringify(results, null, 2));
const failed = results.filter((r) => !r.ok);
console.log(`\nTONG: ${results.length - failed.length}/${results.length} buoc dat`);
process.exit(failed.length ? 1 : 0);
