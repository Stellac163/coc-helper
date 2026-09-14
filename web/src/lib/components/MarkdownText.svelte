<script lang="ts">
  import { markdownToHtml } from '$lib/utils/markdown';
  let { text }: { text: string } = $props();
</script>

<div class="rich-content">{@html markdownToHtml(text)}</div>
