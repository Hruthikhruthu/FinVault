import { createContext, ReactNode, useContext, useMemo, useReducer } from "react";
import type { AlertResponse, GoalResponse, TransactionResponse } from "../api/types";

export interface FeedItem {
  id: string;
  kind: "transaction" | "alert" | "goal" | "import";
  title: string;
  detail: string;
  at: string;
}

interface AppState {
  feed: FeedItem[];
}

type AppAction =
  | { type: "transaction"; payload: TransactionResponse }
  | { type: "alert"; payload: AlertResponse }
  | { type: "goal"; payload: GoalResponse }
  | { type: "import"; payload: { successRows: number; failedRows: number } };

const AppStateContext = createContext<{ state: AppState; dispatch: React.Dispatch<AppAction> } | null>(null);

function feedReducer(state: AppState, action: AppAction): AppState {
  const next = toFeedItem(action);
  return { feed: [next, ...state.feed].slice(0, 8) };
}

function toFeedItem(action: AppAction): FeedItem {
  const now = new Date().toISOString();
  switch (action.type) {
    case "transaction":
      return {
        id: `tx-${action.payload.id}-${now}`,
        kind: "transaction",
        title: `${action.payload.type} saved`,
        detail: `${action.payload.merchant} - ${action.payload.category}`,
        at: now
      };
    case "alert":
      return {
        id: `alert-${action.payload.id}-${now}`,
        kind: "alert",
        title: `${action.payload.severity} budget alert`,
        detail: action.payload.message,
        at: now
      };
    case "goal":
      return {
        id: `goal-${action.payload.id}-${now}`,
        kind: "goal",
        title: `${action.payload.name} milestone`,
        detail: `${action.payload.progressPercent.toFixed(0)}% complete`,
        at: now
      };
    case "import":
      return {
        id: `import-${now}`,
        kind: "import",
        title: "CSV import finished",
        detail: `${action.payload.successRows} imported, ${action.payload.failedRows} failed`,
        at: now
      };
    default:
      return { id: now, kind: "transaction", title: "Update", detail: "", at: now };
  }
}

export function AppStateProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(feedReducer, { feed: [] });
  const value = useMemo(() => ({ state, dispatch }), [state]);
  return <AppStateContext.Provider value={value}>{children}</AppStateContext.Provider>;
}

export function useAppState() {
  const context = useContext(AppStateContext);
  if (!context) {
    throw new Error("useAppState must be used inside AppStateProvider");
  }
  return context;
}
