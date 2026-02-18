export interface CategoryResponse {
  id: string;
  groupId: string;
  name: string;
  active: boolean;
}

export interface CategoryGroupResponse {
  id: string;
  name: string;
  type: 'REVENUE' | 'EXPENSE';
  displayOrder: number;
  categories: CategoryResponse[];
}

export interface CreateCategoryGroupRequest {
  name: string;
  type: 'REVENUE' | 'EXPENSE';
}

export interface UpdateCategoryGroupRequest {
  name: string;
}

export interface CreateCategoryRequest {
  groupId: string;
  name: string;
}

export interface UpdateCategoryRequest {
  name: string;
}
