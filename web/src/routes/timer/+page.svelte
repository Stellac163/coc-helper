<script lang="ts">
  import Icon from '$lib/components/Icon.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';

  let minutes = $state('1');
  let seconds = $state('0');
  let remaining = $state(60);
  let running = $state(false);

  const digits = (v: string, max: number) => v.replace(/\D/g, '').slice(0, max);

  function setTime() {
    const m = parseInt(minutes, 10) || 0;
    const s = parseInt(seconds, 10) || 0;
    remaining = Math.min(Math.max(m * 60 + s, 0), 59 * 60 + 59);
    running = false;
  }

  $effect(() => {
    if (!running) return;
    const id = setInterval(() => {
      if (remaining > 0) remaining -= 1;
      if (remaining <= 0) running = false;
    }, 1000);
    return () => clearInterval(id);
  });

  const mmStr = $derived(String(Math.floor(remaining / 60)).padStart(2, '0'));
  const ssStr = $derived(String(remaining % 60).padStart(2, '0'));
</script>

<div class="page">
  <SectionTopBar title="计时器" onback={() => history.back()} />
  <div class="page-body timer-body">
    <div class="timer-display">{mmStr}:{ssStr}</div>

    <div class="row" style="gap:12px; width:100%;">
      <div class="field grow">
        <label class="label">分</label>
        <input class="text-input" inputmode="numeric" value={minutes} oninput={(e) => (minutes = digits(e.currentTarget.value, 2))} />
      </div>
      <div class="field grow">
        <label class="label">秒</label>
        <input class="text-input" inputmode="numeric" value={seconds} oninput={(e) => (seconds = digits(e.currentTarget.value, 2))} />
      </div>
    </div>

    <button class="btn text" onclick={setTime}>设置时间</button>

    <div class="row" style="gap:12px; width:100%;">
      <button class="btn filled timer-btn grow" onclick={() => (running = !running)}>
        <Icon name={running ? 'pause' : 'play_arrow'} size={20} />
        <span>{running ? '暂停' : '开始'}</span>
      </button>
      <button class="btn filled timer-btn grow" onclick={setTime}>
        <Icon name="replay" size={20} />
        <span>重置</span>
      </button>
    </div>
  </div>
</div>

<style>
  .timer-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }
  .timer-display {
    font-size: 96px;
    line-height: 1;
    color: var(--text);
    padding: 48px 0;
    font-variant-numeric: tabular-nums;
    text-align: center;
  }
  .timer-btn {
    height: 56px;
    border-radius: 28px;
  }
</style>
