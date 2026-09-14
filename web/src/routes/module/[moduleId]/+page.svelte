<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { R } from '$lib/nav/routes';
  import { pickFile, compressImageDataUrl } from '$lib/data/fileStore';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import GroupedListItem from '$lib/components/GroupedListItem.svelte';
  import LoadedImage from '$lib/components/LoadedImage.svelte';
  import Switch from '$lib/components/Switch.svelte';
  import Dialog from '$lib/components/Dialog.svelte';

  const moduleId = Number(page.params.moduleId);
  const module = $derived(repo.modules.getById(moduleId));

  let confirmDelete = $state(false);

  const sections = [
    { icon: 'chat', title: '简介与招募', to: R.intro(moduleId) },
    { icon: 'receipt_long', title: '时间轴与大纲', to: R.timeline(moduleId) },
    { icon: 'location_on', title: '重要地点', to: R.locations(moduleId) },
    { icon: 'groups', title: '重要npc', to: R.npcs(moduleId) },
    { icon: 'person', title: 'pc档案', to: R.pcs(moduleId) }
  ];

  function onOngoing(checked: boolean) {
    if (checked) {
      repo.modules.clearActive();
      repo.modules.setActive(moduleId);
    } else {
      repo.modules.clearActive();
    }
  }

  async function setPhoto() {
    const picked = await pickFile(['image/*']);
    if (!picked) return;
    const compressed = await compressImageDataUrl(picked.dataUrl, 960, 0.82);
    const m = repo.modules.getById(moduleId);
    if (m) repo.modules.update({ ...m, photoUri: compressed });
  }
</script>

{#if !module}
  <EmptyState text="模组不存在或已被删除" />
{:else}
  <div class="page">
    <SectionTopBar
      title={module.name || '模组详情'}
      onback={() => history.back()}
      rightIcon="delete"
      onright={() => (confirmDelete = true)}
    />
    <div class="page-body">
      <div class="ongoing-row">
        <div class="grow ongoing-label">正在进行</div>
        <Switch bind:checked={module.isActive} onchange={onOngoing} />
      </div>

      <div class="banner">
        <LoadedImage uri={module.photoUri || null} radius={20} icon="image" />
        <div class="banner-scrim"></div>
        <div class="banner-title">{module.name || '模组名称'}</div>
        <button class="icon-btn banner-photo" onclick={setPhoto} aria-label="设置照片">
          <Icon name="add_a_photo" size={24} />
        </button>
        <button class="btn filled banner-original" onclick={() => goto(R.original(moduleId))}>
          <Icon name="menu_book" size={20} />
          <span>原文</span>
        </button>
      </div>

      <div class="grouped">
        {#each sections as s, i (s.title)}
          <GroupedListItem index={i} count={sections.length} icon={s.icon} title={s.title} onclick={() => goto(s.to)}>
            {#snippet trailing()}
              <Icon name="chevron_right" size={24} />
            {/snippet}
          </GroupedListItem>
        {/each}
      </div>
    </div>
  </div>

  {#if confirmDelete}
    <Dialog title="删除模组" onclose={() => (confirmDelete = false)}>
      {#snippet children()}
        <p>确定要删除“{module.name}”吗？模组下的时间轴、地点、npc、pc档案与文件都会被一并删除，此操作不可撤销。</p>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (confirmDelete = false)}>取消</button>
        <button
          class="btn text"
          style="color:var(--danger);"
          onclick={() => {
            repo.deleteModule(moduleId);
            confirmDelete = false;
            history.back();
          }}
        >删除</button>
      {/snippet}
    </Dialog>
  {/if}
{/if}

<style>
  .ongoing-row {
    display: flex;
    align-items: center;
    height: 56px;
    padding: 0 16px;
    background: var(--bg-container-low);
    border-radius: 28px;
    margin-bottom: 16px;
  }
  .ongoing-label {
    font-size: 0.95rem;
    color: var(--text);
  }
  .banner {
    position: relative;
    height: 220px;
    border-radius: 20px;
    overflow: hidden;
    margin-bottom: 16px;
  }
  .banner-scrim {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 120px;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.55), transparent);
  }
  .banner-title {
    position: absolute;
    top: 20px;
    left: 20px;
    font-size: 1.5rem;
    font-weight: 600;
    color: var(--text);
  }
  .banner-photo {
    position: absolute;
    top: 8px;
    right: 8px;
    color: var(--text);
  }
  .banner-original {
    position: absolute;
    right: 20px;
    bottom: 20px;
    border-radius: 28px;
  }
</style>
