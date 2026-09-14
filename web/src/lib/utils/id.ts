/** 生成 53 位安全范围内的正整型 ID（兼容 Kotlin Long 时代的旧数据，避免超 Number 精度）。 */
export function nextId(): number {
  const buf = new Uint32Array(2);
  crypto.getRandomValues(buf);
  // 取高 21 位 + 低 32 位 = 53 位，落在 Number.MAX_SAFE_INTEGER 内
  const id = (buf[0] >>> 11) * 0x100000000 + buf[1];
  return id === 0 ? 1 : id;
}

export function now(): number {
  return Date.now();
}
