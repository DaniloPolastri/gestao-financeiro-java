export type AccountType = 'PAYABLE' | 'RECEIVABLE';
export type AccountStatus = 'PENDING' | 'PAID' | 'RECEIVED' | 'OVERDUE' | 'PARTIAL';

export interface AccountResponse {
  id: string;
  type: AccountType;
  description: string;
  amount: number;
  dueDate: string;
  paymentDate: string | null;
  status: AccountStatus;
  category: { id: string; name: string } | null;
  supplier: { id: string; name: string } | null;
  client: { id: string; name: string } | null;
  recurrenceId: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAccountRequest {
  type: AccountType;
  description: string;
  amount: number;
  dueDate: string;
  categoryId: string;
  supplierId?: string;
  clientId?: string;
  notes?: string;
  recurrence?: RecurrenceRequest;
}

export interface RecurrenceRequest {
  frequency: 'MONTHLY' | 'WEEKLY' | 'BIWEEKLY' | 'YEARLY';
  endDate?: string;
  maxOccurrences?: number;
}

export interface UpdateAccountRequest {
  description: string;
  amount: number;
  dueDate: string;
  categoryId: string;
  supplierId?: string;
  clientId?: string;
  notes?: string;
}

export interface PayAccountRequest {
  paymentDate: string;
  amountPaid?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
