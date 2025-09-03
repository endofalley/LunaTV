# Repository Guidelines

## Project Structure & Modules
- `src/app`: Next.js App Router pages, API routes, layout, styles.
- `src/components`: Reusable UI components (prefer `PascalCase` filenames).
- `src/hooks`: Custom React hooks (prefix with `use`).
- `src/lib`: Utilities, data access, versioning helpers.
- `src/styles`: Global and Tailwind styles.
- `public`: Static assets (icons, images, manifest, PWA files).
- `scripts`: Node scripts (e.g., `generate-manifest.js`).

## Build, Test, and Development
- `pnpm dev`: Start local dev server with manifest generation.
- `pnpm build`: Production build (runs manifest generation).
- `pnpm start`: Run built app.
- `pnpm lint` / `pnpm lint:strict`: Lint code; strict fails on warnings.
- `pnpm typecheck`: TypeScript type checks.
- `pnpm test` / `pnpm test:watch`: Run Jest tests.
- `pnpm format` / `pnpm format:check`: Prettier write/check.

## Coding Style & Naming
- Language: TypeScript, React, Next.js 14 App Router, Tailwind CSS.
- Formatting: Prettier (2‑space indent, single quotes, semicolons).
- Linting: ESLint (`next/core-web-vitals`, `@typescript-eslint`).
- Imports: Sorted via `simple-import-sort`; remove unused via `unused-imports`.
- Components: `PascalCase`; hooks: `useX`; files under `app/` follow Next.js conventions (e.g., `page.tsx`, `route.ts`).
- Env: Use `.env.local` for secrets; never commit credentials.

## Testing Guidelines
- Framework: Jest + Testing Library (`jest-environment-jsdom`).
- Location: Co-locate tests with source or under `src/**` using `*.test.ts(x)`.
- Examples: `src/components/Widget.test.tsx`.
- Run: `pnpm test` (CI-safe) or `pnpm test:watch` during dev.

## Commit & Pull Requests
- Conventional Commits enforced via commitlint and Husky.
  - Types: `feat`, `fix`, `docs`, `chore`, `style`, `refactor`, `ci`, `test`, `perf`, `revert`, `vercel`.
  - Example: `feat(play): add HLS error recovery`.
- Pre-commit: lint-staged runs ESLint and Prettier.
- PRs: include scope/intent, linked issues, screenshots for UI, and test notes; pass `lint`, `typecheck`, and `build` locally.

## Security & Configuration
- Required env: `USERNAME`, `PASSWORD`, and storage settings (`NEXT_PUBLIC_STORAGE_TYPE`, `KVROCKS_URL`/`REDIS_URL`/`UPSTASH_*`). See README.
- Do not expose admin without auth; avoid committing `.env*`.
- For Docker, prefer Kvrocks for durability; Redis may lose data.
