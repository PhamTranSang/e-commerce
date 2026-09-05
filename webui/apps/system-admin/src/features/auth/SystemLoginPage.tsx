import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Button, IconInput, Checkbox, EyeIcon, EyeOffIcon, LockIcon, MailIcon, ShieldCheckIcon } from '@ui';
import { AccessDeniedError, useAuth } from '../../auth';

export function SystemLoginPage() {
  const { signIn, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await signIn({ login: email, password });
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from ?? '/', { replace: true });
    } catch (caught) {
      setError(
        caught instanceof AccessDeniedError
          ? caught.message
          : 'Sign in failed. Check your email and password, then try again.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-canvas flex min-h-screen flex-col items-center justify-center px-6 py-12">
      <div className="mb-6 flex items-center gap-2.5">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-sidebar">
          <ShieldCheckIcon size={22} className="text-brand-accent" />
        </span>
        <span className="text-lg font-bold text-ink">System Admin</span>
      </div>

      <form onSubmit={handleSubmit} className="w-full max-w-[440px] rounded-xl bg-white p-10 shadow-panel">
        <h1 className="text-[28px] font-bold tracking-tight text-ink">Sign in</h1>
        <p className="mt-2 text-sm leading-relaxed text-ink-muted">Manage users, roles, and platform access</p>

        <div className="mt-8 space-y-5">
          <div>
            <label htmlFor="email" className="mb-2 block text-sm font-semibold text-ink">
              Email
            </label>
            <IconInput
              id="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="Enter your email"
              icon={<MailIcon size={17} />}
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-2 block text-sm font-semibold text-ink">
              Password
            </label>
            <IconInput
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              icon={<LockIcon size={17} />}
              trailing={
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  className="rounded p-1 text-ink-subtle hover:text-ink"
                >
                  {showPassword ? <EyeOffIcon size={17} /> : <EyeIcon size={17} />}
                </button>
              }
            />
          </div>

          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2.5 text-sm text-ink">
              <Checkbox checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
              Remember me
            </label>
            <a href="#forgot" className="text-sm font-medium text-brand hover:underline">
              Forgot password?
            </a>
          </div>

          {error ? (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
              {error}
            </p>
          ) : null}

          <Button type="submit" size="lg" className="w-full" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </Button>
        </div>
      </form>
    </div>
  );
}
