<script lang="ts">
  import type { TimelineNodeEntity } from '$lib/data/types';
  import { repo } from '$lib/data/repository.svelte';
  import Icon from './Icon.svelte';
  import TimelineList from './TimelineList.svelte';

  const MAX_DEPTH = 3;

  let { allNodes, parentId, depth, onEdit, onAddChild }: {
    allNodes: TimelineNodeEntity[];
    parentId: number | null;
    depth: number;
    onEdit: (n: TimelineNodeEntity) => void;
    onAddChild: (n: TimelineNodeEntity) => void;
  } = $props();

  const siblings = $derived(
    allNodes.filter((n) => n.parentId === parentId).sort((a, b) => a.order - b.order)
  );

  let expanded = $state<Set<number>>(new Set());
  let draggingId = $state<number | null>(null);
  let draggingIndex = -1;
  let dragStartY = 0;
  let dragOffset = $state(0);

  function toggle(id: number) {
    const next = new Set(expanded);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    expanded = next;
  }

  function deleteSubtree(node: TimelineNodeEntity) {
    const toDelete = new Set<number>([node.id]);
    let changed = true;
    while (changed) {
      changed = false;
      for (const n of allNodes) {
        if (n.parentId !== null && toDelete.has(n.parentId) && !toDelete.has(n.id)) {
          toDelete.add(n.id);
          changed = true;
        }
      }
    }
    for (const id of toDelete) {
      const n = allNodes.find((x) => x.id === id);
      if (n) repo.timeline.delete(n);
    }
  }

  function reorder(id: number, from: number, to: number) {
    if (from === to || from < 0 || to < 0 || from >= siblings.length || to >= siblings.length) return;
    const arr = [...siblings];
    const [moved] = arr.splice(from, 1);
    arr.splice(to, 0, moved);
    arr.forEach((n, idx) => {
      if (n.order !== idx) repo.timeline.update({ ...n, order: idx });
    });
  }

  function depthBg(d: number): string {
    const m = d % 3;
    return m === 0 ? 'var(--bg-container-high)' : m === 1 ? 'var(--bg-container)' : 'var(--bg-container-low)';
  }
  function depthAccent(d: number): string {
    const m = d % 3;
    return m === 0 ? 'var(--accent)' : m === 1 ? 'var(--md-tertiary)' : 'var(--md-secondary)';
  }

  function startDrag(id: number, index: number) {
    return (e: PointerEvent) => {
      draggingId = id;
      draggingIndex = index;
      dragStartY = e.clientY;
      dragOffset = 0;
      (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    };
  }
  function moveDrag(e: PointerEvent) {
    if (draggingId === null) return;
    dragOffset = e.clientY - dragStartY;
  }
  function endDrag() {
    if (draggingId === null) return;
    const moved = Math.round(dragOffset / 56);
    if (moved !== 0) {
      const to = Math.max(0, Math.min(siblings.length - 1, draggingIndex + moved));
      reorder(draggingId, draggingIndex, to);
    }
    draggingId = null;
    dragOffset = 0;
  }
</script>

<div class="timeline-list">
  {#each siblings as node, index (node.id)}
    {@const hasChildren = allNodes.some((n) => n.parentId === node.id)}
    {@const isExpanded = expanded.has(node.id)}
    {@const dragging = draggingId === node.id}
    <div class="tl-node">
      <div
        class="tl-row"
        style:background={dragging ? 'var(--accent-container)' : depthBg(depth)}
        style:transform={dragging ? `translateY(${dragOffset}px)` : 'none'}
        style:z-index={dragging ? '5' : 'auto'}
      >
        <span class="tl-accent" style:background={depthAccent(depth)}></span>
        <span
          class="tl-drag"
          role="button"
          tabindex="0"
          aria-label="拖拽排序"
          onpointerdown={startDrag(node.id, index)}
          onpointermove={moveDrag}
          onpointerup={endDrag}
          onpointercancel={endDrag}
        >
          <Icon name="drag_handle" size={24} />
        </span>
        {#if hasChildren}
          <button class="icon-btn" onclick={() => toggle(node.id)} aria-label={isExpanded ? '收起' : '展开'}>
            <Icon name={isExpanded ? 'expand_less' : 'expand_more'} size={24} />
          </button>
        {:else}
          <span class="tl-leaf"><Icon name="arrow_forward" size={20} /></span>
        {/if}
        <button class="tl-title" onclick={() => toggle(node.id)}>{node.title || '未命名'}</button>
        <button class="icon-btn accent" onclick={() => onEdit(node)} aria-label="编辑"><Icon name="edit" size={24} /></button>
        {#if depth < MAX_DEPTH - 1}
          <button class="icon-btn accent" onclick={() => onAddChild(node)} aria-label="添加子事件"><Icon name="add" size={24} /></button>
        {/if}
        <button class="icon-btn danger" onclick={() => deleteSubtree(node)} aria-label="删除"><Icon name="delete" size={24} /></button>
      </div>
      {#if isExpanded && node.content.trim()}
        <div class="tl-content" style:padding-left="{16 + depth * 20}px">{node.content}</div>
      {/if}
      {#if isExpanded && hasChildren}
        <div class="tl-children">
          <TimelineList {allNodes} parentId={node.id} depth={depth + 1} {onEdit} {onAddChild} />
        </div>
      {/if}
    </div>
  {/each}
</div>

<style>
  .timeline-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .tl-node {
    display: flex;
    flex-direction: column;
  }
  .tl-row {
    display: flex;
    align-items: center;
    height: 56px;
    border-radius: 12px;
    overflow: hidden;
    position: relative;
  }
  .tl-accent {
    width: 4px;
    align-self: stretch;
    flex: 0 0 auto;
  }
  .tl-drag {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 100%;
    flex: 0 0 auto;
    color: var(--text-muted);
    cursor: grab;
    touch-action: none;
    user-select: none;
  }
  .tl-leaf {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 100%;
    flex: 0 0 auto;
    color: var(--text-muted);
  }
  .tl-title {
    flex: 1 1 auto;
    min-width: 0;
    height: 100%;
    background: transparent;
    border: none;
    text-align: left;
    font-size: 0.95rem;
    color: var(--text);
    padding: 0 8px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .tl-content {
    padding: 0 16px 14px;
    font-size: 0.88rem;
    color: var(--text-muted);
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  }
  .tl-children {
    padding-left: 20px;
  }
  .icon-btn.danger {
    color: var(--danger);
  }
</style>
