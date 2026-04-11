# 🌐 NeoBrowser

Browser Android buatan sendiri dengan fitur-fitur premium, gratis!

## ✨ Fitur
- 🔧 **Native Userscript** — Tampermonkey/Greasemonkey compatible (GM_addStyle, GM_getValue, GM_setValue, GM_xmlhttpRequest, dll)
- ⬇️ **External Download Manager** — Otomatis kirim download ke IDM+ / ADM
- 📑 **Tab Grid & Group** — Lihat semua tab dalam grid, kelompokkan tab dengan warna
- 🌙 **Dark Mode UI** — Tampilan gelap elegan
- 🖥️ **Desktop Mode** — Toggle user agent desktop
- 🍪 **Cookie Manager** — Full cookie support

## 🚀 Cara Build APK (Tanpa PC!)

### Langkah 1: Upload ke GitHub
1. Buka [github.com](https://github.com) → login
2. Klik **"New repository"**
3. Nama: `NeoBrowser`, set **Public**
4. Klik **"Create repository"**
5. Upload semua file dari folder ini (drag & drop di browser)

### Langkah 2: Build Otomatis
1. Di repo GitHub, klik tab **"Actions"**
2. Klik **"Build APK"** workflow
3. Klik **"Run workflow"** → **"Run workflow"**
4. Tunggu ~5-10 menit ☕

### Langkah 3: Download APK
1. Setelah workflow selesai (✅ hijau)
2. Klik workflow yang baru selesai
3. Scroll ke bawah → **"Artifacts"**
4. Download **"NeoBrowser-debug"**
5. Extract ZIP → install APK di HP!

> ⚠️ Aktifkan "Install from unknown sources" di Settings HP kamu

## 📝 Cara Pakai Userscript

1. Buka menu ≡ → **Userscripts**
2. Tap tombol **+** untuk tambah script baru
3. Isi nama, URL pattern, dan kode JavaScript
4. Toggle ON untuk aktifkan

### Contoh Script:
```javascript
// Skip YouTube ads
setInterval(() => {
    const skip = document.querySelector('.ytp-skip-ad-button');
    if (skip) skip.click();
}, 500);
```

### GM_ API yang Tersedia:
- `GM_addStyle(css)` — Inject CSS
- `GM_getValue(key, default)` — Ambil data tersimpan
- `GM_setValue(key, value)` — Simpan data
- `GM_xmlhttpRequest(details)` — HTTP request
- `GM_log(msg)` — Console log
- `GM_openInTab(url)` — Buka tab baru
- `GM_setClipboard(text)` — Copy ke clipboard

## ⬇️ Download Manager (IDM+)
Browser otomatis mendeteksi IDM+ atau ADM yang terinstall dan meneruskan link download ke app tersebut.

App yang didukung:
- IDM+ (Internet Download Manager+)
- ADM (Advanced Download Manager)
- Fallback ke browser download bawaan

## 📑 Tab Groups
1. Buka tab overview (tombol tab di toolbar)
2. Tap ikon **+** di kartu tab untuk tambah ke grup
3. Beri nama grup dan pilih warna

---
Made with ❤️ | Build dengan GitHub Actions
