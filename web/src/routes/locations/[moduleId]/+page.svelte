<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { newLocation } from '$lib/data/factories';
  import { R } from '$lib/nav/routes';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import GroupedListItem from '$lib/components/GroupedListItem.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const moduleId = Number(page.params.moduleId);
  const locations = $derived(repo.locations.forModule(moduleId));

  let showNew = $state(false);
  let name = $state('');

  function create() {
    if (!name.trim()) return;
    repo.locations.insert(newLocation(moduleId, name.trim()));
    showNew = false;
    name = '';
  }
</script>

<div class="page">
  <SectionTopBar title="重要地点" onback={() => history.back()} />
  <div class="page-body">
    {#if locations.length === 0}
      <EmptyState text="还没有地点，点击右下角新建" icon="location_on" />
    {:else}
      <div class="grouped">
        {#each locations as loc, i (loc.id)}
          <GroupedListItem
            index={i}
            count={locations.length}
            icon="location_on"
            title={loc.name || '未命名地点'}
            subtitle={loc.content || '暂无描述'}
            onclick={() => goto(R.location(loc.id))}
          >
            {#snippet trailing()}
              <Icon name="chevron_right" size={24} />
            {/snippet}
          </GroupedListItem>
        {/each}
      </div>
    {/if}
  </div>

  <button class="fab" onclick={() => (showNew = true)} aria-label="新建地点">
    <Icon name="add" size={24} />
  </button>
</div>

{#if showNew}
  <Dialog title="新建地点" onclose={() => (showNew = false)}>
    {#snippet children()}
      <TextField label="地点名称" bind:value={name} autofocus />
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (showNew = false)}>取消</button>
      <button class="btn text" onclick={create} disabled={!name.trim()}>创建</button>
    {/snippet}
  </Dialog>
{/if}
