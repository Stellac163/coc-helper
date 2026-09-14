<script lang="ts">
  import { goto } from '$app/navigation';
  import { settingsStore } from '$lib/data/settingsStore.svelte';
  import { R } from '$lib/nav/routes';
  import Icon from '$lib/components/Icon.svelte';
  import LoadedImage from '$lib/components/LoadedImage.svelte';
  import Switch from '$lib/components/Switch.svelte';

  const settings = $derived(settingsStore.settings);
  let night = $state(settingsStore.settings.themeMode === 'DARK');

  function onNight(checked: boolean) {
    settingsStore.setThemeMode(checked ? 'DARK' : 'SYSTEM');
  }
</script>

<div class="page">
  <div class="page-body">
    <div class="welcome-header">
      <div class="welcome-center">
        <div class="row" style="gap:16px;">
          <div class="welcome-avatar">
            <LoadedImage uri={settings.avatarUri || null} radius={20} icon="person" iconSize={56} />
          </div>
          <div class="welcome-hello">欢迎！</div>
        </div>
        <div class="welcome-nick">{settings.nickname || '调查员'}</div>
      </div>
      <button class="fab welcome-fab" onclick={() => goto(R.login)} aria-label="登录">
        <Icon name="login" size={24} />
      </button>
    </div>

    <div class="tools-body">
      <div class="night-row">
        <span class="circle-badge"><Icon name="dark_mode" size={24} /></span>
        <div class="grow night-label">夜间模式</div>
        <Switch bind:checked={night} onchange={onNight} />
      </div>

      <div class="row" style="gap:12px;">
        <button class="tool-card" onclick={() => goto(R.combat)}>
          <div class="tool-icon"><Icon name="shield" size={40} /></div>
          <div class="tool-title">战斗轮小助手</div>
        </button>
        <button class="tool-card" onclick={() => goto(R.chase)}>
          <div class="tool-icon"><Icon name="directions_run" size={40} /></div>
          <div class="tool-title">追逐战小助手</div>
        </button>
      </div>

      <div class="row" style="gap:12px;">
        <button class="tool-card" onclick={() => goto(R.timer)}>
          <div class="tool-icon"><Icon name="timer" size={40} /></div>
          <div class="tool-title">计时器</div>
        </button>
        <button class="tool-card" onclick={() => goto(R.clues)}>
          <div class="tool-icon"><Icon name="lightbulb" size={40} /></div>
          <div class="tool-title">线索板</div>
        </button>
      </div>
    </div>
  </div>
</div>

<style>
  .tools-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px 0;
  }
</style>
