// 让 <textarea> 高度随内容自动增长（对齐 Compose 多行 OutlinedTextField 的“文字多长就多长”）。

// 找到最近的「垂直可滚动」祖先容器（.app-content，或对话框里的 .dialog）。
function scrollParent(el: HTMLElement): HTMLElement | null {
  let p = el.parentElement;
  while (p) {
    const oy = getComputedStyle(p).overflowY;
    if (oy === 'auto' || oy === 'scroll') return p;
    p = p.parentElement;
  }
  return null;
}

export function autosize(node: HTMLTextAreaElement, _value?: string) {
  const container = scrollParent(node);

  const resize = () => {
    const prevTop = container ? container.scrollTop : 0;

    // 先塌陷到 auto 测得真实内容高度，再回填。塌陷的瞬间会把滚动容器钳制回顶部，
    // 因此先记下原滚动位置，测量后再恢复，避免「输入时页面突然跳回顶部」。
    node.style.height = 'auto';
    node.style.height = `${node.scrollHeight}px`;

    if (!container) return;

    if (container.scrollTop !== prevTop) container.scrollTop = prevTop;

    // 光标在末尾、正在连续书写时：textarea 增长但浏览器不会自动滚动祖先容器，
    // 这里手动跟随，保证最后一行（光标所在行）始终可见。底部留出固定工具栏的高度。
    if (document.activeElement === node) {
      const len = node.value.length;
      const sel = node.selectionEnd ?? len;
      if (sel >= len - 1) {
        const cRect = container.getBoundingClientRect();
        const nRect = node.getBoundingClientRect();
        const clearance = 88; // 底部固定工具栏/FAB 条高度
        const target = cRect.bottom - clearance;
        if (nRect.bottom > target) {
          container.scrollTop += nRect.bottom - target;
        }
      }
    }
  };

  resize();
  node.addEventListener('input', resize);
  return {
    // 当绑定的值被程序性修改（例如 wrap 加粗/斜体）时，Svelte 会调用 update 重新测量。
    update() {
      requestAnimationFrame(resize);
    },
    destroy() {
      node.removeEventListener('input', resize);
    },
  };
}
