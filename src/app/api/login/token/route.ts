/* eslint-disable no-console,@typescript-eslint/no-explicit-any */
import { NextRequest, NextResponse } from 'next/server';

import { getConfig } from '@/lib/config';
import { db } from '@/lib/db';
import { signToken } from '@/lib/token';

export const runtime = 'nodejs';

const STORAGE_TYPE =
  (process.env.NEXT_PUBLIC_STORAGE_TYPE as
    | 'localstorage'
    | 'redis'
    | 'upstash'
    | 'kvrocks'
    | undefined) || 'localstorage';

function json(data: any, init?: ResponseInit) {
  const res = NextResponse.json(data, init);
  applyCorsHeaders(res);
  return res;
}

function applyCorsHeaders(res: NextResponse) {
  const allowOrigin = process.env.CORS_ALLOW_ORIGIN || '*';
  res.headers.set('Access-Control-Allow-Origin', allowOrigin);
  res.headers.set('Vary', 'Origin');
  res.headers.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.headers.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
}

export async function OPTIONS() {
  const res = new NextResponse(null, { status: 204 });
  applyCorsHeaders(res);
  return res;
}

export async function POST(req: NextRequest) {
  try {
    const secret = process.env.PASSWORD || '';
    if (!secret) return json({ error: 'Server not configured' }, { status: 500 });

    if (STORAGE_TYPE === 'localstorage') {
      const { password } = await req.json();
      if (!password || typeof password !== 'string') {
        return json({ error: '密码不能为空' }, { status: 400 });
      }
      if (password !== secret) {
        return json({ error: '密码错误' }, { status: 401 });
      }
      const now = Date.now();
      const exp = now + 7 * 24 * 60 * 60 * 1000;
      const token = await signToken({ u: 'local', r: 'user', iat: now, exp }, secret);
      return json({ token, user: { username: 'local', role: 'user' }, exp });
    }

    const { username, password } = await req.json();
    if (!username || typeof username !== 'string') {
      return json({ error: '用户名不能为空' }, { status: 400 });
    }
    if (!password || typeof password !== 'string') {
      return json({ error: '密码不能为空' }, { status: 400 });
    }

    if (username === process.env.USERNAME && password === process.env.PASSWORD) {
      const now = Date.now();
      const exp = now + 7 * 24 * 60 * 60 * 1000;
      const token = await signToken({ u: username, r: 'owner', iat: now, exp }, secret);
      return json({ token, user: { username, role: 'owner' }, exp });
    }

    const config = await getConfig();
    const appUser = config.UserConfig.Users.find((u) => u.username === username);
    if (appUser && appUser.banned) {
      return json({ error: '用户被封禁' }, { status: 401 });
    }

    try {
      const pass = await db.verifyUser(username, password);
      if (!pass) return json({ error: '用户名或密码错误' }, { status: 401 });
      const role = (appUser?.role as 'owner' | 'admin' | 'user') || 'user';
      const now = Date.now();
      const exp = now + 7 * 24 * 60 * 60 * 1000;
      const token = await signToken({ u: username, r: role, iat: now, exp }, secret);
      return json({ token, user: { username, role }, exp });
    } catch (e) {
      console.error('数据库验证失败', e);
      return json({ error: '数据库错误' }, { status: 500 });
    }
  } catch (error) {
    console.error('Token 登录接口异常', error);
    return json({ error: '服务器错误' }, { status: 500 });
  }
}

