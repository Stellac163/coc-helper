function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

/** 极简 Markdown 渲染为 HTML：**粗体**、*斜体*、__下划线__、~~删除线~~。 */
export function markdownToHtml(text: string): string {
  let out = '';
  let i = 0;
  const n = text.length;
  while (i < n) {
    if (text.startsWith('**', i)) {
      const end = text.indexOf('**', i + 2);
      if (end > i) {
        out += `<strong>${escapeHtml(text.slice(i + 2, end))}</strong>`;
        i = end + 2;
      } else {
        out += escapeHtml(text[i]);
        i++;
      }
    } else if (text.startsWith('__', i)) {
      const end = text.indexOf('__', i + 2);
      if (end > i) {
        out += `<u>${escapeHtml(text.slice(i + 2, end))}</u>`;
        i = end + 2;
      } else {
        out += escapeHtml(text[i]);
        i++;
      }
    } else if (text.startsWith('~~', i)) {
      const end = text.indexOf('~~', i + 2);
      if (end > i) {
        out += `<s>${escapeHtml(text.slice(i + 2, end))}</s>`;
        i = end + 2;
      } else {
        out += escapeHtml(text[i]);
        i++;
      }
    } else if (text.startsWith('*', i)) {
      const end = text.indexOf('*', i + 1);
      if (end > i) {
        out += `<em>${escapeHtml(text.slice(i + 1, end))}</em>`;
        i = end + 1;
      } else {
        out += escapeHtml(text[i]);
        i++;
      }
    } else {
      out += escapeHtml(text[i]);
      i++;
    }
  }
  return out;
}

/** 用 marker 包裹当前选中文本（无选中则插入 marker 对）。 */
export function applyMarkup(
  text: string,
  start: number,
  end: number,
  marker: string
): { text: string; selStart: number; selEnd: number } {
  const selected = text.slice(start, end);
  const newText = text.slice(0, start) + marker + selected + marker + text.slice(end);
  const newSelStart = start + marker.length;
  const newSelEnd = start === end ? newSelStart : newSelStart + selected.length;
  return { text: newText, selStart: newSelStart, selEnd: newSelEnd };
}
