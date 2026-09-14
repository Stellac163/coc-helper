// 生成 PWA 图标（无外部依赖）：紫色底 + 白色骰子「5」点面。
// 输出 static/pwa-192.png 与 static/pwa-512.png。
import { writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { deflateSync } from 'node:zlib';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'static');
mkdirSync(outDir, { recursive: true });

const SEED = [0x67, 0x50, 0xa4]; // #6750A4

// ---- CRC32 ----
let crcTable;
function crc32(buf) {
  if (!crcTable) {
    crcTable = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      crcTable[n] = c;
    }
  }
  let crc = -1;
  for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ crcTable[(crc ^ buf[i]) & 0xff];
  return (crc ^ -1) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const t = Buffer.from(type, 'ascii');
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([t, data])), 0);
  return Buffer.concat([len, t, data, crc]);
}

function encodePng(size, rgba) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // color type RGBA
  ihdr[10] = 0; // compression
  ihdr[11] = 0; // filter
  ihdr[12] = 0; // interlace
  const stride = size * 4 + 1;
  const raw = Buffer.alloc(stride * size);
  for (let y = 0; y < size; y++) {
    raw[y * stride] = 0; // filter: none
    rgba.copy(raw, y * stride + 1, y * size * 4, (y + 1) * size * 4);
  }
  const idat = deflateSync(raw, { level: 9 });
  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0))]);
}

// 圆形覆盖率（0..1），带 ~1.5px 抗锯齿边缘
function circle(cx, cy, r, nx, ny, size) {
  const d = (Math.hypot(nx - cx, ny - cy) - r) * size;
  return Math.max(0, Math.min(1, 0.5 - d / 1.5));
}

function render(size) {
  const rgba = Buffer.alloc(size * size * 4);
  const pips = [
    [0.5, 0.5],
    [0.36, 0.36],
    [0.64, 0.36],
    [0.36, 0.64],
    [0.64, 0.64]
  ];
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const nx = (x + 0.5) / size;
      const ny = (y + 0.5) / size;
      let r = SEED[0];
      let g = SEED[1];
      let b = SEED[2];
      // 白色骰子面
      const face = circle(0.5, 0.5, 0.34, nx, ny, size);
      r += (255 - r) * face;
      g += (255 - g) * face;
      b += (255 - b) * face;
      // 面上的紫色点数
      let pip = 0;
      for (const [px, py] of pips) pip = Math.max(pip, circle(px, py, 0.058, nx, ny, size));
      r += (SEED[0] - r) * pip;
      g += (SEED[1] - g) * pip;
      b += (SEED[2] - b) * pip;
      const i = (y * size + x) * 4;
      rgba[i] = Math.round(r);
      rgba[i + 1] = Math.round(g);
      rgba[i + 2] = Math.round(b);
      rgba[i + 3] = 255;
    }
  }
  return rgba;
}

for (const size of [192, 512]) {
  const png = encodePng(size, render(size));
  writeFileSync(join(outDir, `pwa-${size}.png`), png);
  console.log(`wrote static/pwa-${size}.png (${png.length} bytes)`);
}
