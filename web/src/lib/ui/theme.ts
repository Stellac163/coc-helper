import { settingsStore } from '$lib/data/settingsStore.svelte';

function systemDark(): boolean {
  return typeof matchMedia !== 'undefined' && matchMedia('(prefers-color-scheme: dark)').matches;
}

export function effectiveDark(): boolean {
  const m = settingsStore.settings.themeMode;
  return m === 'DARK' || (m === 'SYSTEM' && systemDark());
}

export function applyTheme() {
  document.documentElement.dataset.theme = effectiveDark() ? 'dark' : 'light';
}
