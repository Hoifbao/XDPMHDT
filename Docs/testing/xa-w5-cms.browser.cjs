// Run with Playwright installed and CHROME_EXECUTABLE pointing to a Chromium browser.
// Uses an isolated mock API; Java API tests cover actual file persistence/validation.
const { chromium } = require('playwright');
const http = require('node:http');
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../../WebAdmin');
const languages = [{code: 'vi', name: 'Tiếng Việt', active: true}, {code: 'en', name: 'English', active: true}];
let saved;
let uploads = 0;
const artifact = {id: 1, siteId: 1, name: 'Trống đồng', status: 'ACTIVE',
  contents: [{languageCode: 'vi', title: 'Trống đồng', description: 'Mô tả', imageUrls: '[]'}], media: []};
const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    const send = (data, status = 200) => { res.writeHead(status, {'Content-Type': 'application/json; charset=utf-8'}); res.end(JSON.stringify({success: true, data})); };
    const chunks = []; for await (const chunk of req) chunks.push(chunk);
    const raw = Buffer.concat(chunks);
    const body = () => JSON.parse(raw.toString('utf8'));
    if (url.pathname === '/api/admin/languages') {
      if (req.method === 'POST') languages.push(body());
      return send(languages);
    }
    if (url.pathname.startsWith('/api/admin/languages/')) {
      Object.assign(languages.find(l => l.code === url.pathname.split('/').pop()), body());
      return send(languages);
    }
    if (url.pathname === '/api/heritage-sites') return send([{id: 1, name: 'Bảo tàng'}]);
    if (url.pathname === '/api/artifacts') return send([artifact]);
    if (url.pathname === '/api/artifacts/1') {
      if (req.method === 'PUT') { saved = body(); Object.assign(artifact, saved); }
      return send(artifact);
    }
    if (url.pathname === '/api/media/upload') {
      assert.match(req.headers['content-type'], /multipart\/form-data; boundary=/);
      if (raw.includes(Buffer.from('bad.svg'))) {
        res.writeHead(400, {'Content-Type': 'application/json'});
        return res.end(JSON.stringify({success: false, message: 'Định dạng không hỗ trợ'}));
      }
      assert.ok(raw.includes(Buffer.from('photo.png')) || raw.includes(Buffer.from('audio.mp3')));
      uploads++;
      const audio = raw.includes(Buffer.from('audio.mp3'));
      return send({fileUrl: `http://127.0.0.1:${server.address().port}/api/media/files/${audio ? 'audio.mp3' : 'photo.png'}`, mediaType: audio ? 'AUDIO' : 'IMAGE', size: 32}, 201);
    }
    const filename = path.resolve(root, '.' + decodeURIComponent(url.pathname));
    if (!filename.startsWith(root + path.sep)) { res.writeHead(404); return res.end(); }
    const content = await fs.readFile(filename);
    res.writeHead(200, {'Content-Type': ({'.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css'})[path.extname(filename)] || 'application/octet-stream'});
    res.end(content);
  } catch (error) { res.writeHead(500); res.end(String(error)); }
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  let browser;
  try {
    browser = await chromium.launch({headless: true, executablePath: process.env.CHROME_EXECUTABLE});
    const page = await browser.newPage();
    const errors = []; page.on('pageerror', error => errors.push(error.message));
    await page.addInitScript(() => {
      localStorage.setItem('sh_access_token', 'cms-test-token');
      localStorage.setItem('sh_auth_user', JSON.stringify({userId: 1, username: 'admin', role: 'ADMIN'}));
    });
    await page.goto(`${origin}/src/pages/artifacts.html?id=1&api=${encodeURIComponent(origin)}`);
    await page.getByText('Dữ liệu được lưu qua Gateway.', {exact: true}).waitFor();
    const languageForm = page.locator('#language-form');
    await languageForm.locator('[name=code]').fill('fr');
    await languageForm.locator('[name=name]').fill('Français');
    await languageForm.getByRole('button').click();
    await page.getByText('Đã thêm ngôn ngữ.', {exact: true}).waitFor();
    await page.locator('#add-translation').click();
    await page.locator('#add-translation').click();
    assert.equal(await page.locator('#translations select[name="language_code[]"]').count(), 3);
    const fr = page.locator('#translations .repeat-item').nth(2);
    assert.equal(await fr.locator('select[name="language_code[]"]').inputValue(), 'fr');
    const en = page.locator('#translations .repeat-item').nth(1);
    for (const row of [en, fr]) {
      await row.locator('[name="content_title[]"]').fill('Translated title');
      await row.locator('[name="content_description[]"]').fill('Translated description');
    }
    const vi = page.locator('#translations .repeat-item').first();
    await vi.locator('[name="image_urls[]"]').locator('..').locator('input[type=file]').setInputFiles({name: 'photo.png', mimeType: 'image/png', buffer: Buffer.from([137,80,78,71,13,10,26,10])});
    await page.waitForFunction(() => document.querySelector('[name="image_urls[]"]').value.includes('/api/media/files/'));
    const imageUrl = await vi.locator('[name="image_urls[]"]').inputValue();
    await vi.locator('[name="audio_url[]"]').locator('..').locator('input[type=file]').setInputFiles({name: 'audio.mp3', mimeType: 'audio/mpeg', buffer: Buffer.from('ID3' + '0'.repeat(29))});
    await page.waitForFunction(() => document.querySelector('[name="audio_url[]"]').value.includes('/audio.mp3'));
    await vi.locator('[name="image_urls[]"]').locator('..').locator('input[type=file]').setInputFiles({name: 'bad.svg', mimeType: 'image/svg+xml', buffer: Buffer.from('<svg/>')});
    await page.getByText('Không tải được: Định dạng không hỗ trợ', {exact: true}).waitFor();
    assert.equal(await vi.locator('[name="image_urls[]"]').inputValue(), imageUrl);
    await page.locator('#artifact-form [type=submit]').click();
    await page.getByText('Đã lưu hiện vật #1.', {exact: true}).waitFor();
    assert.equal(saved.contents.length, 3);
    assert.ok(saved.contents[0].audioUrl.endsWith('/audio.mp3'));
    assert.ok(JSON.parse(saved.contents[0].imageUrls)[0].endsWith('/photo.png'));
    assert.equal(uploads, 2);
    assert.deepEqual(errors, []);
    console.log('PASS CMS: dynamic languages, three translations, image/audio multipart upload, rejected upload preserves URL, artifact save, no page errors.');
  } finally { await browser?.close(); await new Promise(resolve => server.close(resolve)); }
})().catch(error => { console.error(error); process.exitCode = 1; });
