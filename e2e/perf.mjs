// Do kiem chung NFR01, NFR02, NFR08 tren he thong dang chay (cach do o Bang 1.x cua bao cao).
// NFR01: 50 thao tac them mon lien tiep tren POS, 95% phai <= 1 giay; danh sach mon tai <= 2 giay.
// NFR02: 20 nhan vien dong thoi thanh toan tien mat 200 don qua REST API, moi lan ghi nhan <= 3 giay,
//        khong don nao bi bo sot hoac co hon mot giao dich SUCCESS.
// NFR08: o 1366x768, moi nut va o mon tren POS cao toi thieu 40 px.
// Ket qua ghi vao out/perf.json.
import { chromium } from 'playwright';
import fs from 'node:fs';

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Demo@2026';
fs.mkdirSync('out', { recursive: true });
const report = {};

const pct = (arr, p) => {
  const s = [...arr].sort((a, b) => a - b);
  return s[Math.min(s.length - 1, Math.ceil((p / 100) * s.length) - 1)];
};
const summary = (arr) => ({
  n: arr.length,
  min: Math.min(...arr),
  median: pct(arr, 50),
  p95: pct(arr, 95),
  max: Math.max(...arr),
});

// ---------------------------------------------------------------- NFR01 + NFR08 (giao dien)
{
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1366, height: 768 }, locale: 'vi-VN' });
  const page = await ctx.newPage();
  await page.goto(BASE + '/');
  await page.fill('input[type=email]', 'banhang1@phenikaa-uni.edu.vn');
  await page.fill('input[type=password]', PASSWORD);
  await page.click('button:has-text("Đăng nhập")');
  await page.waitForSelector('.sidebar', { timeout: 15000 });

  // thoi gian tai danh sach mon dang ban
  const loads = [];
  for (let i = 0; i < 10; i++) {
    await page.goto(BASE + '/#/orders');
    await page.waitForSelector('main.content > *');
    const t0 = Date.now();
    await page.goto(BASE + '/#/pos');
    await page.waitForSelector('.tile', { timeout: 15000 });
    loads.push(Date.now() - t0);
  }

  // 50 thao tac them mon lien tiep: tu luc bam den khi tong tien tren man hinh doi
  const names = await page.$$eval('.tile', (els) => els.slice(0, 5).map((e) => e.textContent));
  const tiles = page.locator('.tile');
  const clicks = [];
  for (let i = 0; i < 50; i++) {
    const before = (await page.textContent('.total strong').catch(() => '')) || '';
    const t0 = Date.now();
    await tiles.nth(i % Math.min(5, names.length)).click();
    await page.waitForFunction((b) => {
      const el = document.querySelector('.total strong');
      return el && el.textContent !== b;
    }, before, { timeout: 10000 });
    clicks.push(Date.now() - t0);
  }

  // NFR08: kich thuoc nut, o mon tren POS
  const els = await page.$$eval('.pos button, .tile', (els) => els
    .filter((e) => e.offsetParent !== null)
    .map((e) => ({ h: Math.round(e.getBoundingClientRect().height), cls: e.className, text: e.textContent.trim().slice(0, 20) })));
  const sizes = els.map((e) => e.h);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
  await page.screenshot({ path: 'out/perf_pos.png' });
  await browser.close();

  report.NFR01 = {
    menuLoadMs: summary(loads),
    addItemMs: summary(clicks),
    addItemWithin1s: clicks.filter((x) => x <= 1000).length,
    pass: pct(clicks, 95) <= 1000 && Math.max(...loads) <= 2000,
  };
  report.NFR08 = {
    elements: sizes.length,
    minHeightPx: Math.min(...sizes),
    below40: sizes.filter((h) => h < 40).length,
    below40Samples: els.filter((e) => e.h < 40).slice(0, 5),
    horizontalOverflow: overflow,
    pass: sizes.every((h) => h >= 40) && !overflow,
  };
  console.log('NFR01', JSON.stringify(report.NFR01));
  console.log('NFR08', JSON.stringify(report.NFR08));
}

// ---------------------------------------------------------------- NFR02 (20 nhan vien dong thoi)
async function login(email) {
  const r = await fetch(BASE + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password: PASSWORD }),
  });
  if (!r.ok) throw new Error('login ' + r.status);
  const cookie = r.headers.get('set-cookie').split(';')[0];
  return (path, method = 'GET', body, extra = {}) => fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', Cookie: cookie, ...extra },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).then(async (res) => {
    const txt = await res.text();
    if (!res.ok) throw new Error(`${method} ${path} ${res.status} ${txt.slice(0, 120)}`);
    return txt ? JSON.parse(txt) : null;
  });
}

{
  const WORKERS = 20;
  const ORDERS = 200;
  const clients = [];
  for (let i = 0; i < WORKERS; i++) {
    clients.push(await login(i % 2 === 0 ? 'banhang1@phenikaa-uni.edu.vn' : 'banhang2@phenikaa-uni.edu.vn'));
  }
  const items = (await clients[0]('/api/catalog/menu-items/on-sale')).slice(0, 4);
  const payMs = [];
  const errors = [];
  const paid = [];
  let next = 0;
  const t0 = Date.now();
  await Promise.all(clients.map(async (api, w) => {
    while (true) {
      const n = next++;
      if (n >= ORDERS) return;
      try {
        let o = await api('/api/orders', 'POST', { lines: [
          { menuItemId: items[n % items.length].id, quantity: 1 + (n % 3) },
          { menuItemId: items[(n + 1) % items.length].id, quantity: 1 },
        ] });
        o = await api(`/api/orders/${o.code}/submit`, 'POST');
        const s = Date.now();
        const p = await api('/api/payments/cash', 'POST',
          { orderCode: o.code, receivedAmount: o.totalAmount },
          { 'Idempotency-Key': `perf-${w}-${n}-${Date.now()}` });
        payMs.push(Date.now() - s);
        paid.push({ order: o.code, status: p.status, w });
      } catch (e) {
        errors.push(String(e.message));
      }
    }
  }));
  const wallMs = Date.now() - t0;

  // doi chieu: moi don vua tao co dung mot giao dich SUCCESS
  // moi nhan vien ban hang chi xem duoc don cua minh (NFR04), nen doi chieu bang dung phien da tao don
  let notPaid = 0;
  let multiSuccess = 0;
  for (const { order, w } of paid) {
    const check = clients[w];
    const o = await check(`/api/orders/${order}`);
    if (o.status !== 'PAID') notPaid++;
    const ps = await check(`/api/payments/by-order/${order}`);
    if ((Array.isArray(ps) ? ps : [ps]).filter((x) => x.status === 'SUCCESS').length !== 1) multiSuccess++;
  }
  report.NFR02 = {
    workers: WORKERS,
    orders: ORDERS,
    paid: paid.length,
    errors: errors.length,
    errorSamples: errors.slice(0, 3),
    notPaid,
    ordersWithoutExactlyOneSuccess: multiSuccess,
    payMs: summary(payMs),
    within3s: payMs.filter((x) => x <= 3000).length,
    wallMs,
    pass: errors.length === 0 && paid.length === ORDERS && notPaid === 0 && multiSuccess === 0
      && Math.max(...payMs) <= 3000,
  };
  console.log('NFR02', JSON.stringify(report.NFR02));
}

fs.writeFileSync('out/perf.json', JSON.stringify(report, null, 2));
const ok = report.NFR01.pass && report.NFR02.pass && report.NFR08.pass;
console.log(ok ? 'DAT tat ca chi tieu do' : 'CO CHI TIEU KHONG DAT');
