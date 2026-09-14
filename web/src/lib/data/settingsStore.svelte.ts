import type { AppSettings, ThemeMode } from './types';

const KEY = 'cochelper_settings_v1';

function defaultSettings(): AppSettings {
  return {
    themeMode: 'SYSTEM',
    githubToken: '',
    repoName: 'coc-helper-backup',
    nickname: '',
    avatarUri: ''
  };
}

function load(): AppSettings {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return defaultSettings();
    const p = JSON.parse(raw);
    const themeMode: ThemeMode =
      p?.themeMode === 'LIGHT' || p?.themeMode === 'DARK' ? p.themeMode : 'SYSTEM';
    return {
      themeMode,
      githubToken: typeof p?.githubToken === 'string' ? p.githubToken : '',
      repoName: typeof p?.repoName === 'string' && p.repoName ? p.repoName : 'coc-helper-backup',
      nickname: typeof p?.nickname === 'string' ? p.nickname : '',
      avatarUri: typeof p?.avatarUri === 'string' ? p.avatarUri : ''
    };
  } catch {
    return defaultSettings();
  }
}

class SettingsStore {
  settings = $state<AppSettings>(load());

  private persist() {
    try {
      localStorage.setItem(KEY, JSON.stringify(this.settings));
    } catch {
      // 忽略写入失败
    }
  }

  setThemeMode(mode: ThemeMode) {
    this.settings = { ...this.settings, themeMode: mode };
    this.persist();
  }

  setGithubToken(token: string) {
    this.settings = { ...this.settings, githubToken: token };
    this.persist();
  }

  setRepoName(name: string) {
    this.settings = { ...this.settings, repoName: name };
    this.persist();
  }

  setNickname(nickname: string) {
    this.settings = { ...this.settings, nickname };
    this.persist();
  }

  setAvatarUri(uri: string) {
    this.settings = { ...this.settings, avatarUri: uri };
    this.persist();
  }
}

export const settingsStore = new SettingsStore();
