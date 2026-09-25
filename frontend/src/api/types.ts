export type Role = "USER" | "ADMIN" | "SUPER_ADMIN";
export type TransactionType = "INCOME" | "EXPENSE" | "SAVINGS" | "TRANSFER";
export type AlertSeverity = "WARNING" | "CRITICAL";

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  role: Role;
  userId: number;
  email: string;
}

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface TransactionRequest {
  type: TransactionType;
  category: string;
  merchant: string;
  amount: number;
  occurredAt: string;
  description?: string;
}

export interface TransactionResponse extends TransactionRequest {
  id: number;
  amount: number;
  idempotencyKey?: string;
}

export interface BulkImportResponse {
  jobId: number;
  totalRows: number;
  successRows: number;
  failedRows: number;
  rowErrors: { row: number; message: string }[];
}

export interface BudgetRequest {
  monthKey: string;
  category: string;
  limitAmount: number;
}

export interface BudgetResponse extends BudgetRequest {
  id: number;
  spentAmount: number;
  percentUsed: number;
  active: boolean;
}

export interface AlertResponse {
  id: number;
  severity: AlertSeverity;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface MonthlyTrend {
  month: string;
  total: number;
}

export interface MerchantSpend {
  merchant: string;
  total: number;
}

export interface HeatmapCell {
  category: string;
  day: number;
  total: number;
}

export interface AnalyticsOverview {
  monthlyTrends: MonthlyTrend[];
  topMerchants: MerchantSpend[];
  heatmap: HeatmapCell[];
  monthOverMonthGrowth: number;
  nextMonthPrediction: number;
  categoryTotals: Record<string, number>;
}

export interface GoalRequest {
  name: string;
  category: string;
  targetAmount: number;
  currentAmount: number;
  deadline: string;
}

export interface GoalResponse extends GoalRequest {
  id: number;
  progressPercent: number;
  dailySavingsRequired: number;
  predictedCompletionDate: string;
  completed: boolean;
}

export interface AdminUserResponse {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  enabled: boolean;
  failedAttempts: number;
  lockedUntil?: string;
}

export interface AuditResponse {
  id: number;
  actorEmail: string;
  action: string;
  target: string;
  status: string;
  details: string;
  createdAt: string;
}

export interface MetricsResponse {
  users: number;
  unreadAlerts: number;
  activeWebSocketSessions: number;
  redis: Record<string, unknown>;
  hikari: Record<string, unknown>;
}
