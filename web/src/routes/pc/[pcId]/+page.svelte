<script lang="ts">
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import type { PcEntity } from '$lib/data/types';
  import { pickFile, compressImageDataUrl } from '$lib/data/fileStore';
  import { parseSt, type StResult } from '$lib/domain/stImport';
  import { snackbar } from '$lib/ui/snackbar.svelte';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import LoadedImage from '$lib/components/LoadedImage.svelte';
  import TextField from '$lib/components/TextField.svelte';
  import Dialog from '$lib/components/Dialog.svelte';
  import FabMenu from '$lib/components/FabMenu.svelte';

  const pcId = Number(page.params.pcId);
  const showAvatar = page.url.searchParams.get('avatar') === 'true';

  function copyPc(p: PcEntity): PcEntity {
    return {
      ...p,
      skills: p.skills.map((s) => ({ ...s })),
      attributes: p.attributes.map((a) => ({ ...a }))
    };
  }

  let draft = $state<PcEntity | null>(null);
  const initial = repo.pcs.getById(pcId);
  if (draft === null && initial) draft = copyPc(initial);

  $effect(() => {
    const p = repo.pcs.getById(pcId);
    if (p && (draft === null || draft.id !== p.id)) draft = copyPc(p);
  });

  let selectedTab = $state(0);
  let fabExpanded = $state(false);
  let confirmDelete = $state(false);
  let importOpen = $state(false);
  let editingSkill = $state<number | null>(null);
  let editingAttr = $state<number | null>(null);
  let skName = $state('');
  let skValue = $state('');
  let attrValue = $state('');
  let rawSt = $state('');

  const digits = (v: string, max: number) => v.replace(/\D/g, '').slice(0, max);

  const stResult = $derived.by<StResult | null>(() => {
    if (!rawSt.trim()) return null;
    try {
      const r = parseSt(rawSt);
      return Object.keys(r.attributes).length === 0 && r.skills.length === 0 ? null : r;
    } catch {
      return null;
    }
  });

  function save() {
    if (draft) {
      repo.pcs.update(draft);
      snackbar.show('已保存');
    }
  }

  async function pickAvatar() {
    const picked = await pickFile(['image/*']);
    if (!picked || !draft) return;
    const compressed = await compressImageDataUrl(picked.dataUrl, 512, 0.82);
    draft = { ...draft, imageUri: compressed };
  }

  function openSkillEdit(index: number, name: string, value: number) {
    editingSkill = index;
    skName = name;
    skValue = value ? String(value) : '';
  }

  function openAttrEdit(index: number, value: number) {
    editingAttr = index;
    attrValue = value ? String(value) : '';
  }

  function saveSkill() {
    if (draft && editingSkill !== null) {
      draft = {
        ...draft,
        skills: draft.skills.map((s, i) =>
          i === editingSkill ? { ...s, name: skName.trim(), value: parseInt(skValue, 10) || 0 } : s
        )
      };
    }
    editingSkill = null;
  }

  function deleteSkill() {
    if (draft && editingSkill !== null) {
      draft = { ...draft, skills: draft.skills.filter((_, i) => i !== editingSkill) };
    }
    editingSkill = null;
  }

  function addSkill() {
    if (draft) draft = { ...draft, skills: [...draft.skills, { name: '', value: 0 }] };
  }

  function saveAttr() {
    if (draft && editingAttr !== null) {
      draft = {
        ...draft,
        attributes: draft.attributes.map((a, i) =>
          i === editingAttr ? { ...a, value: parseInt(attrValue, 10) || 0 } : a
        )
      };
    }
    editingAttr = null;
  }

  function applyImport(result: StResult) {
    if (!draft) return;
    const attributes = draft.attributes.map((a) =>
      result.attributes[a.name] != null ? { ...a, value: result.attributes[a.name] } : a
    );
    const skillMap = new Map(draft.skills.map((s) => [s.name, s]));
    for (const s of result.skills) if (s.name) skillMap.set(s.name, s);
    draft = { ...draft, attributes, skills: [...skillMap.values()] };
  }

  function doDelete() {
    if (draft) {
      repo.pcs.delete(draft);
      confirmDelete = false;
      history.back();
    }
  }

  const fabItems = [
    { icon: 'save', label: '保存', onClick: () => { save(); fabExpanded = false; } },
    ...(showAvatar
      ? [{ icon: 'add_a_photo', label: '头像', onClick: () => { pickAvatar(); fabExpanded = false; } }]
      : []),
    { icon: 'delete', label: '删除', onClick: () => { confirmDelete = true; fabExpanded = false; } }
  ];
</script>

{#if !draft}
  <EmptyState text="角色不存在或已被删除" />
{:else}
  {@const d = draft}
  <div class="page">
    <SectionTopBar title="人物详情" onback={() => history.back()} />
    <div class="page-body pc-body">
      {#if showAvatar}
        <div class="avatar-header">
          <div class="avatar-img"><LoadedImage uri={d.imageUri || null} icon="person" iconSize={56} /></div>
          <div class="avatar-name">{d.name || '人物名称'}</div>
          {#if d.player}<div class="muted">玩家 {d.player}</div>{/if}
        </div>
      {/if}

      <div class="tabs">
        {#each ['属性', '技能', '背景'] as label, i}
          <button class="tab" class:active={selectedTab === i} onclick={() => (selectedTab = i)}>{label}</button>
        {/each}
      </div>

      {#if selectedTab === 0}
        <div class="tab-body">
          <TextField label="姓名" bind:value={d.name} />
          <TextField label="玩家" bind:value={d.player} />
          <TextField label="性别" bind:value={d.gender} />
          <TextField label="年龄" bind:value={d.age} />

          <div class="row between">
            <div class="section-head">属性</div>
            <button class="btn text" onclick={() => (importOpen = true)}>
              <Icon name="cloud_upload" size={18} />
              <span>导入骰娘指令</span>
            </button>
          </div>

          <div class="grid2">
            {#each d.attributes as attr, i (i)}
              <button class="cell" onclick={() => openAttrEdit(i, attr.value)}>
                <div class="cell-label">{attr.name}</div>
                <div class="cell-value">{attr.value}</div>
              </button>
            {/each}
          </div>
        </div>
      {:else if selectedTab === 1}
        <div class="tab-body">
          <div class="row between">
            <button class="btn text" onclick={() => (importOpen = true)}>
              <Icon name="cloud_upload" size={18} />
              <span>导入骰娘指令</span>
            </button>
            <button class="btn text" onclick={addSkill}>
              <Icon name="add" size={18} />
              <span>添加技能</span>
            </button>
          </div>

          <div class="grid2">
            {#each d.skills as skill, i (i)}
              <button class="cell" onclick={() => openSkillEdit(i, skill.name, skill.value)}>
                <div class="cell-label ellipsis">{skill.name || '未命名'}</div>
                <div class="cell-value accent">{skill.value}</div>
              </button>
            {/each}
          </div>
        </div>
      {:else}
        <div class="tab-body">
          <TextField label="外貌" bind:value={d.appearance} />
          <TextField label="信仰" bind:value={d.beliefs} />
          <TextField label="重要之人" bind:value={d.importantPeople} />
          <TextField label="重要之地" bind:value={d.importantPlaces} />
          <TextField label="贵重物品" bind:value={d.valuables} />
          <TextField label="性格" bind:value={d.traits} />
          <TextField label="伤痕" bind:value={d.wounds} />
          <TextField label="恐惧症" bind:value={d.phobias} />
          <TextField label="背景故事" bind:value={d.background} single={false} minLines={3} />
        </div>
      {/if}
    </div>

    <FabMenu
      expanded={fabExpanded}
      onToggle={() => (fabExpanded = !fabExpanded)}
      items={fabItems}
      fabIcon="edit"
      fabDescription="编辑操作"
    />
  </div>

  {#if confirmDelete}
    <Dialog title="删除角色" onclose={() => (confirmDelete = false)}>
      {#snippet children()}
        <p>确定要删除“{d.name}”吗？此操作不可撤销。</p>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (confirmDelete = false)}>取消</button>
        <button class="btn text" style="color:var(--danger);" onclick={doDelete}>删除</button>
      {/snippet}
    </Dialog>
  {/if}

  {#if importOpen}
    <Dialog title="导入骰娘指令 (.st)" onclose={() => (importOpen = false)}>
      {#snippet children()}
        <div class="col" style="gap:12px;">
          <TextField
            label="粘贴 .st 指令"
            placeholder="例如：.st 力量50 敏捷70 意志65 侦查70"
            single={false}
            minLines={3}
            bind:value={rawSt}
          />
          {#if rawSt.trim()}
            <div class="muted small">
              {stResult
                ? `识别到 ${Object.keys(stResult.attributes).length} 项属性、${stResult.skills.length} 项技能`
                : '无法解析输入'}
            </div>
          {/if}
        </div>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (importOpen = false)}>取消</button>
        <button
          class="btn text"
          disabled={!stResult}
          onclick={() => { if (stResult) applyImport(stResult); importOpen = false; }}
        >导入</button>
      {/snippet}
    </Dialog>
  {/if}

  {#if editingSkill !== null}
    <Dialog title="编辑技能" onclose={() => (editingSkill = null)}>
      {#snippet children()}
        <div class="col" style="gap:12px;">
          <TextField label="技能名称" bind:value={skName} />
          <div class="field">
            <label class="label">数值</label>
            <input
              class="text-input"
              inputmode="numeric"
              value={skValue}
              oninput={(e) => (skValue = digits(e.currentTarget.value, 3))}
            />
          </div>
          <button class="btn text delete-skill" onclick={deleteSkill}>
            <Icon name="delete" size={18} />
            <span>删除此技能</span>
          </button>
        </div>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (editingSkill = null)}>取消</button>
        <button class="btn text" onclick={saveSkill}>保存</button>
      {/snippet}
    </Dialog>
  {/if}

  {#if editingAttr !== null}
    <Dialog
      title={`编辑属性：${d.attributes[editingAttr]?.name ?? ''}`}
      onclose={() => (editingAttr = null)}
    >
      {#snippet children()}
        <div class="field">
          <label class="label">数值</label>
          <input
            class="text-input"
            inputmode="numeric"
            value={attrValue}
            oninput={(e) => (attrValue = digits(e.currentTarget.value, 3))}
          />
        </div>
      {/snippet}
      {#snippet actions()}
        <button class="btn text" onclick={() => (editingAttr = null)}>取消</button>
        <button class="btn text" onclick={saveAttr}>保存</button>
      {/snippet}
    </Dialog>
  {/if}
{/if}

<style>
  .pc-body {
    padding-bottom: 96px;
  }
  .avatar-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 16px 0;
  }
  .avatar-img {
    width: 140px;
    height: 140px;
  }
  .avatar-name {
    font-size: 1.5rem;
    font-weight: 600;
    color: var(--text);
  }
  .tabs {
    margin-bottom: 16px;
  }
  .tab-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .section-head {
    font-size: 1rem;
    font-weight: 600;
    color: var(--text);
  }
  .grid2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 14px;
    border: none;
    border-radius: 16px;
    background: var(--bg-container-highest);
    cursor: pointer;
    text-align: left;
  }
  .cell-label {
    font-size: 0.78rem;
    color: var(--text-muted);
  }
  .cell-value {
    font-size: 1rem;
    font-weight: 600;
    color: var(--text);
  }
  .cell-value.accent {
    color: var(--accent);
  }
  .delete-skill {
    align-self: flex-start;
    color: var(--danger);
  }
</style>
