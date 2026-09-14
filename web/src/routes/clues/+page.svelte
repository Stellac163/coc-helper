<script lang="ts">
  import { repo } from '$lib/data/repository.svelte';
  import { newClue } from '$lib/data/factories';
  import type { ClueEntity } from '$lib/data/types';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import TextField from '$lib/components/TextField.svelte';

  const clues = $derived(repo.clues.all());

  let showAdd = $state(false);
  let editing = $state<ClueEntity | null>(null);
  let deleting = $state<ClueEntity | null>(null);
  let title = $state('');
  let content = $state('');

  function openAdd() {
    editing = null;
    title = '';
    content = '';
    showAdd = true;
  }

  function openEdit(clue: ClueEntity) {
    editing = clue;
    title = clue.title;
    content = clue.content;
    showAdd = true;
  }

  function save() {
    if (!title.trim()) return;
    if (editing) {
      repo.clues.update({ ...editing, title: title.trim(), content });
    } else {
      repo.clues.insert(newClue(title.trim(), content, clues.length));
    }
    showAdd = false;
    editing = null;
  }

  function moveClue(index: number, delta: number) {
    const target = index + delta;
    if (index < 0 || index >= clues.length || target < 0 || target >= clues.length) return;
    const sorted = clues.slice();
    const [moved] = sorted.splice(index, 1);
    sorted.splice(target, 0, moved);
    sorted.forEach((c, idx) => {
      if (c.order !== idx) repo.clues.update({ ...c, order: idx });
    });
  }
</script>

<div class="page">
  <SectionTopBar title="线索板" onback={() => history.back()} />
  <div class="page-body">
    {#if clues.length === 0}
      <EmptyState text="还没有线索，点击右下角添加" icon="lightbulb" />
    {:else}
      {#each clues as clue, i (clue.id)}
        <div class="clue-card">
          <button class="clue-main" onclick={() => openEdit(clue)}>
            <div class="clue-title">{clue.title || '未命名线索'}</div>
            {#if clue.content}<div class="clue-content">{clue.content}</div>{/if}
          </button>
          <div class="col">
            <button class="icon-btn" disabled={i === 0} onclick={() => moveClue(i, -1)} aria-label="上移">
              <Icon name="keyboard_arrow_up" size={24} />
            </button>
            <button class="icon-btn" disabled={i === clues.length - 1} onclick={() => moveClue(i, 1)} aria-label="下移">
              <Icon name="keyboard_arrow_down" size={24} />
            </button>
          </div>
          <button class="icon-btn danger" onclick={() => (deleting = clue)} aria-label="删除">
            <Icon name="delete" size={24} />
          </button>
        </div>
      {/each}
    {/if}
  </div>

  <button class="fab" onclick={openAdd} aria-label="新建线索">
    <Icon name="add" size={24} />
  </button>
</div>

{#if showAdd}
  <Dialog title={editing ? '编辑线索' : '新建线索'} onclose={() => { showAdd = false; editing = null; }}>
    {#snippet children()}
      <div class="col" style="gap:12px;">
        <TextField label="标题" bind:value={title} />
        <TextField label="内容" bind:value={content} single={false} minLines={3} />
      </div>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => { showAdd = false; editing = null; }}>取消</button>
      <button class="btn text" onclick={save} disabled={!title.trim()}>保存</button>
    {/snippet}
  </Dialog>
{/if}

{#if deleting}
  {@const d = deleting}
  <Dialog title="删除线索" onclose={() => (deleting = null)}>
    {#snippet children()}
      <p>确定要删除“{d.title}”吗？此操作不可撤销。</p>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (deleting = null)}>取消</button>
      <button
        class="btn text"
        style="color:var(--danger);"
        onclick={() => { repo.clues.delete(d); deleting = null; }}
      >删除</button>
    {/snippet}
  </Dialog>
{/if}

<style>
  .clue-card {
    display: flex;
    align-items: center;
    gap: 4px;
    background: var(--bg-container-highest);
    border-radius: 20px;
    padding: 12px 4px 12px 0;
    margin-bottom: 12px;
  }
  .clue-main {
    flex: 1 1 auto;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    border: none;
    background: transparent;
    text-align: left;
    padding: 0 0 0 16px;
    cursor: pointer;
  }
  .clue-title {
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--text);
  }
  .clue-content {
    font-size: 0.82rem;
    color: var(--text-muted);
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }
  .clue-card .icon-btn:disabled {
    color: var(--border);
    cursor: default;
  }
  .clue-card .icon-btn.danger {
    color: var(--danger);
  }
</style>
