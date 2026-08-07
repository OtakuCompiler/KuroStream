// KuroStream Sync Room Durable Object
// Manages WebSocket connections for real-time cross-device sync

interface SyncMessage {
  type: "sync" | "ping" | "pong" | "user_left";
  payload?: any;
}

export class SyncRoom {
  private state: DurableObjectState;
  private env: unknown;
  private sessions: Set<WebSocket>;

  constructor(state: DurableObjectState, env: unknown) {
    this.state = state;
    this.env = env;
    this.sessions = new Set();
  }

  async fetch(request: Request): Promise<Response> {
    const upgradeHeader = request.headers.get("Upgrade");
    if (upgradeHeader !== "websocket") {
      return new Response("Expected WebSocket", { status: 426 });
    }

    const pair: WebSocketPair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];

    server.accept();
    this.sessions.add(server);

    server.addEventListener("message", (event: MessageEvent) => {
      try {
        const message: SyncMessage = JSON.parse(event.data as string);
        this.handleMessage(server, message);
      } catch (e) {
        console.error("Invalid message:", e);
      }
    });

    server.addEventListener("close", () => {
      this.sessions.delete(server);
      this.broadcast({ type: "user_left", payload: { timestamp: Date.now() } }, server);
    });

    return new Response(null, { status: 101, webSocket: client } as any);
  }

  private async handleMessage(ws: WebSocket, message: SyncMessage) {
    switch (message.type) {
      case "ping":
        ws.send(JSON.stringify({ type: "pong", timestamp: Date.now() }));
        break;
      case "sync":
        await this.broadcast(message, ws);
        break;
      default:
        console.warn("Unknown message type:", message.type);
    }
  }

  private broadcast(message: SyncMessage, exclude?: WebSocket) {
    const data = JSON.stringify(message);
    for (const session of this.sessions) {
      if (session !== exclude && session.readyState === WebSocket.OPEN) {
        session.send(data);
      }
    }
  }
}
