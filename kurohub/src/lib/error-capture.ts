// Global error capture for SSR catastrophic errors
let lastError: Error | null = null;

function capture(err: Error) {
  lastError = err;
  console.error("[KuroStream Error]", err);
}

if (typeof window !== "undefined") {
  window.addEventListener("error", (e) => capture(e.error));
  window.addEventListener("unhandledrejection", (e) => capture(e.reason));
}

export function consumeLastCapturedError(): Error | null {
  const e = lastError;
  lastError = null;
  return e;
}
