import { Navigate, Route, Routes } from 'react-router-dom';
import { renderRoutes } from '@shell';
import { SystemLayout } from './layouts/SystemLayout';
import { RequireAuth } from './auth';
import { SystemLoginPage } from './features/auth/SystemLoginPage';
import { routes } from './routes';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<SystemLoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<SystemLayout />}>{renderRoutes(routes)}</Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
