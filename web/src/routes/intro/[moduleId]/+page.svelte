<script lang="ts">
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { applyMarkup } from '$lib/utils/markdown';
  import { autosize } from '$lib/ui/autosize';
  import Icon from '$lib/components/Icon.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import MarkdownText from '$lib/components/MarkdownText.svelte';
  import FloatingToolbar from '$lib/components/FloatingToolbar.svelte';

  const moduleId = Number(page.params.moduleId);
  const module = $derived(repo.modules.getById(moduleId));

  let editing = $state(false);
  let textField = $state('');
  let ta = $state<HTMLTextAreaElement | null>(null);

  function startEditing() {
    textField = module?.introText ?? '';
    editing = true;
  }

  function save() {
    const m = repo.modules.getById(moduleId);
    if (m) repo.modules.update({ ...m, introText: textField });
  }

  function wrap(marker: string) {
    const start = ta?.selectionStart ?? textField.length;
    const end = ta?.selectionEnd ?? textField.length;
    const r = applyMarkup(textField, start, end, marker);
    textField = r.text;
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
    { icon: 'format_underlined', description: '下划线', onClick: () => wrap('__') },
    { icon: 'attach_file', description: '附件', onClick: () => (textField += '\n[附件]') }
  ];
</script>

<div class="page">
  <SectionTopBar title="简介与招募" onback={() => history.back()} />
  <div class="page-body intro-body">
    {#if editing}
      <textarea
        class="text-input intro-editor"
        bind:this={ta}
        bind:value={textField}
        oninput={save}
        placeholder="输入简介与招募信息…"
        use:autosize={textField}
      ></textarea>
    {:else}
      <div class="intro-preview">
        {#if !module?.introText}
          <div class="muted">还没有内容，点击右下角编辑按钮开始书写</div>
        {:else}
          <MarkdownText text={module.introText} />
        {/if}
      </div>
    {/if}
  </div>

  <div class="intro-bottom">
    {#if editing}
      <FloatingToolbar actions={toolbarActions} />
    {/if}
    <div class="grow"></div>
    <button
      class="fab intro-fab"
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

<style>
  .intro-body {
    padding-bottom: 96px;
  }
  .intro-editor {
    width: 100%;
    min-height: 100%;
    resize: none;
    border: none;
    background: transparent;
    font-size: 0.95rem;
    line-height: 1.6;
  }
  .intro-preview {
    overflow-y: auto;
  }
  .intro-bottom {
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
  .intro-fab {
    position: relative;
    flex: 0 0 auto;
  }
</style>
