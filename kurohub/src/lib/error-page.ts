export function renderErrorPage(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Error — Kuro Stream</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      background: #0a0a0f;
      color: #e2e2e8;
      font-family: system-ui, -apple-system, sans-serif;
      display: grid;
      place-items: center;
      min-height: 100vh;
      padding: 2rem;
    }
    .card {
      max-width: 420px;
      text-align: center;
      padding: 2.5rem;
      border-radius: 1rem;
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.06);
      backdrop-filter: blur(20px);
    }
    h1 { font-size: 1.5rem; font-weight: 700; margin-bottom: 0.5rem; background: linear-gradient(135deg, #BB86FC, #03DAC6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    p { color: #888; font-size: 0.875rem; line-height: 1.6; margin-bottom: 1.5rem; }
    a { display: inline-block; padding: 0.6rem 1.2rem; background: rgba(187,134,252,0.15); color: #BB86FC; text-decoration: none; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 500; border: 1px solid rgba(187,134,252,0.2); transition: all 0.2s; }
    a:hover { background: rgba(187,134,252,0.25); }
  </style>
</head>
<body>
  <div class="card">
    <h1>Something went wrong</h1>
    <p>We're sorry, but something unexpected happened. Please try refreshing the page or come back later.</p>
    <a href="/">Back to home</a>
  </div>
</body>
</html>`;
}
