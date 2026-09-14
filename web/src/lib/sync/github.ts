import type { BackupPayload } from '$lib/data/types';

export type HttpPhase = 'UPLOAD' | 'DOWNLOAD';

export interface HttpProgress {
  phase: HttpPhase;
  loaded: number;
  total: number;
}

export interface GithubUser {
  login: string;
  name: string | null;
  avatarUrl: string;
}

export const BACKUP_PATH = 'cochelper_backup.json';

function headers(token: string): Record<string, string> {
  // 令牌只能是可见 ASCII；剔除误粘入的空格/换行/全角/不可见字符
  const clean = token.replace(/[^\x21-\x7E]/g, '');
  return {
    Authorization: `Bearer ${clean}`,
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28',
    'Content-Type': 'application/json'
  };
}

function httpRequest(
  method: string,
  url: string,
  hs: Record<string, string>,
  body?: string,
  onProgress?: (p: HttpProgress) => void
): Promise<{ status: number; body: string }> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url);
    for (const [k, v] of Object.entries(hs)) xhr.setRequestHeader(k, v);
    xhr.onload = () => resolve({ status: xhr.status, body: xhr.responseText });
    xhr.onerror = () =>
      reject(new Error(`fetch ${method} ${url} 失败（网络错误或被 CORS 拦截）`));
    xhr.onabort = () => reject(new Error(`fetch ${method} ${url} 已中止`));
    if (onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress({ phase: 'UPLOAD', loaded: e.loaded, total: e.total });
      };
      xhr.onprogress = (e) => {
        if (e.lengthComputable) onProgress({ phase: 'DOWNLOAD', loaded: e.loaded, total: e.total });
      };
    }
    xhr.send(body ?? '');
  });
}

function utf8ToBase64(s: string): string {
  const bytes = new TextEncoder().encode(s);
  let bin = '';
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  return btoa(bin);
}

function base64ToUtf8(b64: string): string {
  const bin = atob(b64.replace(/\s/g, ''));
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new TextDecoder().decode(bytes);
}

function ok(status: number): boolean {
  return status >= 200 && status <= 299;
}

/** 校验 token 并返回登录用户信息。 */
export async function getUser(token: string): Promise<GithubUser> {
  const resp = await httpRequest('GET', 'https://api.github.com/user', headers(token));
  if (!ok(resp.status)) {
    const hint = resp.status === 401 ? '令牌无效或已过期，请重新生成并粘贴' : `HTTP ${resp.status}`;
    throw new Error(`${hint}：${resp.body.slice(0, 200)}`);
  }
  const raw = JSON.parse(resp.body) as { login: string; name: string | null; avatar_url: string };
  return { login: raw.login ?? '', name: raw.name ?? null, avatarUrl: raw.avatar_url ?? '' };
}

/** 确保私有仓库存在，返回 owner/repo 形式。 */
export async function ensureRepo(token: string, repoName: string): Promise<string> {
  const user = await getUser(token);
  const owner = user.login;
  const getResp = await httpRequest(
    'GET',
    `https://api.github.com/repos/${owner}/${repoName}`,
    headers(token)
  );
  if (!ok(getResp.status)) {
    const createBody = JSON.stringify({
      name: repoName,
      private: true,
      auto_init: true,
      description: 'CocHelper 跑团助手同步数据'
    });
    const createResp = await httpRequest(
      'POST',
      'https://api.github.com/user/repos',
      headers(token),
      createBody
    );
    if (!ok(createResp.status)) throw new Error(`创建仓库失败 HTTP ${createResp.status}`);
  }
  return `${owner}/${repoName}`;
}

type ProgressCb = ((p: HttpProgress) => void) | undefined;

function onlyPhase(cb: ProgressCb, phase: HttpPhase): ProgressCb {
  return cb ? (p) => (p.phase === phase ? cb(p) : undefined) : undefined;
}

export async function pushBackup(
  token: string,
  repoFullName: string,
  payload: BackupPayload,
  onProgress?: (p: HttpProgress) => void
): Promise<void> {
  const encoded = utf8ToBase64(JSON.stringify(payload));
  const up = onlyPhase(onProgress, 'UPLOAD');
  if (encoded.length < 900_000) await pushSmall(token, repoFullName, encoded, up);
  else await pushLargeWithRetry(token, repoFullName, encoded, up);
}

function fail(step: string, resp: { status: number; body: string }): never {
  throw new Error(`${step} HTTP ${resp.status}：${resp.body.slice(0, 200)}`);
}

async function pushSmall(
  token: string,
  repoFullName: string,
  encoded: string,
  onProgress: ProgressCb
): Promise<void> {
  const existing = await fetchContent(token, repoFullName, BACKUP_PATH);
  const body = JSON.stringify({
    message: 'sync: CocHelper backup',
    content: encoded,
    sha: existing?.sha ?? null,
    branch: 'main'
  });
  const resp = await httpRequest(
    'PUT',
    `https://api.github.com/repos/${repoFullName}/contents/${BACKUP_PATH}`,
    headers(token),
    body,
    onProgress
  );
  if (!ok(resp.status)) fail('上传', resp);
}

async function pushLargeWithRetry(
  token: string,
  repoFullName: string,
  encoded: string,
  onProgress: ProgressCb
): Promise<void> {
  let lastError: unknown = null;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      await pushLarge(token, repoFullName, encoded, onProgress);
      return;
    } catch (e) {
      lastError = e;
      if (attempt < 2) await new Promise((r) => setTimeout(r, 1200 * (attempt + 1)));
    }
  }
  throw lastError ?? new Error('pushLarge 失败');
}

async function pushLarge(
  token: string,
  repoFullName: string,
  encoded: string,
  onProgress: ProgressCb
): Promise<void> {
  const hs = headers(token);
  const base = `https://api.github.com/repos/${repoFullName}`;

  // 1. 创建 blob
  const blobResp = await httpRequest(
    'POST',
    `${base}/git/blobs`,
    hs,
    JSON.stringify({ content: encoded, encoding: 'base64' }),
    onProgress
  );
  if (!ok(blobResp.status)) fail('创建 blob', blobResp);
  const blobSha = (JSON.parse(blobResp.body) as { sha: string }).sha;

  // 2. 取 main 分支当前 commit
  const refResp = await httpRequest('GET', `${base}/git/refs/heads/main`, hs);
  if (!ok(refResp.status)) fail('读取分支', refResp);
  const parentSha = (JSON.parse(refResp.body) as { object: { sha: string } }).object.sha;

  // 3. 取 commit 的 tree
  const commitResp = await httpRequest('GET', `${base}/git/commits/${parentSha}`, hs);
  if (!ok(commitResp.status)) fail('读取提交', commitResp);
  const baseTree = (JSON.parse(commitResp.body) as { tree: { sha: string } }).tree.sha;

  // 4. 建 tree
  const treeResp = await httpRequest(
    'POST',
    `${base}/git/trees`,
    hs,
    JSON.stringify({
      base_tree: baseTree,
      tree: [{ path: BACKUP_PATH, mode: '100644', type: 'blob', sha: blobSha }]
    })
  );
  if (!ok(treeResp.status)) fail(`创建 tree（blob=${blobSha} baseTree=${baseTree}）`, treeResp);
  const treeSha = (JSON.parse(treeResp.body) as { sha: string }).sha;

  // 5. 建 commit
  const newCommitResp = await httpRequest(
    'POST',
    `${base}/git/commits`,
    hs,
    JSON.stringify({ message: 'sync: CocHelper backup', tree: treeSha, parents: [parentSha] })
  );
  if (!ok(newCommitResp.status)) fail('创建提交', newCommitResp);
  const newCommitSha = (JSON.parse(newCommitResp.body) as { sha: string }).sha;

  // 6. 更新 main 分支 ref
  const updateResp = await httpRequest(
    'PATCH',
    `${base}/git/refs/heads/main`,
    hs,
    JSON.stringify({ sha: newCommitSha })
  );
  if (!ok(updateResp.status)) fail('更新分支', updateResp);
}

export async function pullBackup(
  token: string,
  repoFullName: string,
  onProgress?: (p: HttpProgress) => void
): Promise<BackupPayload> {
  const payload = await readBackup(token, repoFullName, onlyPhase(onProgress, 'DOWNLOAD'));
  if (!payload) throw new Error('云端尚未有备份数据');
  return payload;
}

export async function fetchBackup(token: string, repoFullName: string): Promise<BackupPayload | null> {
  return readBackup(token, repoFullName);
}

async function readBackup(
  token: string,
  repoFullName: string,
  onProgress?: (p: HttpProgress) => void
): Promise<BackupPayload | null> {
  const file = await fetchContent(token, repoFullName, BACKUP_PATH, onProgress);
  if (!file) return null;

  // Contents API 对 >1MB 的文件不返回 content，只返回 sha → 改走 Git Data API 的 blob 端点。
  let text: string | null;
  if (file.content) {
    text = base64ToUtf8(file.content);
  } else {
    const sha = file.sha;
    if (!sha || file.size == null || file.size === 0) return null;
    text = await fetchBlobContent(token, repoFullName, sha, onProgress);
  }
  if (!text || text.trim() === '') return null;
  try {
    return JSON.parse(text) as BackupPayload;
  } catch (e) {
    throw new Error(`云端备份解析失败（size=${file.size}）：${(e as Error).message}`);
  }
}

async function fetchBlobContent(
  token: string,
  repoFullName: string,
  sha: string,
  onProgress?: (p: HttpProgress) => void
): Promise<string> {
  const resp = await httpRequest(
    'GET',
    `https://api.github.com/repos/${repoFullName}/git/blobs/${sha}`,
    headers(token),
    undefined,
    onProgress
  );
  if (!ok(resp.status)) throw new Error(`读取云端备份 blob 失败 HTTP ${resp.status}`);
  const blob = JSON.parse(resp.body) as { content: string };
  return base64ToUtf8(blob.content);
}

async function fetchContent(
  token: string,
  repoFullName: string,
  path: string,
  onProgress?: (p: HttpProgress) => void
): Promise<{ sha?: string; content?: string; size?: number } | null> {
  const resp = await httpRequest(
    'GET',
    `https://api.github.com/repos/${repoFullName}/contents/${path}`,
    headers(token),
    undefined,
    onProgress
  );
  if (resp.status === 404) return null;
  if (!ok(resp.status)) throw new Error(`读取失败 HTTP ${resp.status}`);
  return JSON.parse(resp.body) as { sha?: string; content?: string; size?: number };
}
