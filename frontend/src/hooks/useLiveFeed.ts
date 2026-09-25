import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useEffect } from "react";
import { websocketUrl } from "../api/client";
import type { AlertResponse, BulkImportResponse, GoalResponse, TransactionResponse } from "../api/types";
import { useAppState } from "../context/AppStateContext";

export function useLiveFeed(userId?: number) {
  const { dispatch } = useAppState();

  useEffect(() => {
    if (!userId) {
      return;
    }
    const client = new Client({
      reconnectDelay: 5000,
      webSocketFactory: () => new SockJS(websocketUrl()),
      onConnect: () => {
        client.subscribe(`/topic/users/${userId}/transactions`, message => {
          dispatch({ type: "transaction", payload: JSON.parse(message.body) as TransactionResponse });
        });
        client.subscribe(`/topic/users/${userId}/alerts`, message => {
          dispatch({ type: "alert", payload: JSON.parse(message.body) as AlertResponse });
        });
        client.subscribe(`/topic/users/${userId}/goals`, message => {
          dispatch({ type: "goal", payload: JSON.parse(message.body) as GoalResponse });
        });
        client.subscribe(`/topic/users/${userId}/imports`, message => {
          const payload = JSON.parse(message.body) as BulkImportResponse;
          dispatch({ type: "import", payload });
        });
      }
    });
    client.activate();
    return () => {
      client.deactivate();
    };
  }, [dispatch, userId]);
}
