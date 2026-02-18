export interface ClientResponse {
  id: string;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClientRequest {
  name: string;
  document?: string;
  email?: string;
  phone?: string;
}

export interface UpdateClientRequest {
  name: string;
  document?: string;
  email?: string;
  phone?: string;
}
