<script lang="ts">
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { newTimeline } from '$lib/data/factories';
  import type { TimelineNodeEntity } from '$lib/data/types';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import TimelineList from '$lib/components/TimelineList.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const moduleId = Number(page.params.moduleId);
  const nodes = $derived(repo.timeline.getAll(moduleId));

  let editing = $state<TimelineNodeEntity | null>(null);
  let editTitle = $state('');
  let editContent = $state('');

  let addingUnder = $state<number | null>(null);
  let showAdd = $state(false);
  let newTitle = $state('');

  function openEdit(node: TimelineNodeEntity) {
    editing = node;
    editTitle = node.title;
    editContent = node.content;
  }

  function saveEdit() {
    if (editing) {
      repo.timeline.update({ ...editing, title: editTitle.trim(), content: editContent });
      editing = null;
    }
  }

  function create() {
    if (newTitle.trim()) {
      const siblings = nodes.filter((n) => n.parentId === addingUnder);
      repo.timeline.insert(newTimeline(moduleId, newTitle.trim(), addingUnder, siblings.length));
    }
    showAdd = false;
    addingUnder = null;
    newTitle = '';
  }
</script>

<div class="page">
  <SectionTopBar title="时间轴与大纲" onback={() => history.back()} />
  <div class="page-body">
    {#if nodes.length === 0}
      <EmptyState text="还没有时间轴节点，点击右下角新建" icon="receipt_long" />
    {:else}
      <TimelineList
        allNodes={nodes}
        parentId={null}
        depth={0}
        onEdit={openEdit}
        onAddChild={(n) => (addingUnder = n.id)}
      />
    {/if}
  </div>

  <button class="fab" onclick={() => (showAdd = true)} aria-label="新建时间轴">
    <Icon name="add" size={24} />
  </button>
</div>

{#if editing}
  <Dialog title="编辑节点" onclose={() => (editing = null)}>
    {#snippet children()}
      <div class="col" style="gap:12px;">
        <TextField label="标题" bind:value={editTitle} />
        <TextField label="内容" bind:value={editContent} single={false} minLines={3} />
      </div>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (editing = null)}>取消</button>
      <button class="btn text" onclick={saveEdit}>保存</button>
    {/snippet}
  </Dialog>
{/if}

{#if showAdd || addingUnder !== null}
  <Dialog title={addingUnder === null ? '新建事件' : '新建子事件'} onclose={() => { showAdd = false; addingUnder = null; }}>
    {#snippet children()}
      <TextField label="标题" bind:value={newTitle} autofocus />
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => { showAdd = false; addingUnder = null; }}>取消</button>
      <button class="btn text" onclick={create} disabled={!newTitle.trim()}>创建</button>
    {/snippet}
  </Dialog>
{/if}
