import { Navigate, Route, Routes } from 'react-router-dom';
import { renderRoutes } from '@shell';
import { CommerceLayout } from './layouts/CommerceLayout';
import { RequireAuth } from './auth';
import { CommerceLoginPage } from './features/auth/CommerceLoginPage';
import { routes } from './routes';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<CommerceLoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<CommerceLayout />}>{renderRoutes(routes)}</Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
