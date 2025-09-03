/* eslint-disable no-console */

import { NextRequest, NextResponse } from 'next/server';

import { getAuthInfoFromCookie } from '@/lib/auth';
import { verifyToken } from '@/lib/token';

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isApi = pathname.startsWith('/api');

  // CORS 预检处理（仅 API）
  if (isApi && request.method === 'OPTIONS') {
    const res = new NextResponse(null, { status: 204 });
    applyCorsHeaders(res);
    return res;
  }

  // 跳过不需要认证的路径
  if (shouldSkipAuth(pathname)) {
    const res = NextResponse.next();
    if (isApi) applyCorsHeaders(res);
    return res;
  }

  const storageType = process.env.NEXT_PUBLIC_STORAGE_TYPE || 'localstorage';

  if (!process.env.PASSWORD) {
    // 如果没有设置密码，页面重定向到警告；API 返回 401
    if (isApi) {
      const res = new NextResponse('Unauthorized', { status: 401 });
      applyCorsHeaders(res);
      return res;
    }
    const warningUrl = new URL('/warning', request.url);
    return NextResponse.redirect(warningUrl);
  }

  // API: Bearer Token 优先
  if (isApi) {
    const authz = request.headers.get('authorization');
    const secret = process.env.PASSWORD || '';
    if (authz && authz.toLowerCase().startsWith('bearer ')) {
      const token = authz.slice(7).trim();
      const payload = await verifyToken(token, secret);
      if (payload) {
        const res = NextResponse.next();
        applyCorsHeaders(res);
        return res;
      }
    }
  }

  // Cookie 认证（兼容 Web）
  const authInfo = getAuthInfoFromCookie(request);
  if (!authInfo) {
    return handleAuthFailure(request, pathname);
  }

  // localstorage模式：在middleware中完成验证
  if (storageType === 'localstorage') {
    if (!authInfo.password || authInfo.password !== process.env.PASSWORD) {
      return handleAuthFailure(request, pathname);
    }
    const res = NextResponse.next();
    if (isApi) applyCorsHeaders(res);
    return res;
  }

  // 其他模式：只验证签名
  if (!authInfo.username || !authInfo.signature) {
    return handleAuthFailure(request, pathname);
  }

  const isValidSignature = await verifySignature(
    authInfo.username,
    authInfo.signature,
    process.env.PASSWORD || ''
  );
  if (isValidSignature) {
    const res = NextResponse.next();
    if (isApi) applyCorsHeaders(res);
    return res;
  }

  return handleAuthFailure(request, pathname);
}

// 验证签名
async function verifySignature(
  data: string,
  signature: string,
  secret: string
): Promise<boolean> {
  const encoder = new TextEncoder();
  const keyData = encoder.encode(secret);
  const messageData = encoder.encode(data);

  try {
    // 导入密钥
    const key = await crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['verify']
    );

    // 将十六进制字符串转换为Uint8Array
    const signatureBuffer = new Uint8Array(
      signature.match(/.{1,2}/g)?.map((byte) => parseInt(byte, 16)) || []
    );

    // 验证签名
    return await crypto.subtle.verify(
      'HMAC',
      key,
      signatureBuffer,
      messageData
    );
  } catch (error) {
    console.error('签名验证失败:', error);
    return false;
  }
}

// 处理认证失败的情况
function handleAuthFailure(
  request: NextRequest,
  pathname: string
): NextResponse {
  // 如果是 API 路由，返回 401 状态码
  if (pathname.startsWith('/api')) {
    const res = new NextResponse('Unauthorized', { status: 401 });
    applyCorsHeaders(res);
    return res;
  }

  // 否则重定向到登录页面
  const loginUrl = new URL('/login', request.url);
  // 保留完整的URL，包括查询参数
  const fullUrl = `${pathname}${request.nextUrl.search}`;
  loginUrl.searchParams.set('redirect', fullUrl);
  return NextResponse.redirect(loginUrl);
}

// 判断是否需要跳过认证的路径
function shouldSkipAuth(pathname: string): boolean {
  const skipPaths = [
    '/_next',
    '/favicon.ico',
    '/robots.txt',
    '/manifest.json',
    '/icons/',
    '/logo.png',
    '/screenshot.png',
  ];

  return skipPaths.some((path) => pathname.startsWith(path));
}

// 配置middleware匹配规则
export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|login|warning|api/login|api/register|api/logout|api/cron|api/server-config).*)',
  ],
};

function applyCorsHeaders(res: NextResponse) {
  const allowOrigin = process.env.CORS_ALLOW_ORIGIN || '*';
  res.headers.set('Access-Control-Allow-Origin', allowOrigin);
  res.headers.set('Vary', 'Origin');
  res.headers.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.headers.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
}
