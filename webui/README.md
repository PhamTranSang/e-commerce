# WebUI Workspace

Nx + pnpm monorepo for the e-commerce admin frontends.

Apps:

- **commerce-admin** — catalog console (categories, brands, products, SKUs).
- **system-admin** — platform console (users, roles); role-gated login.

Both are React 18 + Vite + Tailwind v4, and talk to the Spring backend at
`http://localhost:8080` through the Vite dev proxy (so no CORS setup is needed).

## Prerequisites

- **Node 20+** (developed on Node 22 / 24).
- **pnpm 9** — the repo pins `pnpm@9.12.3`. The easiest way is Corepack (ships with Node):

  ```bash
  corepack enable        # once per machine
  ```

  If `pnpm` isn't on your PATH, prefix the commands below with `corepack ` (e.g. `corepack pnpm install`).

## Install

From this `webui/` directory:

```bash
pnpm install
```

## Run a dev server

Each app runs on its own port with hot-reload:

| App            | Command                     | URL                     |
| -------------- | --------------------------- | ----------------------- |
| commerce-admin | `pnpm dev`                  | http://localhost:4200   |
| commerce-admin | `pnpm dev:commerce-admin`   | http://localhost:4200   |
| system-admin   | `pnpm dev:system-admin`     | http://localhost:4201   |

`pnpm dev` is a shortcut for `dev:commerce-admin`. Run the two commands in
separate terminals to work on both apps at once.

Open the app and you land on `/login`. Sign in with a backend account (the seeded
admin is `admin@ecommerce.local`). system-admin additionally requires an admin role.

### Backend & API proxy

The dev server proxies every `/api/*` request to the backend, keeping calls same-origin:

- Default target: `http://localhost:8080` — start the Spring app first.
- Point at a different backend without editing files:

  ```bash
  VITE_API_PROXY_TARGET=http://localhost:9090 pnpm dev
  ```

- To bypass the proxy and call a backend directly (that backend must then enable CORS),
  set `VITE_API_BASE_URL` instead.

> The proxy only exists in dev/preview. For production, serve the built files behind a
> reverse proxy or enable CORS on the backend.

## Build

```bash
pnpm build            # build every app → dist/apps/<app>
```

Preview a production build locally (serves `dist/` on the dev ports):

```bash
pnpm build
pnpm exec vite preview --config apps/commerce-admin/vite.config.ts   # http://localhost:4200
pnpm exec vite preview --config apps/system-admin/vite.config.ts     # http://localhost:4201
```

## Checks

```bash
pnpm typecheck        # tsc --noEmit across all projects
pnpm lint             # eslint across all projects
```

Per-project (faster while iterating):

```bash
pnpm exec nx typecheck commerce-admin
pnpm exec nx build system-admin
```

## Layout

```
apps/
  commerce-admin/     # catalog console
  system-admin/       # platform console
packages/
  ui/                 # design system: primitives (components/ui) + patterns (components/patterns) + theme.css
  shell/              # app shell shared by both apps: createAuth, Sidebar, Topbar, AppLayout, route config
  api/                # axios client + typed endpoint calls (auth, catalog, admin)
  types/              # shared response/request types (the API contract)
  config/             # shared constants
```

Import aliases (see `tsconfig.base.json`): `@ui`, `@shell`, `@api`, `@domain` (→ `packages/types`), `@config`.

## Notes

- Each app owns its routes in `src/routes.tsx` — a single source that drives both
  the router and the sidebar. Add a page there and it shows up in both.
- The system-admin User/Role endpoints are not implemented on the backend yet; the FE
  is written against the contract in `docs/system-admin-api-contract.md`.
