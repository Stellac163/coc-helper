<script lang="ts">
  import Icon from './Icon.svelte';
  import type { FileEntity } from '$lib/data/types';
  import { downloadFile, openInNewTab } from '$lib/data/fileStore';

  let { file }: { file: FileEntity } = $props();

  const name = file.name.toLowerCase();
  const isImage =
    file.mimeType.startsWith('image/') ||
    /\.(png|jpg|jpeg|gif|webp|bmp)$/.test(name);
  const isText =
    file.mimeType.startsWith('text/') ||
    /\.(txt|md|json|csv)$/.test(name);
  const isPdf = file.mimeType === 'application/pdf' || name.endsWith('.pdf');

  const dataUri = `data:${file.mimeType};base64,${file.contentBase64}`;

  const textContent = (() => {
    try {
      const bin = atob(file.contentBase64.replace(/\s/g, ''));
      const bytes = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
      return new TextDecoder('utf-8').decode(bytes);
    } catch {
      return '无法读取文本内容';
    }
  })();
</script>

{#if isImage}
  <div class="file-content">
    <img class="file-center" src={dataUri} alt={file.name} style="object-fit:contain;" />
  </div>
{:else if isText}
  <div class="file-content">
    <pre class="file-text">{textContent}</pre>
  </div>
{:else if isPdf}
  <div class="file-content file-center-col">
    <Icon name="description" size={48} />
    <div class="file-name">{file.name}</div>
    <div class="row gap-2" style="margin-top:16px;">
      <button class="btn tonal" onclick={() => openInNewTab(file.name, 'application/pdf', file.contentBase64)}>
        新标签页打开
      </button>
      <button class="btn tonal" onclick={() => downloadFile(file.name, 'application/pdf', file.contentBase64)}>
        下载
      </button>
    </div>
  </div>
{:else}
  <div class="file-content file-center-col">
    <Icon name="open_in_new" size={48} />
    <div class="file-muted" style="margin-top:16px;">此类型暂不支持内置预览</div>
    <div class="row gap-2" style="margin-top:16px;">
      <button class="btn tonal" onclick={() => openInNewTab(file.name, file.mimeType, file.contentBase64)}>
        新标签页打开
      </button>
      <button class="btn tonal" onclick={() => downloadFile(file.name, file.mimeType, file.contentBase64)}>
        下载
      </button>
    </div>
  </div>
{/if}
