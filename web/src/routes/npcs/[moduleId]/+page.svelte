<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { newNpc } from '$lib/data/factories';
  import { R } from '$lib/nav/routes';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import GroupedListItem from '$lib/components/GroupedListItem.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const moduleId = Number(page.params.moduleId);
  const npcs = $derived(repo.npcs.forModule(moduleId));

  let showNew = $state(false);
  let name = $state('');

  function create() {
    if (!name.trim()) return;
    repo.npcs.insert(newNpc(moduleId, name.trim()));
    showNew = false;
    name = '';
  }
</script>

<div class="page">
  <SectionTopBar title="重要npc" onback={() => history.back()} />
  <div class="page-body">
    {#if npcs.length === 0}
      <EmptyState text="还没有npc，点击右下角新建" icon="groups" />
    {:else}
      <div class="grouped">
        {#each npcs as npc, i (npc.id)}
          <GroupedListItem
            index={i}
            count={npcs.length}
            icon="groups"
            title={npc.name || '未命名npc'}
            subtitle={npc.content || '暂无描述'}
            onclick={() => goto(R.npc(npc.id))}
          >
            {#snippet trailing()}
              <Icon name="chevron_right" size={24} />
            {/snippet}
          </GroupedListItem>
        {/each}
      </div>
    {/if}
  </div>

  <button class="fab" onclick={() => (showNew = true)} aria-label="新建npc">
    <Icon name="add" size={24} />
  </button>
</div>

{#if showNew}
  <Dialog title="新建npc" onclose={() => (showNew = false)}>
    {#snippet children()}
      <TextField label="npc名称" bind:value={name} autofocus />
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (showNew = false)}>取消</button>
      <button class="btn text" onclick={create} disabled={!name.trim()}>创建</button>
    {/snippet}
  </Dialog>
{/if}
