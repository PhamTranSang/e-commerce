import type { AuthenticatedAccount, LoginRequest, LoginResponse } from '@domain/index';
import { http } from './http';

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await http.post<LoginResponse>('/api/auth/login', payload);
  return data;
}

export async function fetchCurrentAccount(): Promise<AuthenticatedAccount> {
  const { data } = await http.get<AuthenticatedAccount>('/api/auth/me');
  return data;
}
