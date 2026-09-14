<script lang="ts">
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { applyMarkup } from '$lib/utils/markdown';
  import { autosize } from '$lib/ui/autosize';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import MarkdownText from '$lib/components/MarkdownText.svelte';
  import FloatingToolbar from '$lib/components/FloatingToolbar.svelte';
  import TextField from '$lib/components/TextField.svelte';
  import Dialog from '$lib/components/Dialog.svelte';

  const npcId = Number(page.params.npcId);
  const npc = $derived(repo.npcs.getById(npcId));

  let editing = $state(false);
  let name = $state('');
  let content = $state('');
  let confirmDelete = $state(false);
  let ta = $state<HTMLTextAreaElement | null>(null);

  function startEditing() {
    if (!npc) return;
    name = npc.name;
    content = npc.content;
    editing = true;
  }

  function save() {
    if (!npc) return;
    repo.npcs.update({ ...npc, name: name.trim(), content });
  }

  function wrap(marker: string) {
    const start = ta?.selectionStart ?? content.length;
    const end = ta?.selectionEnd ?? content.length;
    const r = applyMarkup(content, start, end, marker);
    content = r.text;
    save();
    requestAnimationFrame(() => {
      if (ta) {
        ta.focus();
        ta.setSelectionRange(r.selStart, r.selEnd);
      }
    });
  }

  const toolbarActions = [
    { icon: 'format_bold', description: '加粗', onClick: () => wrap('**') },
    { icon: 'format_italic', description: '斜体', onClick: () => wrap('*') },
    { icon: 'format_underlined', description: '下划线', onClick: () => wrap('__') }
  ];
</script>

{#if !npc}
  <EmptyState text="npc不存在或已被删除" />
{:else}
  <div class="page">
    <SectionTopBar
      title={editing ? '编辑npc' : 'npc详情'}
      onback={() => history.back()}
      rightIcon={editing ? undefined : 'delete'}
      onright={() => (confirmDelete = true)}
    />
    <div class="page-body detail-body">
      {#if editing}
        <div class="col" style="gap:12px; height:100%;">
          <TextField label="npc名称" bind:value={name} />
          <textarea
            class="text-input detail-editor"
            bind:this={ta}
            bind:value={content}
            oninput={save}
            placeholder="npc描述…"
            use:autosize={content}
          ></textarea>
        </div>
      {:else}
        <div class="detail-view">
          <div class="detail-name">{npc.name || '未命名npc'}</div>
          {#if !npc.content}
            <div class="muted">还没有描述，点击右下角编辑</div>
          {:else}
            <MarkdownText text={npc.content} />
          {/if}
        </div>
      {/if}
    </div>

    <div class="detail-bottom">
      {#if editing}
        <FloatingToolbar actions={toolbarActions} />
      {/if}
      <div class="grow"></div>
      <button
        class="fab detail-fab"
        onclick={() => {
          if (editing) {
            save();
            editing = false;
          } else {
            startEditing();
          }
        }}
        aria-label="编辑"
      >
        <Icon name="edit" size={24} />
      </button>
    </div>
  </div>

  {#if confirmDelete}
    <Dialog title="删除npc" onclose={() => (confirmDelete = false)}>
      {#snippet children()}
        <p>确定要删除“{npc.name}”吗？此操作不可撤销。</p>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (confirmDelete = false)}>取消</button>
        <button
          class="btn text"
          style="color:var(--danger);"
          onclick={() => {
            repo.npcs.delete(npc);
            confirmDelete = false;
            history.back();
          }}
        >删除</button>
      {/snippet}
    </Dialog>
  {/if}
{/if}

<style>
  .detail-body {
    display: flex;
    flex-direction: column;
    padding-bottom: 96px;
  }
  .detail-editor {
    min-height: 200px;
    resize: none;
    font-size: 0.95rem;
    line-height: 1.6;
  }
  .detail-view {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .detail-name {
    font-size: 1.5rem;
    font-weight: 600;
    color: var(--text);
  }
  .detail-bottom {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    padding: 16px;
    gap: 8px;
    z-index: 35;
    animation: overlay-fade 160ms ease var(--nav-duration, 340ms) backwards;
  }
  .detail-fab {
    position: relative;
    flex: 0 0 auto;
  }
</style>
