<script lang="ts">
  import { goto } from '$app/navigation';
  import { repo } from '$lib/data/repository.svelte';
  import { newPc } from '$lib/data/factories';
  import { R } from '$lib/nav/routes';
  import { pcSummary, pcStatsSummary, skilledCount } from '$lib/ui/pcSummary';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import LoadedImage from '$lib/components/LoadedImage.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  let showNew = $state(false);
  let name = $state('');

  const pcs = $derived(repo.pcs.playerPcs());

  function create() {
    if (!name.trim()) return;
    repo.pcs.insert(newPc(name.trim()));
    showNew = false;
    name = '';
  }
</script>

<div class="page">
  <div class="page-body">
    {#if pcs.length === 0}
      <EmptyState text="还没有角色，点击右下角新建调查员" icon="person" />
    {:else}
      <div class="page-title">角色</div>
      {#each pcs as pc (pc.id)}
        <button class="pc-card" onclick={() => goto(R.pc(pc.id, true))}>
          <div class="thumb"><LoadedImage uri={pc.imageUri || null} radius={0} icon="person" /></div>
          <div class="body">
            <div class="t">{pc.name || '人物名称'}</div>
            <div class="s">{pcSummary(pc)}</div>
            <div class="stats-row">
              <div class="stats ellipsis">{pcStatsSummary(pc)}</div>
              <span class="skill-badge">{skilledCount(pc)} 技能</span>
            </div>
          </div>
        </button>
      {/each}
    {/if}
  </div>

  <button class="fab" onclick={() => (showNew = true)} aria-label="新建角色">
    <Icon name="edit" size={24} />
  </button>

  {#if showNew}
    <Dialog title="新建角色" onclose={() => (showNew = false)}>
      {#snippet children()}
        <TextField label="人物名称" bind:value={name} autofocus />
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (showNew = false)}>取消</button>
        <button class="btn text" onclick={create} disabled={!name.trim()}>创建</button>
      {/snippet}
    </Dialog>
  {/if}
</div>
