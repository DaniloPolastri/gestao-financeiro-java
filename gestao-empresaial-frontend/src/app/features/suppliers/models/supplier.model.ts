export interface SupplierResponse {
  id: string;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierRequest {
  name: string;
  document?: string;
  email?: string;
  phone?: string;
}

export interface UpdateSupplierRequest {
  name: string;
  document?: string;
  email?: string;
  phone?: string;
}
