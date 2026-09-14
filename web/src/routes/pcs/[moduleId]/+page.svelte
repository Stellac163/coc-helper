<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { newPc } from '$lib/data/factories';
  import { R } from '$lib/nav/routes';
  import { pcSummary, pcStatsSummary, skilledCount } from '$lib/ui/pcSummary';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const moduleId = Number(page.params.moduleId);
  const pcs = $derived(repo.pcs.forModule(moduleId));

  let showNew = $state(false);
  let name = $state('');

  function create() {
    if (!name.trim()) return;
    repo.pcs.insert(newPc(name.trim(), moduleId));
    showNew = false;
    name = '';
  }
</script>

<div class="page">
  <SectionTopBar title="pc档案" onback={() => history.back()} />
  <div class="page-body">
    {#if pcs.length === 0}
      <EmptyState text="还没有pc，点击右下角新建调查员" icon="person" />
    {:else}
      {#each pcs as pc (pc.id)}
        <button class="pc-list-card" onclick={() => goto(R.pc(pc.id, false))}>
          <div class="t">{pc.name || '人物名称'}</div>
          <div class="s">{pcSummary(pc)}</div>
          <div class="stats-row">
            <div class="stats ellipsis">{pcStatsSummary(pc)}</div>
            <span class="skill-badge">{skilledCount(pc)} 技能</span>
          </div>
        </button>
      {/each}
    {/if}
  </div>

  <button class="fab" onclick={() => (showNew = true)} aria-label="新建pc">
    <Icon name="add" size={24} />
  </button>
</div>

{#if showNew}
  <Dialog title="新建pc" onclose={() => (showNew = false)}>
    {#snippet children()}
      <TextField label="人物名称" bind:value={name} autofocus />
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (showNew = false)}>取消</button>
      <button class="btn text" onclick={create} disabled={!name.trim()}>创建</button>
    {/snippet}
  </Dialog>
{/if}

<style>
  .pc-list-card {
    display: flex;
    flex-direction: column;
    gap: 4px;
    width: 100%;
    border: none;
    border-radius: 20px;
    background: var(--bg-container-highest);
    padding: 16px;
    cursor: pointer;
    text-align: left;
    margin-bottom: 16px;
  }
  .pc-list-card .t {
    font-size: 1.05rem;
    font-weight: 600;
    color: var(--text);
  }
  .pc-list-card .s {
    font-size: 0.88rem;
    color: var(--text-muted);
  }
  .pc-list-card .stats-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 4px;
  }
  .pc-list-card .stats {
    font-size: 0.8rem;
    color: var(--text-muted);
    flex: 1 1 auto;
    min-width: 0;
  }
</style>
