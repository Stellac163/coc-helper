<script lang="ts">
  import { roll, LEVEL_LABEL, type DiceResult, type SuccessLevel } from '$lib/domain/dice';
  import Icon from '$lib/components/Icon.svelte';

  let faces = $state('100');
  let count = $state('1');
  let skill = $state('');
  let bonus = $state('0');
  let penalty = $state('0');
  let opponent = $state('');
  let result = $state<DiceResult | null>(null);

  const digits = (v: string, max: number) => v.replace(/\D/g, '').slice(0, max);
  const clampInt = (v: string, min: number, max: number, fallback: number) => {
    const n = parseInt(v, 10);
    return Number.isNaN(n) ? fallback : Math.min(Math.max(n, min), max);
  };
  const toIntOrNull = (v: string): number | null => {
    const n = parseInt(v, 10);
    return Number.isNaN(n) ? null : n;
  };

  function doRoll() {
    const f = clampInt(faces, 1, 10000, 100);
    const c = clampInt(count, 1, 100, 1);
    const s = toIntOrNull(skill);
    const b = clampInt(bonus, 0, 10, 0);
    const p = clampInt(penalty, 0, 10, 0);
    const o = toIntOrNull(opponent);
    result = roll(f, c, s, b, p, o);
  }

  function levelColor(level: SuccessLevel): string {
    switch (level) {
      case 'CRITICAL': return 'var(--accent)';
      case 'EXTREME': return 'var(--md-tertiary)';
      case 'HARD': return 'var(--accent)';
      case 'SUCCESS': return 'var(--text)';
      case 'FAILURE': return 'var(--text-muted)';
      case 'FUMBLE': return 'var(--danger)';
      default: return 'var(--text)';
    }
  }
</script>

<div class="page">
  <div class="page-body">
    <div class="page-title">骰子</div>

    <div class="result-box">
      {#if !result}
        <div class="col" style="align-items:center; gap:12px;">
          <Icon name="sync" size={40} />
          <div class="muted">设置好参数后点击“投掷”</div>
        </div>
      {:else if result.level}
        <div class="col" style="align-items:center; gap:6px;">
          <div class="result-level" style="color:{levelColor(result.level)};">{LEVEL_LABEL[result.level]}</div>
          {#if result.rollValue != null}
            <div class="result-value">投掷点数：{result.rollValue}</div>
          {/if}
          <div class="muted result-summary">{result.summary}</div>
        </div>
      {:else}
        <div class="col" style="align-items:center; gap:10px;">
          <div class="result-total">{result.total}</div>
          <div class="muted result-summary">{result.summary}</div>
        </div>
      {/if}
    </div>

    <div class="row" style="gap:12px;">
      <div class="field grow">
        <label class="label">骰子面数</label>
        <input class="text-input" inputmode="numeric" value={faces} oninput={(e) => (faces = digits(e.currentTarget.value, 6))} />
      </div>
      <div class="field grow">
        <label class="label">骰子数目</label>
        <input class="text-input" inputmode="numeric" value={count} oninput={(e) => (count = digits(e.currentTarget.value, 4))} />
      </div>
    </div>

    <div class="field">
      <label class="label">技能数值</label>
      <input class="text-input" inputmode="numeric" value={skill} oninput={(e) => (skill = digits(e.currentTarget.value, 4))} />
    </div>

    <div class="row" style="gap:12px;">
      <div class="field grow">
        <label class="label">奖励骰</label>
        <input class="text-input" inputmode="numeric" value={bonus} oninput={(e) => (bonus = digits(e.currentTarget.value, 2))} />
      </div>
      <div class="field grow">
        <label class="label">惩罚骰</label>
        <input class="text-input" inputmode="numeric" value={penalty} oninput={(e) => (penalty = digits(e.currentTarget.value, 2))} />
      </div>
    </div>

    <div class="field">
      <label class="label">对抗者技能数值</label>
      <input class="text-input" inputmode="numeric" value={opponent} oninput={(e) => (opponent = digits(e.currentTarget.value, 4))} />
    </div>

    <button class="btn filled roll-btn" onclick={doRoll}>
      <Icon name="sync" size={20} />
      <span>投掷</span>
    </button>
  </div>
</div>

<style>
  .result-box {
    min-height: 220px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-container-high);
    border-radius: 28px;
    padding: 20px;
    margin-bottom: 16px;
  }
  .result-level {
    font-size: 2.8rem;
    font-weight: 600;
    text-align: center;
  }
  .result-value {
    font-size: 1.75rem;
    font-weight: 600;
    text-align: center;
    color: var(--text);
  }
  .result-total {
    font-size: 3.4rem;
    font-weight: 600;
    text-align: center;
    color: var(--text);
  }
  .result-summary {
    text-align: center;
    white-space: pre-line;
  }
  .roll-btn {
    width: 100%;
    height: 56px;
    margin-top: 16px;
    border-radius: 28px;
    font-size: 1.05rem;
  }
</style>
