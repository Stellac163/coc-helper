<script lang="ts">
  import { repo } from '$lib/data/repository.svelte';
  import { newCombatant } from '$lib/data/factories';
  import {
    COMBATANT_ALIVE,
    COMBATANT_UNCONSCIOUS,
    COMBATANT_DEAD,
    type CombatantEntity
  } from '$lib/data/types';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';
  import Switch from '$lib/components/Switch.svelte';

  const combatants = $derived(repo.combat.all());

  let turn = $state(0);
  let showAdd = $state(false);
  let editing = $state<CombatantEntity | null>(null);
  let newName = $state('');
  let newDex = $state('50');
  let newHp = $state('10');
  let newRanged = $state(false);

  let editHp = $state(0);
  let editStatus = $state(COMBATANT_ALIVE);
  let editRanged = $state(false);

  const digits = (v: string, max: number) => v.replace(/\D/g, '').slice(0, max);

  function statusLabel(s: number): string {
    return s === COMBATANT_UNCONSCIOUS ? '昏迷' : s === COMBATANT_DEAD ? '死亡' : '存活';
  }
  function statusClass(s: number): string {
    return s === COMBATANT_UNCONSCIOUS ? 'unconscious' : s === COMBATANT_DEAD ? 'dead' : 'alive';
  }

  function addCombatant() {
    if (!newName.trim()) return;
    const dex = parseInt(newDex, 10) || 50;
    const hp = parseInt(newHp, 10) || 10;
    repo.combat.insert(newCombatant(newName.trim(), dex, hp, hp, newRanged));
    showAdd = false;
    newName = '';
    newDex = '50';
    newHp = '10';
    newRanged = false;
  }

  function openEdit(c: CombatantEntity) {
    editing = c;
    editHp = c.hp;
    editStatus = c.status;
    editRanged = c.ranged;
  }

  function saveEdit() {
    if (editing) repo.combat.update({ ...editing, hp: editHp, status: editStatus, ranged: editRanged });
    editing = null;
  }

  function deleteEdit() {
    if (editing) repo.combat.delete(editing);
    editing = null;
  }
</script>

<div class="page">
  <SectionTopBar title="战斗轮小助手" onback={() => history.back()} />
  <div class="page-body">
    {#if combatants.length === 0}
      <EmptyState text="还没有参战者，点击右下角添加" icon="shield" />
    {:else}
      <button class="btn filled next-btn" onclick={() => { turn++; }}>
        <Icon name="skip_next" size={20} />
        <span>下一个行动</span>
      </button>
      {#each combatants as c, i (c.id)}
        {@const isCurrent = i === turn % combatants.length}
        <button class="combat-row" class:current={isCurrent} onclick={() => openEdit(c)}>
          <div class="combat-info">
            <div class="combat-name">{c.name || '未命名'}</div>
            <div class="muted">DEX {c.dex}</div>
          </div>
          <div class="combat-hp">{c.hp}/{c.maxHp} HP</div>
          {#if c.ranged}<span class="chip ranged">远程</span>{/if}
          <span class="chip status {statusClass(c.status)}">{statusLabel(c.status)}</span>
        </button>
      {/each}
    {/if}
  </div>

  <button class="fab" onclick={() => (showAdd = true)} aria-label="添加参战者">
    <Icon name="add" size={24} />
  </button>
</div>

{#if showAdd}
  <Dialog title="添加参战者" onclose={() => (showAdd = false)}>
    {#snippet children()}
      <div class="col" style="gap:12px;">
        <TextField label="名称" bind:value={newName} autofocus />
        <div class="field">
          <label class="label">敏捷 DEX</label>
          <input class="text-input" inputmode="numeric" value={newDex} oninput={(e) => (newDex = digits(e.currentTarget.value, 3))} />
        </div>
        <div class="field">
          <label class="label">生命值 HP</label>
          <input class="text-input" inputmode="numeric" value={newHp} oninput={(e) => (newHp = digits(e.currentTarget.value, 4))} />
        </div>
        <div class="row">
          <span class="grow" style="color:var(--text);">携带远程武器</span>
          <Switch bind:checked={newRanged} />
        </div>
      </div>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (showAdd = false)}>取消</button>
      <button class="btn text" onclick={addCombatant} disabled={!newName.trim()}>添加</button>
    {/snippet}
  </Dialog>
{/if}

{#if editing}
  {@const c = editing}
  <Dialog title={c.name || '未命名'} onclose={() => (editing = null)}>
    {#snippet children()}
      <div class="col" style="gap:16px;">
        <div class="col" style="gap:4px;">
          <div class="muted">生命值</div>
          <div class="row">
            <button class="icon-btn" onclick={() => (editHp = Math.max(0, editHp - 1))} aria-label="-1"><Icon name="remove" size={24} /></button>
            <div class="hp-big">{editHp}/{c.maxHp}</div>
            <button class="icon-btn" onclick={() => (editHp = Math.min(c.maxHp, editHp + 1))} aria-label="+1"><Icon name="add" size={24} /></button>
          </div>
        </div>
        <div class="col" style="gap:8px;">
          <div class="muted">状态</div>
          <div class="row" style="gap:8px;">
            {#each [COMBATANT_ALIVE, COMBATANT_UNCONSCIOUS, COMBATANT_DEAD] as s}
              <button class="chip" class:selected={editStatus === s} onclick={() => (editStatus = s)}>{statusLabel(s)}</button>
            {/each}
          </div>
        </div>
        <div class="row">
          <span class="grow" style="color:var(--text);">携带远程武器</span>
          <Switch bind:checked={editRanged} />
        </div>
      </div>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" style="color:var(--danger);" onclick={deleteEdit}>删除</button>
      <button class="btn text" onclick={() => (editing = null)}>取消</button>
      <button class="btn text" onclick={saveEdit}>保存</button>
    {/snippet}
  </Dialog>
{/if}

<style>
  .next-btn {
    width: 100%;
    height: 56px;
    border-radius: 28px;
    margin-bottom: 16px;
  }
  .combat-row {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    min-height: 72px;
    padding: 0 16px;
    border: none;
    border-radius: 20px;
    cursor: pointer;
    text-align: left;
    background: var(--bg-container-low);
    margin-bottom: 8px;
  }
  .combat-row.current {
    background: var(--accent-container);
  }
  .combat-info {
    flex: 1 1 auto;
    min-width: 0;
  }
  .combat-name {
    font-size: 1rem;
    font-weight: 600;
    color: var(--text);
  }
  .combat-row.current .combat-name {
    color: var(--on-accent-container);
  }
  .combat-hp {
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--text);
    white-space: nowrap;
  }
  .chip.ranged {
    background: var(--md-secondary-container);
    color: var(--md-on-secondary-container);
    border-color: transparent;
  }
  .chip.status {
    border-color: transparent;
  }
  .chip.status.alive {
    color: var(--accent);
    background: color-mix(in srgb, var(--accent) 15%, transparent);
  }
  .chip.status.unconscious {
    color: var(--md-tertiary);
    background: color-mix(in srgb, var(--md-tertiary) 15%, transparent);
  }
  .chip.status.dead {
    color: var(--danger);
    background: color-mix(in srgb, var(--danger) 15%, transparent);
  }
  .chip.selected {
    background: var(--md-secondary-container);
    color: var(--md-on-secondary-container);
    border-color: transparent;
  }
  .hp-big {
    font-size: 1.5rem;
    font-weight: 600;
    min-width: 96px;
    text-align: center;
    color: var(--text);
  }
</style>
