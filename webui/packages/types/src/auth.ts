export type AuthRole = 'ADMIN' | 'STAFF' | 'SUPER_ADMIN' | 'SECURITY_ADMIN' | 'OPS_ADMIN' | 'AUDITOR' | 'VIEWER';

export type LoginRequest = {
  login: string;
  password: string;
};

/** Mirrors app.ecommerce.security.api.dto.response.LoginResponse. */
export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  accountId: string;
  email: string;
  fullName: string;
  roles: string[];
};
