<script lang="ts">
  import { goto } from '$app/navigation';
  import { repo } from '$lib/data/repository.svelte';
  import { newModule } from '$lib/data/factories';
  import { R } from '$lib/nav/routes';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import GroupedListItem from '$lib/components/GroupedListItem.svelte';
  import LoadedImage from '$lib/components/LoadedImage.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  let showNew = $state(false);
  let name = $state('');
  let desc = $state('');

  const modules = $derived(repo.modules.all());
  const active = $derived(modules.find((m) => m.isActive));

  function create() {
    if (!name.trim()) return;
    repo.modules.insert(newModule(name.trim(), desc.trim()));
    showNew = false;
    name = '';
    desc = '';
  }
</script>

<div class="page">
  <div class="page-body">
    {#if modules.length === 0}
      <EmptyState text="还没有模组，点击右下角按钮新建一个模组" icon="image" />
    {:else}
      <div class="page-title">模组</div>
      {#if active}
        <button class="active-card" onclick={() => goto(R.module(active.id))}>
          <div class="cover"><LoadedImage uri={active.photoUri || null} radius={0} icon="image" /></div>
          <div class="body">
            <div class="t">{active.name || '模组名称'}</div>
            <div class="s">{active.description || '模组简介'}</div>
          </div>
        </button>
      {/if}
      <div class="grouped">
        {#each modules as m, i (m.id)}
          <GroupedListItem
            index={i}
            count={modules.length}
            icon="home"
            title={m.name || '未命名模组'}
            subtitle={m.description || '模组简介'}
            onclick={() => goto(R.module(m.id))}
          >
            {#snippet trailing()}
              <Icon name="chevron_right" size={24} />
            {/snippet}
          </GroupedListItem>
        {/each}
      </div>
    {/if}
  </div>

  <button class="fab" onclick={() => (showNew = true)} aria-label="新建模组">
    <Icon name="edit" size={24} />
  </button>

  {#if showNew}
    <Dialog title="新建模组" onclose={() => (showNew = false)}>
      {#snippet children()}
        <div class="col" style="gap:12px;">
          <TextField label="模组名称" bind:value={name} />
          <TextField label="模组简介" bind:value={desc} single={false} minLines={2} />
        </div>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (showNew = false)}>取消</button>
        <button class="btn text" onclick={create} disabled={!name.trim()}>创建</button>
      {/snippet}
    </Dialog>
  {/if}
</div>
