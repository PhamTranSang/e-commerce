import type { AppRoute } from '@shell';
import { FolderIcon, GridIcon, HomeIcon, PackageIcon, TagIcon } from '@ui';
import { CommerceDashboardPage } from './features/dashboard/CommerceDashboardPage';
import { CommerceCategoriesPage } from './features/categories/CommerceCategoriesPage';
import { CommerceBrandsPage } from './features/brands/CommerceBrandsPage';
import { CommerceProductsPage } from './features/products/CommerceProductsPage';
import { CommerceSkusPage } from './features/skus/CommerceSkusPage';

export const routes: AppRoute[] = [
  { path: '/', label: 'Dashboard', icon: HomeIcon, element: <CommerceDashboardPage />, end: true },
  { path: '/categories', label: 'Categories', icon: FolderIcon, element: <CommerceCategoriesPage />, expandable: true },
  { path: '/brands', label: 'Brands', icon: TagIcon, element: <CommerceBrandsPage /> },
  { path: '/products', label: 'Products', icon: PackageIcon, element: <CommerceProductsPage /> },
  { path: '/skus', label: 'SKUs', icon: GridIcon, element: <CommerceSkusPage /> }
];
