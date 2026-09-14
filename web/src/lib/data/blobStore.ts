const DB_NAME = 'cochelper_blobs';
const DB_VERSION = 1;
const STORE = 'blobs';

/**
 * 极简 IndexedDB 封装：把「文件内容 / 照片」等大字段单独存到 IndexedDB，
 * 绕开 localStorage 约 5MB 的每站点配额限制。
 * 所有 blob 打成一个 JSON 对象、存为单条记录（key="all"），与旧版保持一致。
 */
export class BlobStore {
  private dbPromise: Promise<IDBDatabase | null> = openDb();

  /** 用整份映射覆盖式写入（clear + put 一条记录）。 */
  async putAll(entries: Record<string, string>): Promise<void> {
    const db = await this.dbPromise;
    if (!db) return;
    return new Promise<void>((resolve, reject) => {
      try {
        const tx = db.transaction(STORE, 'readwrite');
        const store = tx.objectStore(STORE);
        store.clear();
        const keys = Object.keys(entries);
        if (keys.length > 0) store.put(JSON.stringify(entries), 'all');
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(new Error('IndexedDB 事务失败'));
        tx.onabort = () => reject(new Error('IndexedDB 事务中止'));
      } catch (e) {
        reject(e);
      }
    });
  }

  /** 读回整份映射；从未写入过时返回空对象。 */
  async getAll(): Promise<Record<string, string>> {
    const db = await this.dbPromise;
    if (!db) return {};
    return new Promise<Record<string, string>>((resolve) => {
      try {
        const tx = db.transaction(STORE, 'readonly');
        const store = tx.objectStore(STORE);
        const req = store.getAll();
        tx.oncomplete = () => {
          const arr = (req.result ?? []) as string[];
          if (arr.length === 0) return resolve({});
          try {
            const parsed = JSON.parse(arr[0]);
            resolve(parsed && typeof parsed === 'object' ? parsed : {});
          } catch {
            resolve({});
          }
        };
        tx.onerror = () => resolve({});
        tx.onabort = () => resolve({});
      } catch {
        resolve({});
      }
    });
  }
}

function openDb(): Promise<IDBDatabase | null> {
  return new Promise((resolve) => {
    if (typeof indexedDB === 'undefined') return resolve(null);
    let settled = false;
    try {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => {
        try {
          req.result.createObjectStore(STORE);
        } catch {
          // store 已存在（仅在版本号提升时走到这里）
        }
      };
      req.onsuccess = () => {
        if (!settled) {
          settled = true;
          resolve(req.result);
        }
      };
      req.onerror = () => {
        if (!settled) {
          settled = true;
          resolve(null);
        }
      };
    } catch {
      resolve(null);
    }
  });
}
