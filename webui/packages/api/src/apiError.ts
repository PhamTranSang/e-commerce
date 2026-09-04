import axios from 'axios';
import type { ProblemDetail } from '@domain/index';

export type ParsedApiError = {
  message: string;
  /** Field name → message, from the backend's `errors[]` validation array. */
  fieldErrors: Record<string, string>;
};

/** Turn any thrown value from an axios mutation into a message + per-field errors. */
export function parseApiError(error: unknown): ParsedApiError {
  if (axios.isAxiosError(error)) {
    const problem = error.response?.data as ProblemDetail | undefined;
    const fieldErrors: Record<string, string> = {};
    for (const item of problem?.errors ?? []) {
      if (item.field && !fieldErrors[item.field]) fieldErrors[item.field] = item.message;
    }
    const message =
      problem?.detail ??
      (error.response?.status === 401
        ? 'Your session has expired. Please sign in again.'
        : 'Something went wrong. Please try again.');
    return { message, fieldErrors };
  }
  return { message: 'Unexpected error. Please try again.', fieldErrors: {} };
}
