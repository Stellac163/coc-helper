<script lang="ts">
  import { settingsStore } from '$lib/data/settingsStore.svelte';
  import { repo } from '$lib/data/repository.svelte';
  import { getUser, type HttpProgress } from '$lib/sync/github';
  import { pickFile, compressImageDataUrl } from '$lib/data/fileStore';
  import { snackbar } from '$lib/ui/snackbar.svelte';
  import Icon from '$lib/components/Icon.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import LoadedImage from '$lib/components/LoadedImage.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const settings = $derived(settingsStore.settings);

  let token = $state(settingsStore.settings.githubToken);
  let repoName = $state(settingsStore.settings.repoName);
  let nickname = $state(settingsStore.settings.nickname);
  let loading = $state(false);
  let progress = $state<HttpProgress | null>(null);

  function notify(msg: string) {
    snackbar.show(msg);
  }

  async function doLogin() {
    loading = true;
    try {
      const repoNameValue = repoName.trim() || 'coc-helper-backup';
      const user = await getUser(token);
      settingsStore.setGithubToken(token);
      settingsStore.setRepoName(repoNameValue);
      if (nickname.trim()) settingsStore.setNickname(nickname);
      // 仅在尚未设置自定义头像时用 GitHub 头像兜底，避免覆盖用户自己上传的头像
      if (!settingsStore.settings.avatarUri && user.avatarUrl) settingsStore.setAvatarUri(user.avatarUrl);
      notify(`登录成功：${user.login}`);
    } catch (e) {
      notify(`登录失败：${(e as Error).message}`);
    }
    loading = false;
  }

  async function upload() {
    loading = true;
    progress = null;
    try {
      const repoNameValue = repoName.trim() || 'coc-helper-backup';
      settingsStore.setRepoName(repoNameValue);
      const msg = await repo.uploadToCloud(token, repoNameValue, (p) => (progress = p));
      notify(msg);
    } catch (e) {
      notify(`上传失败：${(e as Error).message}`);
    }
    loading = false;
    progress = null;
  }

  async function restore() {
    loading = true;
    progress = null;
    try {
      const repoNameValue = repoName.trim() || 'coc-helper-backup';
      settingsStore.setRepoName(repoNameValue);
      const msg = await repo.restoreFromCloud(token, repoNameValue, (p) => (progress = p));
      notify(msg);
    } catch (e) {
      notify(`恢复失败：${(e as Error).message}`);
    }
    loading = false;
    progress = null;
  }

  async function pickAvatar() {
    const picked = await pickFile(['image/*']);
    if (!picked) return;
    const compressed = await compressImageDataUrl(picked.dataUrl, 256, 0.82);
    settingsStore.setAvatarUri(compressed);
    notify('头像已更新');
  }

  function logout() {
    settingsStore.setGithubToken('');
    token = '';
    notify('已退出登录');
  }

  const showSync = $derived(settings.githubToken !== '' || token !== '');
</script>

<div class="page">
  <SectionTopBar title="登录与同步" onback={() => history.back()} />
  <div class="page-body">
    <div class="col" style="gap:12px;">
      <p class="muted" style="white-space: pre-line; margin:0;">使用 GitHub 私有仓库备份。令牌仅保存在本机。
「上传」用本地覆盖云端；「从云端恢复」用云端覆盖本地（单向，不合并）。</p>

      <TextField label="GitHub 访问令牌" type="password" bind:value={token} />
      <TextField label="仓库名（私有）" bind:value={repoName} />
      <TextField label="昵称" bind:value={nickname} />

      <div class="row" style="gap:16px;">
        <div class="login-avatar"><LoadedImage uri={settings.avatarUri || null} radius={36} icon="person" /></div>
        <button class="btn outlined" onclick={pickAvatar}>
          <Icon name="add_a_photo" size={18} />
          <span>更换头像</span>
        </button>
      </div>

      {#if loading}
        <div class="col" style="gap:8px;">
          {#if progress && progress.total > 0}
            <div class="progress"><div class="bar" style="width:{Math.round((progress.loaded * 100) / progress.total)}%;"></div></div>
            <div class="muted small">{progress.phase === 'UPLOAD' ? '上传' : '下载'}中 {Math.round((progress.loaded * 100) / progress.total)}%</div>
          {:else}
            <div class="progress indeterminate"><div class="bar"></div></div>
            <div class="muted small">处理中…</div>
          {/if}
        </div>
      {/if}

      <button class="btn filled login-btn" disabled={!token.trim() || loading} onclick={doLogin}>
        <Icon name="login" size={20} />
        <span>登录并验证</span>
      </button>

      {#if showSync}
        <button class="btn filled login-btn" disabled={!token.trim() || loading} onclick={upload}>
          <Icon name="cloud_upload" size={20} />
          <span>上传（覆盖云端）</span>
        </button>
        <button class="btn outlined login-btn" disabled={!token.trim() || loading} onclick={restore}>
          <Icon name="cloud_download" size={20} />
          <span>从云端恢复（覆盖本地）</span>
        </button>
        <button class="btn text" onclick={logout}>
          <Icon name="logout" size={18} />
          <span>退出登录</span>
        </button>
      {/if}
    </div>
  </div>
</div>

<style>
  .login-btn {
    width: 100%;
    height: 56px;
    border-radius: 28px;
  }
  .login-avatar {
    width: 72px;
    height: 72px;
    flex: 0 0 auto;
  }
  .progress.indeterminate .bar {
    width: 40%;
    animation: login-slide 1.2s ease-in-out infinite;
  }
  @keyframes login-slide {
    0% { margin-left: 0; }
    50% { margin-left: 60%; }
    100% { margin-left: 0; }
  }
</style>
