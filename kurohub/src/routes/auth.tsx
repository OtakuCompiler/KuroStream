import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { signInWithGoogle, signInWithEmail, signUpWithEmail } from "@/integrations/firebase/auth";
import { useUser } from "@/lib/marketplace";
import { m, AnimatePresence } from "framer-motion";
import { Mail, Lock, User, ArrowRight, Chrome, Eye, EyeOff } from "lucide-react";

const searchSchema = z.object({ redirect: z.string().optional() });

export const Route = createFileRoute("/auth")({
  validateSearch: searchSchema,
  head: () => ({
    meta: [
      { title: "Sign in — Kuro Stream" },
      {
        name: "description",
        content: "Sign in to sync purchases and skins across every Kuro Stream device.",
      },
    ],
  }),
  component: AuthPage,
});

function AuthPage() {
  const { user } = useUser();
  const navigate = useNavigate();
  const { redirect } = Route.useSearch();
  const [mode, setMode] = useState<"signin" | "signup">("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPass, setShowPass] = useState(false);
  const [displayName, setDisplayName] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (user && redirect) navigate({ to: redirect as "/" });
    else if (user) navigate({ to: "/account" });
  }, [user, redirect, navigate]);

  const handleEmail = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      if (mode === "signup") {
        await signUpWithEmail(email, password, displayName || email.split("@")[0]);
      } else {
        await signInWithEmail(email, password);
      }
    } catch (e2) {
      setErr(e2 instanceof Error ? e2.message : String(e2));
    } finally {
      setBusy(false);
    }
  };

  const handleGoogle = async () => {
    setErr(null);
    try {
      await signInWithGoogle();
    } catch (e2) {
      setErr(e2 instanceof Error ? e2.message : String(e2));
    }
  };

  if (user) {
    return (
      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mx-auto max-w-md px-5 py-20 text-center"
      >
        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
          <User className="w-8 h-8 text-primary" />
        </div>
        <h1 className="font-display text-2xl font-bold">You're signed in</h1>
        <p className="text-sm text-muted-foreground mt-2">{user.email}</p>
        <Button className="mt-6" onClick={() => navigate({ to: "/account" })}>
          Go to account
        </Button>
      </m.div>
    );
  }

  return (
    <m.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="mx-auto max-w-md px-5 py-16"
    >
      <div className="text-center mb-8">
        <h1 className="font-display text-3xl font-bold text-gradient">
          {mode === "signup" ? "Create account" : "Welcome back"}
        </h1>
        <p className="text-sm text-muted-foreground mt-2">
          Sync purchases, skins, and preferences across all your devices.
        </p>
      </div>

      <m.button
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        onClick={handleGoogle}
        className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-white/5 border border-border/50 text-sm hover:bg-white/10 transition-colors"
      >
        <Chrome className="w-4 h-4" /> Continue with Google
      </m.button>

      <div className="my-6 flex items-center gap-3">
        <span className="flex-1 h-px bg-border/30" />
        <span className="text-xs text-muted-foreground/60">or continue with email</span>
        <span className="flex-1 h-px bg-border/30" />
      </div>

      <form onSubmit={handleEmail} className="space-y-4">
        <AnimatePresence mode="wait">
          {mode === "signup" && (
            <m.div
              key="name"
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
            >
              <label className="block text-xs text-muted-foreground mb-1.5 ml-1">
                Display name
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/40" />
                <input
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Anime fan"
                  className="w-full pl-10 pr-4 py-2.5 bg-input/60 border border-border/50 rounded-lg text-sm focus:ring-2 focus:ring-primary/40 focus:border-primary/40 transition-all outline-none"
                />
              </div>
            </m.div>
          )}
        </AnimatePresence>

        <div>
          <label className="block text-xs text-muted-foreground mb-1.5 ml-1">Email</label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/40" />
            <input
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full pl-10 pr-4 py-2.5 bg-input/60 border border-border/50 rounded-lg text-sm focus:ring-2 focus:ring-primary/40 focus:border-primary/40 transition-all outline-none"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs text-muted-foreground mb-1.5 ml-1">Password</label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/40" />
            <input
              type={showPass ? "text" : "password"}
              required
              autoComplete={mode === "signup" ? "new-password" : "current-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full pl-10 pr-10 py-2.5 bg-input/60 border border-border/50 rounded-lg text-sm focus:ring-2 focus:ring-primary/40 focus:border-primary/40 transition-all outline-none"
            />
            <button
              type="button"
              onClick={() => setShowPass(!showPass)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground/40 hover:text-muted-foreground transition-colors"
            >
              {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <AnimatePresence>
          {err && (
            <m.p
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg px-3 py-2"
            >
              {err}
            </m.p>
          )}
        </AnimatePresence>

        <m.div whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }}>
          <Button type="submit" disabled={busy} className="w-full h-11 text-base">
            {busy ? (
              <m.span
                animate={{ opacity: [1, 0.5, 1] }}
                transition={{ duration: 1, repeat: Infinity }}
              >
                Processing...
              </m.span>
            ) : mode === "signup" ? (
              <>
                Create account <ArrowRight className="w-4 h-4 ml-1" />
              </>
            ) : (
              <>
                Sign in <ArrowRight className="w-4 h-4 ml-1" />
              </>
            )}
          </Button>
        </m.div>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        {mode === "signup" ? (
          <>
            Already have an account?{" "}
            <button
              onClick={() => {
                setErr(null);
                setMode("signin");
              }}
              className="text-primary hover:underline font-medium"
            >
              Sign in
            </button>
          </>
        ) : (
          <>
            New to KuroStream?{" "}
            <button
              onClick={() => {
                setErr(null);
                setMode("signup");
              }}
              className="text-primary hover:underline font-medium"
            >
              Create account
            </button>
          </>
        )}
      </p>

      <p className="text-[11px] text-muted-foreground/60 text-center mt-6">
        By continuing you agree to our{" "}
        <Link to="/legal" className="hover:text-foreground underline">
          terms
        </Link>
        .
      </p>
    </m.div>
  );
}
