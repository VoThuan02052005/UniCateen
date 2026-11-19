// Quay video demo UniCateen theo kich ban 6 buoc (bao ve hoc phan). Moi vai tro mot cua so trinh duyet,
// thao tac cham nhu nguoi that, co dong chu thich buoc o tren man hinh. Video ghi vao video/.
import { chromium } from 'playwright';
import fs from 'node:fs';

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Demo@2026';
const SIZE = { width: 1280, height: 720 };
fs.rmSync('video', { recursive: true, force: true });
fs.mkdirSync('video', { recursive: true });
const browser = await chromium.launch({ slowMo: 120 });
const clips = [];

async function caption(page, text) {
  await page.evaluate((t) => {
    let el = document.getElementById('demo-caption');
    if (!el) {
      el = document.createElement('div');
      el.id = 'demo-caption';
      el.style.cssText = 'position:fixed;left:122px;bottom:16px;z-index:99999;'
        + 'background:rgba(34,55,113,.94);color:#fff;font:600 19px/1.35 system-ui,sans-serif;'
        + 'padding:10px 22px;border-radius:10px;max-width:560px;text-align:left;box-shadow:0 4px 14px rgba(0,0,0,.25);pointer-events:none';
      document.body.appendChild(el);
    }
    el.textContent = t;
  }, text);
}

const SLOW = 1.6;
const pause = (page, ms = 1500) => page.waitForTimeout(Math.round(ms * SLOW));

async function open(name, email, intro) {
  const ctx = await browser.newContext({ viewport: SIZE, locale: 'vi-VN', recordVideo: { dir: 'video/raw', size: SIZE } });
  const page = await ctx.newPage();
  await page.goto(BASE + '/');
  await page.waitForSelector('input[type=email]');
  await caption(page, intro);
  await pause(page, 1800);
  await page.type('input[type=email]', email, { delay: 35 });
  await page.type('input[type=password]', PASSWORD, { delay: 60 });
  await pause(page, 500);
  await page.click('button:has-text("Đăng nhập")');
  await page.waitForSelector('.sidebar', { timeout: 15000 });
  await pause(page, 1000);
  return { ctx, page, name };
}

async function go(page, path) {
  await page.click(`.sidebar a[href="#/${path}"]`).catch(() => page.goto(`${BASE}/#/${path}`));
  await page.waitForSelector('main.content > *', { timeout: 15000 });
  await pause(page, 900);
}

async function close(s) {
  await pause(s.page, 1200);
  const v = s.page.video();
  await s.ctx.close();
  const dst = `video/${s.name}.webm`;
  fs.renameSync(await v.path(), dst);
  clips.push(dst);
  console.log('Quay xong', dst);
}

// ---- 1-3. Nhan vien ban hang
{
  const s = await open('1_ban_hang', 'banhang1@phenikaa-uni.edu.vn', 'Bước 1 · Nhân viên bán hàng đăng nhập (banhang1)');
  const { page } = s;
  await go(page, 'pos');
  await caption(page, 'Bước 1 · Tạo đơn tại quầy: chọn món, hệ thống tự tính tổng tiền (UC2.1)');
  await pause(page, 1500);
  const tile = async (n) => { await page.click(`.tile:has-text("${n}")`); await pause(page, 700); };
  await tile('Cơm gà xối mỡ'); await tile('Cơm gà xối mỡ'); await tile('Cà phê sữa đá'); await tile('Trà đá');
  await pause(page, 1500);
  await caption(page, 'Bước 1 · Chốt đơn: đơn chuyển sang Chờ thanh toán');
  await page.click('button:has-text("Chốt đơn")');
  await page.waitForSelector('.paybox');
  await pause(page, 2200);
  await caption(page, 'Bước 2 · Thanh toán tiền mặt: nhập tiền khách đưa, hệ thống tính tiền thừa (UC3.2, BR25)');
  await page.type('.paybox input[type=number]', '100000', { delay: 90 });
  await pause(page, 2000);
  await page.click('button:has-text("Thu tiền mặt")');
  await page.waitForSelector('.change', { timeout: 15000 });
  await caption(page, 'Bước 2 · Đã thanh toán: giao dịch thành công, đơn Đã thanh toán, tạo phiếu chế biến (BR22)');
  await pause(page, 3500);

  await caption(page, 'Bước 3 · Đơn mới, thanh toán bằng thẻ SV/GV (UC3.1)');
  await page.click('button:has-text("Đơn mới")');
  await pause(page, 900);
  await tile('Cơm sườn nướng');
  await page.click('button:has-text("Chốt đơn")');
  await page.waitForSelector('.paybox');
  await pause(page, 1000);
  await page.click('.paybox .tabs button:has-text("Thẻ SV/GV")');
  await pause(page, 800);
  await page.type('.paybox input[placeholder="SV..."]', 'SV23010062DECLINE', { delay: 45 });
  await caption(page, 'Bước 3 · Mã thẻ kết thúc bằng DECLINE: dịch vụ thẻ mô phỏng sẽ từ chối');
  await pause(page, 1800);
  await page.click('button:has-text("Thanh toán thẻ")');
  await page.waitForSelector('.paybox .error', { timeout: 20000 });
  await caption(page, 'Bước 3 · Thẻ bị từ chối: giao dịch Thất bại, đơn vẫn Chờ thanh toán để chọn hình thức khác');
  await pause(page, 4000);
  await page.click('.paybox .tabs button:has-text("Tiền mặt")');
  await pause(page, 800);
  await page.fill('.paybox input[placeholder="SV..."]', '').catch(() => {});
  await page.type('.paybox input[type=number]', '50000', { delay: 90 });
  await caption(page, 'Bước 3 · Khách đổi sang tiền mặt');
  await pause(page, 1500);
  await page.click('button:has-text("Thu tiền mặt")');
  await page.waitForSelector('.change', { timeout: 15000 });
  await pause(page, 2500);
  await go(page, 'payments');
  await caption(page, 'Mục Giao dịch: đơn thứ hai có hai lần thử, một Thất bại và một Thành công (UC3.5)');
  await pause(page, 4500);
  await close(s);
}

// ---- 4. Bep
{
  const s = await open('2_bep', 'bep@phenikaa-uni.edu.vn', 'Bước 4 · Nhân viên chế biến đăng nhập (bep)');
  const { page } = s;
  await go(page, 'kitchen');
  await caption(page, 'Bước 4 · Bảng chuẩn bị món: chỉ đơn đã thanh toán mới xuất hiện (UC4.5)');
  await pause(page, 3000);
  const btn = page.locator('.board-col').first().locator('.ticket button').first();
  if (await btn.count()) {
    await btn.click();
    await caption(page, 'Bước 4 · Nhận làm: đơn chuyển sang cột Đang làm');
    await pause(page, 3000);
  }
  await close(s);
}

// ---- 5. Kho
{
  const s = await open('3_kho', 'kho@phenikaa-uni.edu.vn', 'Bước 5 · Nhân viên kho đăng nhập (kho)');
  const { page } = s;
  await go(page, 'stock');
  await caption(page, 'Bước 5 · Tồn kho theo lô, có cảnh báo tồn thấp và lô sắp hết hạn');
  await pause(page, 3000);
  await go(page, 'issues');
  await caption(page, 'Bước 5 · Lập phiếu xuất nguyên liệu cho ca theo số phần dự kiến (UC4.4)');
  await page.click('button:has-text("Lập phiếu xuất")');
  await pause(page, 1000);
  await page.locator('.panel select').nth(1).selectOption({ label: 'Cơm gà xối mỡ' });
  await pause(page, 800);
  await page.locator('.panel input[type=number]').first().fill('20');
  await pause(page, 1000);
  await page.click('button:has-text("Tạo phiếu")');
  await pause(page, 1200);
  await page.click('button:has-text("Gợi ý theo định mức")');
  await page.waitForSelector('table.lines', { timeout: 15000 });
  await caption(page, 'Bước 5 · Hệ thống gợi ý lô theo FEFO: lô hết hạn trước xuất trước (BR34)');
  await pause(page, 4000);
  await page.click('button:has-text("Xác nhận xuất")');
  await caption(page, 'Bước 5 · Xác nhận phiếu xuất: tồn của từng lô được trừ (BR36)');
  await pause(page, 3000);
  await close(s);
}

// ---- 6. Quan ly
{
  const s = await open('4_quan_ly', 'quanly@phenikaa-uni.edu.vn', 'Bước 6 · Quản lý canteen đăng nhập (quanly)');
  const { page } = s;
  await go(page, 'revenue');
  await page.waitForSelector('.stat');
  await caption(page, 'Bước 6 · Báo cáo doanh thu theo hình thức thanh toán; giao dịch thất bại không được tính (UC5.4)');
  await pause(page, 4500);
  const dl = page.waitForEvent('download', { timeout: 15000 }).catch(() => null);
  await page.click('button:has-text("Xuất Excel")');
  const d = await dl;
  await caption(page, d ? `Bước 6 · Đã xuất báo cáo Excel: ${d.suggestedFilename()}` : 'Bước 6 · Xuất báo cáo Excel');
  await pause(page, 3000);
  await caption(page, 'Hết phần demo · Mã nguồn: github.com/VoThuan02052005/UniCateen');
  await pause(page, 3000);
  await close(s);
}

await browser.close();
fs.writeFileSync('video/list.txt', clips.map((c) => `file '${c.replace('video/', '')}'`).join('\n') + '\n');
console.log('Xong', clips);
