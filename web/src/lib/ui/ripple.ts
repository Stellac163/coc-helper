// 全局委托的水波纹：在可点击元素上按下时注入 .ripple-ink，动画结束后移除。
// 复刻 Compose Material3 的 ripple 点击反馈，无需逐组件接入。
const RIPPLE_TARGET =
  '.btn, .icon-btn, .fab, .fab-item, .list-item, .grouped-item, .nav-item, .tab, .card.clickable, .active-card, .pc-card, .tool-card, .night-row';

export function initRipple(): () => void {
  const onPointerDown = (e: PointerEvent) => {
    const el = (e.target as HTMLElement | null)?.closest?.(RIPPLE_TARGET);
    if (!el) return;
    if ((el as HTMLButtonElement).disabled) return;

    const rect = el.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height) * 2;
    const x = e.clientX - rect.left - size / 2;
    const y = e.clientY - rect.top - size / 2;

    const ink = document.createElement('span');
    ink.className = 'ripple-ink';
    ink.style.width = ink.style.height = `${size}px`;
    ink.style.left = `${x}px`;
    ink.style.top = `${y}px`;
    el.appendChild(ink);
    ink.addEventListener('animationend', () => ink.remove(), { once: true });
  };

  document.addEventListener('pointerdown', onPointerDown);
  return () => document.removeEventListener('pointerdown', onPointerDown);
}
