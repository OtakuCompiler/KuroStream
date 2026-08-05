import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { User as UserIcon, LogOut, Receipt, Package, Shield, Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useUser, useLibrary } from "@/lib/marketplace";
import { m, AnimatePresence } from "framer-motion";

export const Route = createFileRoute("/account")({
  head: () => ({
    meta: [
      { title: "Account — Kuro Stream" },
      { name: "description", content: "Manage your KuroStream profile, purchases, and library." },
    ],
  }),
  component: AccountPage,
});

function AccountPage() {
  const { user, loading, signOut } = useUser();
  const { purchases } = useLibrary();
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState("");
  const [saved, setSaved] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    setDisplayName(user.name);
  }, [user]);

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl px-5 py-20">
        <div className="h-8 w-48 animate-shimmer rounded mb-4" />
        <div className="h-40 animate-shimmer rounded-xl" />
      </div>
    );
  }

  if (!user) {
    return (
      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mx-auto max-w-md px-5 py-20 text-center"
      >
        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
          <UserIcon className="w-8 h-8 text-primary" />
        </div>
        <h1 className="font-display text-2xl font-bold">Sign in to your account</h1>
        <p className="text-sm text-muted-foreground mt-2">
          Access your library, purchases, and skins.
        </p>
        <Button asChild className="mt-6">
          <Link to="/auth">Sign in</Link>
        </Button>
      </m.div>
    );
  }

  const saveName = async () => {
    setErr(null);
    setSaved(true);
    setTimeout(() => setSaved(false), 1500);
  };

  const doSignOut = async () => {
    await signOut();
    navigate({ to: "/" });
  };

  return (
    <m.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
      className="mx-auto max-w-3xl px-5 py-12"
    >
      <h1 className="font-display text-4xl font-bold text-gradient mb-8">Account</h1>

      <div className="grid md:grid-cols-3 gap-5 mb-8">
        <m.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="glass-card p-5"
        >
          <Package className="w-5 h-5 text-primary mb-3" />
          <div className="font-display text-2xl font-bold">{purchases.length}</div>
          <div className="text-xs text-muted-foreground">Items owned</div>
        </m.div>
        <m.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="glass-card p-5"
        >
          <Shield className="w-5 h-5 text-emerald-400 mb-3" />
          <div className="font-display text-2xl font-bold">Active</div>
          <div className="text-xs text-muted-foreground">Account status</div>
        </m.div>
        <m.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="glass-card p-5"
        >
          <Download className="w-5 h-5 text-secondary mb-3" />
          <div className="font-display text-2xl font-bold">1</div>
          <div className="text-xs text-muted-foreground">Device synced</div>
        </m.div>
      </div>

      <m.section
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="glass-card p-6 mb-5"
      >
        <h2 className="font-display text-lg font-semibold mb-4">Profile</h2>
        <div className="space-y-4">
          <div>
            <label className="text-xs text-muted-foreground uppercase tracking-wide">
              Display name
            </label>
            <input
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full mt-1.5 bg-input/60 border border-border/50 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-primary/40 focus:border-primary/40 transition-all outline-none"
            />
          </div>
          <div>
            <label className="text-xs text-muted-foreground uppercase tracking-wide">Email</label>
            <div className="mt-1.5 text-sm text-muted-foreground bg-white/5 rounded-lg px-3 py-2.5">
              {user.email}
            </div>
          </div>
          <div className="flex gap-2 pt-1">
            <Button onClick={saveName} size="sm">
              Save
            </Button>
            <AnimatePresence>
              {saved && (
                <m.span
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0 }}
                  className="text-xs text-emerald-400 self-center"
                >
                  Saved ✓
                </m.span>
              )}
            </AnimatePresence>
            {err && <span className="text-xs text-destructive self-center">{err}</span>}
          </div>
        </div>
      </m.section>

      <m.section
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="glass-card p-6 mb-5"
      >
        <h2 className="font-display text-lg font-semibold flex items-center gap-2 mb-4">
          <Receipt className="w-4 h-4 text-primary" /> Purchase history
        </h2>
        {purchases.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No purchases yet. Browse the{" "}
            <Link to="/marketplace" className="text-primary hover:underline">
              marketplace
            </Link>
            .
          </p>
        ) : (
          <ul className="divide-y divide-border/20">
            {purchases.map((p) => (
              <li key={p.id} className="flex justify-between items-center py-3 text-sm">
                <span className="font-medium">{p.item_id}</span>
                <span className="text-muted-foreground text-xs">
                  {new Date(p.created_at).toLocaleDateString()} · ${p.amount.toFixed(2)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </m.section>

      <m.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="flex gap-3"
      >
        <Button asChild variant="secondary" size="sm">
          <Link to="/library">Open library</Link>
        </Button>
        <Button
          onClick={doSignOut}
          variant="ghost"
          size="sm"
          className="text-muted-foreground hover:text-destructive transition-colors"
        >
          <LogOut className="w-4 h-4 mr-1" /> Sign out
        </Button>
      </m.div>
    </m.div>
  );
}
