// Firebase ID Token Verification using REST API (Works on Cloudflare Workers)
// Uses Google's public keys to verify Firebase ID tokens without Admin SDK

const FIREBASE_JWKS_URL =
  "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
const FIREBASE_ISSUER_PREFIX = "https://securetoken.google.com/";

interface JWKS {
  [key: string]: string;
}

interface TokenPayload {
  iss: string;
  aud: string;
  auth_time: number;
  user_id: string;
  sub: string;
  iat: number;
  exp: number;
  email?: string;
  email_verified?: boolean;
  firebase: {
    identities: {
      "google.com"?: string[];
      email?: string[];
    };
    sign_in_provider: string;
  };
}

let jwksCache: JWKS | null = null;
let jwksExpiresAt = 0;

async function fetchJWKS(): Promise<JWKS> {
  const now = Date.now();
  if (jwksCache && now < jwksExpiresAt) {
    return jwksCache;
  }

  const response = await fetch(FIREBASE_JWKS_URL);
  if (!response.ok) {
    throw new Error(`Failed to fetch JWKS: ${response.status}`);
  }

  // Respect Cache-Control max-age if present
  const cacheControl = response.headers.get("Cache-Control");
  if (cacheControl) {
    const maxAgeMatch = cacheControl.match(/max-age=(\d+)/);
    if (maxAgeMatch) {
      const maxAge = parseInt(maxAgeMatch[1], 10) * 1000; // Convert to ms
      jwksExpiresAt = now + maxAge;
    } else {
      jwksExpiresAt = now + 3600000; // Default 1 hour
    }
  } else {
    jwksExpiresAt = now + 3600000; // Default 1 hour
  }

  const jwks = (await response.json()) as JWKS;
  jwksCache = jwks;
  return jwks;
}

function parseJwtHeader(token: string): { kid: string; alg: string } | null {
  try {
    const headerB64 = token.split(".")[0];
    const headerJson = atob(headerB64.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(headerJson);
  } catch {
    return null;
  }
}

async function verifySignature(token: string, publicKey: string): Promise<boolean> {
  const [headerB64, payloadB64, signatureB64] = token.split(".");
  const signingInput = `${headerB64}.${payloadB64}`;

  // Import public key
  const pem = `-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`;
  const key = await crypto.subtle.importKey(
    "spki",
    new TextEncoder().encode(
      pem
        .replace(/-----BEGIN PUBLIC KEY-----\n|\n-----END PUBLIC KEY-----/g, "")
        .replace(/\n/g, ""),
    ),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"],
  );

  // Verify signature
  const signature = Uint8Array.from(atob(signatureB64.replace(/-/g, "+").replace(/_/g, "/")), (c) =>
    c.charCodeAt(0),
  );
  const data = new TextEncoder().encode(signingInput);

  return crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, signature, data);
}

export async function verifyFirebaseIdToken(
  token: string,
  projectId: string,
): Promise<TokenPayload | null> {
  try {
    // 1. Parse header to get kid
    const header = parseJwtHeader(token);
    if (!header || !header.kid) {
      console.error("Invalid token header");
      return null;
    }

    // 2. Fetch JWKS and get public key
    const jwks = await fetchJWKS();
    const publicKey = jwks[header.kid];
    if (!publicKey) {
      console.error(`Key ID ${header.kid} not found in JWKS`);
      return null;
    }

    // 3. Verify signature
    const valid = await verifySignature(token, publicKey);
    if (!valid) {
      console.error("Invalid token signature");
      return null;
    }

    // 4. Parse and validate payload
    const payloadB64 = token.split(".")[1];
    const payloadJson = atob(payloadB64.replace(/-/g, "+").replace(/_/g, "/"));
    const payload: TokenPayload = JSON.parse(payloadJson);

    // 5. Validate claims
    const now = Math.floor(Date.now() / 1000);

    // Check expiration
    if (payload.exp < now) {
      console.error("Token expired");
      return null;
    }

    // Check issued at
    if (payload.iat > now + 60) {
      // Allow 60s clock skew
      console.error("Token issued in future");
      return null;
    }

    // Check issuer
    const expectedIssuer = `${FIREBASE_ISSUER_PREFIX}${projectId}`;
    if (payload.iss !== expectedIssuer) {
      console.error(`Invalid issuer: ${payload.iss} != ${expectedIssuer}`);
      return null;
    }

    // Check audience
    if (payload.aud !== projectId) {
      console.error(`Invalid audience: ${payload.aud} != ${projectId}`);
      return null;
    }

    // Check subject matches user_id
    if (payload.sub !== payload.user_id) {
      console.error("Subject mismatch");
      return null;
    }

    return payload;
  } catch (error) {
    console.error("Token verification error:", error);
    return null;
  }
}

export function extractBearerToken(request: Request): string | null {
  const header = request.headers.get("authorization") ?? "";
  if (header.toLowerCase().startsWith("bearer ")) {
    return header.slice(7).trim();
  }
  return null;
}