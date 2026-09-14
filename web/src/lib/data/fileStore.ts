export interface PickedFile {
  name: string;
  mimeType: string;
  dataUrl: string;
  sizeBytes: number;
}

function base64ToBlob(mimeType: string, base64: string): Blob {
  const bin = atob(base64.replace(/\s/g, ''));
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new Blob([bytes], { type: mimeType });
}

/** base64 → blob: URL（Chrome 的 PDF 阅读器在 iframe 里只认 blob: URL）。 */
export function base64ToBlobUrl(mimeType: string, base64: string): string {
  return URL.createObjectURL(base64ToBlob(mimeType, base64));
}

/** 打开文件选择器，返回选中文件（取消返回 null）。 */
export function pickFile(accept?: string[]): Promise<PickedFile | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    if (accept && accept.length > 0) input.accept = accept.join(',');
    let settled = false;
    const done = (v: PickedFile | null) => {
      if (!settled) {
        settled = true;
        resolve(v);
      }
    };

    const read = (file: File) => {
      const reader = new FileReader();
      reader.onload = () =>
        done({
          name: file.name,
          mimeType: file.type || 'application/octet-stream',
          dataUrl: String(reader.result),
          sizeBytes: file.size
        });
      reader.onerror = () => done(null);
      reader.readAsDataURL(file);
    };

    input.addEventListener('change', () => {
      const file = input.files && input.files[0];
      if (file) read(file);
      else done(null);
    });

    // 取消检测：文件对话框关闭后窗口重获焦点，若 change 未触发则视为取消。
    const onFocus = () => window.setTimeout(() => done(null), 500);
    window.addEventListener('focus', onFocus);
    input.addEventListener(
      'change',
      () => window.removeEventListener('focus', onFocus),
      { once: true }
    );

    input.click();
  });
}

/** 触发浏览器下载。base64 为不含 data: 前缀的纯 base64 内容。 */
export function downloadFile(name: string, mimeType: string, base64: string) {
  const url = base64ToBlobUrl(mimeType, base64);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}

/** 在新标签页中打开（用于预览 PDF 等）。 */
export function openInNewTab(name: string, mimeType: string, base64: string) {
  const url = base64ToBlobUrl(mimeType, base64);
  const opened = window.open(url, '_blank');
  if (!opened) window.location.href = url;
}

/** 图片压缩：缩放至 maxDimension 内并转 JPEG，压缩后反而更大则保留原图。 */
export function compressImageDataUrl(
  dataUrl: string,
  maxDimension: number,
  quality: number
): Promise<string> {
  if (!dataUrl.startsWith('data:image/')) return Promise.resolve(dataUrl);
  return new Promise((resolve) => {
    const idx = dataUrl.indexOf('base64,');
    if (idx < 0) return resolve(dataUrl);
    const img = new Image();
    img.onload = () => {
      const w = img.naturalWidth;
      const h = img.naturalHeight;
      if (!w || !h) return resolve(dataUrl);
      const scale = Math.min(1, maxDimension / Math.max(w, h));
      const nw = Math.max(1, Math.floor(w * scale));
      const nh = Math.max(1, Math.floor(h * scale));
      if (nw === w && nh === h && dataUrl.startsWith('data:image/jpeg')) return resolve(dataUrl);
      try {
        const canvas = document.createElement('canvas');
        canvas.width = nw;
        canvas.height = nh;
        const ctx = canvas.getContext('2d');
        if (!ctx) return resolve(dataUrl);
        ctx.drawImage(img, 0, 0, nw, nh);
        const out = canvas.toDataURL('image/jpeg', quality);
        resolve(out.length >= dataUrl.length ? dataUrl : out);
      } catch {
        resolve(dataUrl);
      }
    };
    img.onerror = () => resolve(dataUrl);
    img.src = dataUrl;
  });
}
