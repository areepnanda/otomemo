/**
 * OtoMemo - Sinkronisasi ke Google Sheet
 *
 * CARA PASANG (sekali saja):
 * 1. Buat Google Sheet baru (sheet kosong, tidak perlu isi apa-apa)
 * 2. Menu Extensions (Ekstensi) -> Apps Script
 * 3. Hapus semua isi Code.gs, ganti dengan file ini
 * 4. Ganti nilai TOKEN di bawah dengan kata rahasia apa pun
 *    (harus sama dengan yang diisi di app OtoMemo)
 * 5. Deploy -> New deployment -> pilih tipe "Web app"
 *    - Description: otomemo sync
 *    - Execute as: Me (akun kamu)
 *    - Who has access: Anyone
 * 6. Klik Deploy, izinkan akses (Authorize) saat diminta
 * 7. Copy "Web app URL" (berakhiran /exec)
 * 8. Di app OtoMemo: buka drawer -> "Sinkron Google Sheet"
 *    -> isi URL + token -> "Kirim semua data"
 *
 * CATATAN:
 * - Tab Vehicles, Items, Workshops, Records dibuat otomatis di sheet.
 * - Add/edit/delete dari app otomatis terkirim ke sheet.
 * - Edit/add/hapus baris di sheet otomatis ketarik ke app
 *   saat app dibuka (atau lewat tombol "Tarik data dari sheet").
 * - Kalau HP offline, perubahan masuk antrean dan terkirim saat online.
 * - KALAU SUDAH PERNAH PASANG versi lama: replace isi file ini, lalu
 *   Deploy -> Manage deployments -> ikon pensil -> Version: New version -> Deploy.
 * - Ganti TOKEN kalau URL kebocoran: cukup ubah TOKEN di sini,
 *   deploy ulang (New deployment atau edit deployment), lalu update token di app.
 */

const TOKEN = 'Wtx^b0lSe#g8HF3gGn@Vm9oR3ro4uPkum5kS7^pJc4@=';

const TABLES = {
  vehicles: { name: 'Vehicles', cols: ['id', 'brand', 'model', 'year', 'plate', 'type'] },
  items: { name: 'Items', cols: ['id', 'name', 'type'] },
  workshops: { name: 'Workshops', cols: ['id', 'name'] },
  records: {
    name: 'Records',
    cols: ['id', 'vehicleId', 'vehicleName', 'plateNumber', 'date', 'workshop',
      'odometer', 'items', 'cost', 'notes', 'images']
  }
};

function doPost(e) {
  let res;
  try {
    const req = JSON.parse(e.postData.contents);
    if (req.token !== TOKEN) throw new Error('Token salah');
    if (req.action === 'fetchAll') {
      res = { ok: true, data: fetchAll_() };
    } else {
      switch (req.action) {
        case 'replaceAll': replaceAll_(req.data); break;
        case 'upsert': upsert_(req.table, req.row); break;
        case 'delete': deleteRow_(req.table, req.id); break;
        default: throw new Error('Action tidak dikenal: ' + req.action);
      }
      res = { ok: true };
    }
  } catch (err) {
    res = { ok: false, error: String(err && err.message ? err.message : err) };
  }
  return ContentService.createTextOutput(JSON.stringify(res))
    .setMimeType(ContentService.MimeType.JSON);
}

const NUMERIC_COLS = { id: true, vehicleId: true, odometer: true, cost: true };

function readRows_(key) {
  const s = sheet_(key);
  const last = s.sh.getLastRow();
  if (last < 2) return [];
  const values = s.sh.getRange(2, 1, last - 1, s.conf.cols.length).getValues();
  return values.map(function (row) {
    const obj = {};
    s.conf.cols.forEach(function (c, i) {
      const raw = row[i];
      let v;
      if (raw instanceof Date) {
        v = Utilities.formatDate(raw, Session.getScriptTimeZone(), 'dd MMM yyyy');
      } else {
        v = (raw === null || raw === undefined) ? '' : String(raw);
      }
      if (NUMERIC_COLS[c] && v !== '') v = Number(v);
      if (c === 'images') {
        obj[c] = v ? v.split(',').map(function (x) { return x.trim(); }).filter(String) : [];
      } else {
        obj[c] = v;
      }
    });
    return obj;
  });
}

function fetchAll_() {
  const data = {};
  for (const key in TABLES) data[key] = readRows_(key);
  return data;
}

function sheet_(key) {
  const conf = TABLES[key];
  if (!conf) throw new Error('Table tidak dikenal: ' + key);
  const ss = SpreadsheetApp.getActive();
  let sh = ss.getSheetByName(conf.name);
  if (!sh) {
    sh = ss.insertSheet(conf.name);
    sh.getRange(1, 1, 1, conf.cols.length).setValues([conf.cols]).setFontWeight('bold');
    sh.setFrozenRows(1);
  }
  return { sh: sh, conf: conf };
}

function rowValues_(row, conf) {
  return conf.cols.map(function (c) {
    const v = row[c];
    if (v === undefined || v === null) return '';
    if (Array.isArray(v)) return v.join(', ');
    return v;
  });
}

function replaceAll_(data) {
  for (const key in TABLES) {
    const s = sheet_(key);
    const rows = (data && data[key]) || [];
    s.sh.clearContents();
    s.sh.getRange(1, 1, 1, s.conf.cols.length).setValues([s.conf.cols]).setFontWeight('bold');
    s.sh.setFrozenRows(1);
    if (rows.length) {
      s.sh.getRange(2, 1, rows.length, s.conf.cols.length)
        .setValues(rows.map(function (r) { return rowValues_(r, s.conf); }));
    }
  }
}

function upsert_(key, row) {
  const s = sheet_(key);
  const values = rowValues_(row, s.conf);
  const last = s.sh.getLastRow();
  const ids = last > 1 ? s.sh.getRange(2, 1, last - 1, 1).getValues() : [];
  for (let i = 0; i < ids.length; i++) {
    if (String(ids[i][0]) === String(values[0])) {
      s.sh.getRange(i + 2, 1, 1, values.length).setValues([values]);
      return;
    }
  }
  s.sh.getRange(last + 1, 1, 1, values.length).setValues([values]);
}

function deleteRow_(key, id) {
  const s = sheet_(key);
  const last = s.sh.getLastRow();
  if (last < 2) return;
  const ids = s.sh.getRange(2, 1, last - 1, 1).getValues();
  for (let i = ids.length - 1; i >= 0; i--) {
    if (String(ids[i][0]) === String(id)) s.sh.deleteRow(i + 2);
  }
}
