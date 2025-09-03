/* eslint-disable no-console */
export type TokenPayload = {
  u: string; // username or 'local'
  r: 'owner' | 'admin' | 'user';
  iat: number; // issued at (ms)
  exp: number; // expires at (ms)
};

function toBase64Url(input: string): string {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function fromBase64Url(input: string): string {
  const pad = input.length % 4 === 0 ? '' : '='.repeat(4 - (input.length % 4));
  const b64 = input.replace(/-/g, '+').replace(/_/g, '/') + pad;
  return Buffer.from(b64, 'base64').toString();
}

async function hmacSHA256(message: string, secret: string): Promise<string> {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    'raw',
    enc.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', key, enc.encode(message));
  return Buffer.from(new Uint8Array(sig)).toString('base64url');
}

export async function signToken(payload: TokenPayload, secret: string): Promise<string> {
  const header = { alg: 'HS256', typ: 'JWT' };
  const headerB64 = toBase64Url(JSON.stringify(header));
  const payloadB64 = toBase64Url(JSON.stringify(payload));
  const toSign = `${headerB64}.${payloadB64}`;
  const sig = await hmacSHA256(toSign, secret);
  return `${toSign}.${sig}`;
}

export async function verifyToken(token: string, secret: string): Promise<TokenPayload | null> {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const [h, p, s] = parts;
    const toSign = `${h}.${p}`;
    const expectSig = await hmacSHA256(toSign, secret);
    if (s !== expectSig) return null;
    const json = fromBase64Url(p);
    const payload = JSON.parse(json) as TokenPayload;
    if (!payload.exp || Date.now() > payload.exp) return null;
    return payload;
  } catch (e) {
    return null;
  }
}

