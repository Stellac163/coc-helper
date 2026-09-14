<script lang="ts">
  import { repo } from '$lib/data/repository.svelte';
  import { newChasePoint, newChaseParticipant } from '$lib/data/factories';
  import {
    CHASE_QUARRY,
    CHASE_PURSUER,
    type ChasePointEntity,
    type ChaseParticipantEntity
  } from '$lib/data/types';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const participants = $derived(repo.chase.all());
  const points = $derived(repo.chasePoints.all());

  let showAddParticipant = $state(false);
  let showAddPoint = $state(false);
  let pName = $state('');
  let pMov = $state('8');
  let pRole = $state(CHASE_QUARRY);
  let ptName = $state('');

  const digits = (v: string, max: number) => v.replace(/\D/g, '').slice(0, max);

  const minMov = $derived(participants.length ? Math.min(...participants.map((p) => p.mov)) : 0);

  function roleLabel(role: number): string {
    return role === CHASE_PURSUER ? '追捕者' : '逃亡者';
  }
  function actionPoints(mov: number, min: number): number {
    return 1 + (mov - min);
  }
  function pointName(pointId: number): string {
    return points.find((pt) => pt.id === pointId)?.name || '—';
  }
  function moveParticipant(p: ChaseParticipantEntity, delta: number): ChaseParticipantEntity {
    if (points.length === 0) return p;
    const idx = points.findIndex((pt) => pt.id === p.pointId);
    const newIdx =
      idx < 0 ? (delta > 0 ? 0 : points.length - 1) : Math.min(points.length - 1, Math.max(0, idx + delta));
    return { ...p, pointId: points[newIdx].id };
  }

  function addPoint() {
    if (!ptName.trim()) return;
    const order = points.reduce((m, pt) => Math.max(m, pt.order), -1) + 1;
    repo.chasePoints.insert(newChasePoint(ptName.trim(), order));
    showAddPoint = false;
    ptName = '';
  }

  function addParticipant() {
    if (!pName.trim()) return;
    const mov = parseInt(pMov, 10) || 8;
    repo.chase.insert(newChaseParticipant(pName.trim(), mov, pRole));
    showAddParticipant = false;
    pName = '';
    pMov = '8';
    pRole = CHASE_QUARRY;
  }

  function movePoint(index: number, delta: number) {
    const target = index + delta;
    if (index < 0 || index >= points.length || target < 0 || target >= points.length) return;
    const a = points[index];
    const b = points[target];
    repo.chasePoints.update({ ...a, order: b.order });
    repo.chasePoints.update({ ...b, order: a.order });
  }

  function deletePoint(point: ChasePointEntity) {
    repo.chasePoints.delete(point);
    for (const p of participants) {
      if (p.pointId === point.id) repo.chase.update({ ...p, pointId: 0 });
    }
  }

  function allForward() {
    for (const p of participants) {
      const moved = moveParticipant(p, +1);
      if (moved.pointId !== p.pointId) repo.chase.update(moved);
    }
  }

  function moveOne(p: ChaseParticipantEntity, delta: number) {
    repo.chase.update(moveParticipant(p, delta));
  }
</script>

<div class="page">
  <SectionTopBar title="追逐战小助手" onback={() => history.back()} />
  <div class="page-body">
    <div class="row between chase-section">
      <div class="section-head">追逐点位</div>
      <button class="btn outlined" onclick={() => (showAddPoint = true)}>
        <Icon name="place" size={18} />
        <span>添加点位</span>
      </button>
    </div>

    {#if points.length === 0}
      <div class="point-empty">还没有点位。点「添加点位」设定追逐路线上的地点（如 巷口 / 仓库 / 河边）。</div>
    {:else}
      {#each points as point, i (point.id)}
        {@const count = participants.filter((p) => p.pointId === point.id).length}
        <div class="point-row">
          <Icon name="place" size={24} color="var(--accent)" />
          <div class="grow">
            <div class="point-name">{point.name || '未命名点位'}</div>
            <div class="muted small">{count === 0 ? '无人' : `${count} 人`}</div>
          </div>
          <button class="icon-btn" disabled={i === 0} onclick={() => movePoint(i, -1)} aria-label="上移"><Icon name="arrow_upward" size={24} /></button>
          <button class="icon-btn" disabled={i === points.length - 1} onclick={() => movePoint(i, 1)} aria-label="下移"><Icon name="arrow_downward" size={24} /></button>
          <button class="icon-btn danger" onclick={() => deletePoint(point)} aria-label="删除点位"><Icon name="delete" size={24} /></button>
        </div>
      {/each}
    {/if}

    {#if participants.length === 0}
      <EmptyState text="还没有参与者，点击右下角添加" icon="directions_run" />
    {:else}
      <div class="row between chase-section">
        <div class="section-head">参与者</div>
        <div class="muted small">最低 MOV {minMov} · 行动点 = 1 + (MOV − {minMov})</div>
      </div>

      <button class="btn filled forward-btn" onclick={allForward}>
        <Icon name="directions_run" size={20} />
        <span>全体前进一个点位</span>
      </button>

      {#each participants as p (p.id)}
        <div class="chase-row">
          <div class="row">
            <div class="grow">
              <div class="chase-name">{p.name || '未命名'}</div>
              <div class="muted small">{roleLabel(p.role)} · MOV {p.mov} · 位置 {pointName(p.pointId)}</div>
            </div>
            <span class="chip accent">行动点 ×{actionPoints(p.mov, minMov)}</span>
          </div>
          <div class="row">
            <button class="icon-btn" onclick={() => moveOne(p, -1)} aria-label="后退"><Icon name="keyboard_arrow_left" size={24} /></button>
            <div class="grow chase-pos">{pointName(p.pointId)}</div>
            <button class="icon-btn" style="color:var(--accent);" onclick={() => moveOne(p, 1)} aria-label="前进"><Icon name="keyboard_arrow_right" size={24} /></button>
            <button class="icon-btn danger" onclick={() => repo.chase.delete(p)} aria-label="删除"><Icon name="delete" size={24} /></button>
          </div>
        </div>
      {/each}
    {/if}
  </div>

  <button class="fab" onclick={() => (showAddParticipant = true)} aria-label="添加参与者">
    <Icon name="add" size={24} />
  </button>
</div>

{#if showAddPoint}
  <Dialog title="添加点位" onclose={() => (showAddPoint = false)}>
    {#snippet children()}
      <TextField label="地点名称" bind:value={ptName} autofocus />
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (showAddPoint = false)}>取消</button>
      <button class="btn text" onclick={addPoint} disabled={!ptName.trim()}>添加</button>
    {/snippet}
  </Dialog>
{/if}

{#if showAddParticipant}
  <Dialog title="添加参与者" onclose={() => (showAddParticipant = false)}>
    {#snippet children()}
      <div class="col" style="gap:12px;">
        <TextField label="名称" bind:value={pName} autofocus />
        <div class="field">
          <label class="label">移动力 MOV</label>
          <input class="text-input" inputmode="numeric" value={pMov} oninput={(e) => (pMov = digits(e.currentTarget.value, 3))} />
        </div>
        <div class="row" style="gap:8px;">
          {#each [CHASE_QUARRY, CHASE_PURSUER] as r}
            <button class="chip" class:selected={pRole === r} onclick={() => (pRole = r)}>{r === CHASE_PURSUER ? '追捕者' : '逃亡者'}</button>
          {/each}
        </div>
      </div>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (showAddParticipant = false)}>取消</button>
      <button class="btn text" onclick={addParticipant} disabled={!pName.trim()}>添加</button>
    {/snippet}
  </Dialog>
{/if}

<style>
  .chase-section {
    margin: 16px 0 12px;
  }
  .section-head {
    font-size: 1rem;
    font-weight: 600;
    color: var(--text);
  }
  .point-empty {
    background: var(--bg-container-low);
    border-radius: 16px;
    padding: 16px;
    color: var(--text-muted);
    font-size: 0.9rem;
    margin-bottom: 12px;
  }
  .point-row {
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--bg-container-low);
    border-radius: 16px;
    padding: 8px 12px;
    margin-bottom: 8px;
  }
  .point-name {
    font-size: 1rem;
    color: var(--text);
  }
  .forward-btn {
    width: 100%;
    height: 48px;
    border-radius: 24px;
    margin-bottom: 12px;
  }
  .chase-row {
    background: var(--bg-container-low);
    border-radius: 20px;
    padding: 16px;
    margin-bottom: 8px;
  }
  .chase-name {
    font-size: 1rem;
    font-weight: 600;
    color: var(--text);
  }
  .chase-pos {
    text-align: center;
    font-size: 1rem;
    font-weight: 600;
    color: var(--text);
  }
  .chip.selected {
    background: var(--md-secondary-container);
    color: var(--md-on-secondary-container);
    border-color: transparent;
  }
  .icon-btn.danger {
    color: var(--danger);
  }
  .icon-btn:disabled {
    color: var(--border);
    opacity: 0.6;
    cursor: default;
  }
</style>
