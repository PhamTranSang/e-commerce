# WebUI Workspace

Monorepo Nx + pnpm cho các frontend admin của hệ thống e-commerce.

Ứng dụng:

- **commerce-admin** — console quản lý catalog (categories, brands, products, SKUs).
- **system-admin** — console quản lý nền tảng (users, roles); đăng nhập có kiểm tra role.

Cả hai đều dùng React 18 + Vite + Tailwind v4, và gọi tới backend Spring ở
`http://localhost:8080` thông qua dev proxy của Vite (nên không cần cấu hình CORS).

## Yêu cầu

- **Node 20+** (phát triển trên Node 22 / 24).
- **pnpm 9** — repo ghim `pnpm@9.12.3`. Cách đơn giản nhất là dùng Corepack (đi kèm Node):

  ```bash
  corepack enable        # chạy một lần cho mỗi máy
  ```

  Nếu `pnpm` không có trong PATH, thêm tiền tố `corepack ` vào các lệnh bên dưới (ví dụ `corepack pnpm install`).

## Cài đặt

Từ thư mục `webui/` này:

```bash
pnpm install
```

## Chạy development

Mỗi app chạy trên một cổng riêng, có hot-reload:

| App            | Lệnh                        | URL                     |
| -------------- | --------------------------- | ----------------------- |
| commerce-admin | `pnpm dev`                  | http://localhost:4200   |
| commerce-admin | `pnpm dev:commerce-admin`   | http://localhost:4200   |
| system-admin   | `pnpm dev:system-admin`     | http://localhost:4201   |

`pnpm dev` là lối tắt của `dev:commerce-admin`. Chạy hai lệnh ở hai terminal
riêng nếu muốn làm việc trên cả hai app cùng lúc.

Mở app lên sẽ vào thẳng `/login`. Đăng nhập bằng một tài khoản backend (tài khoản
admin được seed sẵn là `admin@ecommerce.local`). Riêng system-admin còn yêu cầu tài khoản có role admin.

### Backend & API proxy

Dev server proxy mọi request `/api/*` sang backend, giữ cho các lời gọi cùng origin:

- Target mặc định: `http://localhost:8080` — hãy khởi động app Spring trước.
- Trỏ sang backend khác mà không cần sửa file:

  ```bash
  VITE_API_PROXY_TARGET=http://localhost:9090 pnpm dev
  ```

- Muốn bỏ qua proxy để gọi thẳng backend (khi đó backend phải bật CORS),
  đặt biến `VITE_API_BASE_URL` thay thế.

> Proxy chỉ tồn tại ở dev/preview. Với production, hãy phục vụ file đã build phía sau
> một reverse proxy hoặc bật CORS trên backend.

## Build

```bash
pnpm build            # build mọi app → dist/apps/<app>
```

Xem thử bản production tại máy (phục vụ `dist/` trên các cổng dev):

```bash
pnpm build
pnpm exec vite preview --config apps/commerce-admin/vite.config.ts   # http://localhost:4200
pnpm exec vite preview --config apps/system-admin/vite.config.ts     # http://localhost:4201
```

## Kiểm tra

```bash
pnpm typecheck        # chạy tsc --noEmit cho toàn bộ project
pnpm lint             # chạy eslint cho toàn bộ project
```

Chạy riêng từng project (nhanh hơn khi đang code):

```bash
pnpm exec nx typecheck commerce-admin
pnpm exec nx build system-admin
```

## Cấu trúc thư mục

```
apps/
  commerce-admin/     # console quản lý catalog
  system-admin/       # console quản lý nền tảng
packages/
  ui/                 # design system: primitives (components/ui) + patterns (components/patterns) + theme.css
  shell/              # app shell dùng chung cho cả hai app: createAuth, Sidebar, Topbar, AppLayout, route config
  api/                # axios client + các lời gọi endpoint có kiểu (auth, catalog, admin)
  types/              # type request/response dùng chung (chính là API contract)
  config/             # hằng số dùng chung
```

Alias import (xem `tsconfig.base.json`): `@ui`, `@shell`, `@api`, `@domain` (→ `packages/types`), `@config`.

## Ghi chú

- Mỗi app tự quản routes của mình trong `src/routes.tsx` — một nguồn duy nhất chi phối
  cả router lẫn sidebar. Thêm một trang vào đó là nó tự xuất hiện ở cả hai chỗ.
- Các endpoint User/Role của system-admin chưa được implement ở backend; phần FE được
  viết theo contract trong `../.claude/plan/docs/system-admin-api-contract.md`.