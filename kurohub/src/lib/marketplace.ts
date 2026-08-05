// KuroStream marketplace store — backed by Firebase (client) + Cloudflare D1 (server API)
// Sellers keep 85%, platform keeps 15%.
import { useCallback, useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  onAuthStateChanged,
  signInWithGoogle,
  signInWithEmail,
  signUpWithEmail,
  signOut as fbSignOut,
  mapUser,
  isFirebaseConfigured,
  type AuthUser,
} from "@/integrations/firebase/auth";
import { getFirebaseAuth } from "@/integrations/firebase/config";

export const PLATFORM_COMMISSION = 0.15;
export const SKINS_PASS_PRICE = 14.99;
export const SKINS_PASS_ID = "skins_pass";

export type SkinCategory = "skin" | "pack" | "pass" | "addon";

export type SkinPalette = {
  primary: string;
  secondary: string;
  bg: string;
  accent: string;
};

export type Extension = {
  id: string;
  name: string;
  author: string;
  description: string;
  longDescription: string;
  category: SkinCategory;
  price: number;
  rating: number;
  installs: number;
  createdAt: number;
  screenshots: string[];
  reviews: { user: string; rating: number; text: string }[];
  fileUrl: string;
  emoji?: string;
  palette?: SkinPalette;
  particle?: string;
  isPremium?: boolean;
};

type ApiItem = {
  id: string;
  name: string;
  author: string;
  description: string;
  long_description: string;
  category: string;
  price: number;
  rating: number;
  installs: number;
  emoji: string | null;
  palette: string | null;
  particle: string | null;
  is_premium: number;
  file_url: string | null;
  screenshots: string | null;
  created_at: string;
};

function parsePalette(p: string | null): SkinPalette | undefined {
  if (!p) return undefined;
  try {
    const parsed = JSON.parse(p);
    if (parsed && typeof parsed === "object" && "primary" in parsed) return parsed as SkinPalette;
  } catch {
    /* ignore */
  }
  return undefined;
}

function parseScreenshots(s: string | null): string[] {
  if (!s) return [];
  try {
    const parsed = JSON.parse(s);
    if (Array.isArray(parsed)) return parsed;
  } catch {
    /* ignore */
  }
  return [];
}

function mapItem(r: ApiItem): Extension {
  return {
    id: r.id,
    name: r.name,
    author: r.author,
    description: r.description,
    longDescription: r.long_description ?? "",
    category: r.category as SkinCategory,
    price: Number(r.price),
    rating: Number(r.rating),
    installs: r.installs,
    createdAt: new Date(r.created_at).getTime(),
    screenshots: parseScreenshots(r.screenshots),
    reviews: [],
    fileUrl: r.file_url ?? "#",
    emoji: r.emoji ?? undefined,
    palette: parsePalette(r.palette),
    particle: r.particle ?? undefined,
    isPremium: !!r.is_premium,
  };
}

// ────────────────────────────────────────────────────────────
// Auth (Firebase)
// ────────────────────────────────────────────────────────────

export function useUser() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isFirebaseConfigured()) {
      setLoading(false);
      return;
    }
    const auth = getFirebaseAuth();
    if (!auth) {
      setLoading(false);
      return;
    }
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(mapUser(u));
      setLoading(false);
    });
    return () => unsub();
  }, []);

  const signOut = useCallback(async () => {
    await fbSignOut();
  }, []);

  return { user, loading, signOut };
}

// Get Firebase ID token for API authentication
export async function getIdToken(): Promise<string | null> {
  const auth = getFirebaseAuth();
  if (!auth) return null;
  const currentUser = auth.currentUser;
  if (!currentUser) return null;
  return currentUser.getIdToken();
}

// Authenticated fetch helper
async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const token = await getIdToken();
  const headers = new Headers(options.headers);
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  return fetch(url, { ...options, headers });
}

// ────────────────────────────────────────────────────────────
// Listings (Cloudflare D1 via API)
// ────────────────────────────────────────────────────────────

export function useListings() {
  return useQuery({
    queryKey: ["marketplace_items"],
    queryFn: async () => {
      const res = await fetch("/api/public/v1/catalog");
      if (!res.ok) throw new Error("Failed to load catalog");
      const json = await res.json();
      return (json.items as ApiItem[]).map(mapItem);
    },
    staleTime: 30_000,
  });
}

export function useListing(id: string) {
  return useQuery({
    queryKey: ["marketplace_items", id],
    queryFn: async () => {
      const res = await fetch(`/api/public/v1/catalog`);
      if (!res.ok) throw new Error("Failed to load catalog");
      const json = await res.json();
      const item = (json.items as ApiItem[]).find((i) => i.id === id);
      if (!item) throw new Error("Not found");
      return mapItem(item);
    },
    staleTime: 30_000,
    enabled: !!id,
  });
}

// ────────────────────────────────────────────────────────────
// Library / Purchases (Cloudflare D1 via API)
// ────────────────────────────────────────────────────────────

export function useLibrary() {
  const { user } = useUser();
  const queryClient = useQueryClient();

  const { data: purchases = [], isLoading } = useQuery({
    queryKey: ["purchases", user?.id],
    queryFn: async () => {
      const res = await authFetch("/api/public/v1/purchases");
      if (!res.ok) throw new Error("Failed to load purchases");
      const json = await res.json();
      return (json.purchases ?? []) as {
        id: string;
        item_id: string;
        amount: number;
        status: string;
        created_at: string;
      }[];
    },
    enabled: !!user,
    staleTime: 10_000,
  });

  const claimFree = useCallback(
    async (itemId: string) => {
      const res = await authFetch("/api/public/v1/purchases", {
        method: "POST",
        body: JSON.stringify({ item_id: itemId }),
      });
      if (!res.ok) throw new Error("Claim failed");
      queryClient.invalidateQueries({ queryKey: ["purchases", user?.id] });
      return res.json();
    },
    [user, queryClient],
  );

  const purchase = useCallback(
    async (itemId: string) => {
      const res = await authFetch("/api/private/v1/checkout-session", {
        method: "POST",
        body: JSON.stringify({ item_id: itemId }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err?.error || "Purchase failed");
      }
      const data = await res.json();
      if (data.url) {
        window.location.href = data.url;
      } else if (data.session_id) {
        queryClient.invalidateQueries({ queryKey: ["purchases", user?.id] });
      }
      return data;
    },
    [user, queryClient],
  );

  return { purchases, isLoading, claimFree, purchase };
}

// ────────────────────────────────────────────────────────────
// Me / Entitlements (Cloudflare D1 via API)
// ────────────────────────────────────────────────────────────

export function useMe() {
  const { user } = useUser();

  return useQuery({
    queryKey: ["me", user?.id],
    queryFn: async () => {
      const res = await authFetch("/api/public/v1/me");
      if (!res.ok) throw new Error("Failed to load profile");
      return res.json();
    },
    enabled: !!user,
    staleTime: 10_000,
  });
}

// Active skin management
export function useActiveSkin() {
  const { user } = useUser();
  const queryClient = useQueryClient();

  const { data: activeSkin } = useQuery({
    queryKey: ["active_skin", user?.id],
    queryFn: async () => {
      const res = await authFetch("/api/public/v1/me");
      if (!res.ok) return null;
      const json = await res.json();
      return json.entitlements?.active_skin_id ?? null;
    },
    enabled: !!user,
    staleTime: 10_000,
  });

  const setActiveSkin = useCallback(
    async (itemId: string) => {
      const res = await authFetch("/api/public/v1/active-skin", {
        method: "POST",
        body: JSON.stringify({ item_id: itemId }),
      });
      if (!res.ok) throw new Error("Failed to set active skin");
      queryClient.invalidateQueries({ queryKey: ["me", user?.id] });
      queryClient.invalidateQueries({ queryKey: ["active_skin", user?.id] });
      return res.json();
    },
    [user, queryClient],
  );

  return { activeSkin, setActiveSkin };
}