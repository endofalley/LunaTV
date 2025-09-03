import { NextRequest } from 'next/server';

import { getAuthInfoFromCookie } from '@/lib/auth';
import { verifyToken } from '@/lib/token';

export type AuthInfo = {
  username?: string;
  role?: 'owner' | 'admin' | 'user';
} | null;

export async function getAuthInfoFromRequest(req: NextRequest): Promise<AuthInfo> {
  const authz = req.headers.get('authorization');
  const secret = process.env.PASSWORD || '';
  if (authz && authz.toLowerCase().startsWith('bearer ') && secret) {
    const token = authz.slice(7).trim();
    const payload = await verifyToken(token, secret);
    if (payload) {
      return { username: payload.u, role: payload.r };
    }
  }
  // 回退到 Cookie
  const info = getAuthInfoFromCookie(req);
  if (info?.username) {
    return { username: info.username, role: info as any };
  }
  return null;
}

