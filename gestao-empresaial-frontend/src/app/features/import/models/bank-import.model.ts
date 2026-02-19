export interface BankImportItem {
  id: string;
  date: string;
  description: string;
  amount: number;
  type: 'CREDIT' | 'DEBIT';
  accountType: 'PAYABLE' | 'RECEIVABLE';
  supplierId: string | null;
  supplierName: string | null;
  categoryId: string | null;
  categoryName: string | null;
  possibleDuplicate: boolean;
}

export interface BankImport {
  id: string;
  fileName: string;
  fileType: 'OFX' | 'CSV';
  status: 'PENDING_REVIEW' | 'COMPLETED' | 'CANCELLED';
  totalRecords: number;
  createdAt: string;
  items: BankImportItem[];
}

export interface BankImportSummary {
  id: string;
  fileName: string;
  fileType: 'OFX' | 'CSV';
  status: 'PENDING_REVIEW' | 'COMPLETED' | 'CANCELLED';
  totalRecords: number;
  createdAt: string;
}

export interface UpdateImportItemRequest {
  supplierId?: string;
  categoryId?: string;
  accountType?: 'PAYABLE' | 'RECEIVABLE';
}

export interface BatchUpdateImportItemsRequest {
  itemIds: string[];
  supplierId?: string;
  categoryId?: string;
  accountType?: 'PAYABLE' | 'RECEIVABLE';
}
