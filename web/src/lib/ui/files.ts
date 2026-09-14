/** 依据 mimeType 返回对应图标名（对齐 Kotlin fileTypeIcon）。 */
export function fileTypeIcon(mimeType: string): string {
  if (mimeType.startsWith('image/')) return 'image';
  if (mimeType.startsWith('audio/')) return 'audio_file';
  if (mimeType.startsWith('video/')) return 'insert_drive_file';
  if (mimeType === 'application/pdf' || mimeType.includes('word') || mimeType.includes('document'))
    return 'description';
  if (mimeType.startsWith('text/')) return 'description';
  return 'insert_drive_file';
}

function round1(v: number): string {
  const r = Math.round(v * 10) / 10;
  return Number.isInteger(r) ? String(r) : String(r);
}

export function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${round1(bytes / 1024)} KB`;
  return `${round1(bytes / (1024 * 1024))} MB`;
}
