import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { toast } from "sonner";

export const Route = createFileRoute("/admin")({
  component: AdminPage,
});

type ReviewRow = { item_id: string; created_at: string; rejection_reason?: string | null };
type ReportRow = {
  id: string;
  item_id: string;
  reason: string;
  details?: string | null;
  reporter_id: string;
  created_at: string;
};

function AdminPage() {
  const [data, setData] = useState<{ reviews: ReviewRow[]; reports: ReportRow[] } | null>(null);
  const [loading, setLoading] = useState(true);
  const [adminKey, setAdminKey] = useState("");

  useEffect(() => {
    if (!adminKey) return;
    Promise.all([
      fetch("/api/admin/v1/reviews", { headers: { "x-admin-key": adminKey } }).then((r) => r.json()),
      fetch("/api/admin/v1/reports?status=open", { headers: { "x-admin-key": adminKey } }).then((r) => r.json()),
    ])
      .then(([reviewsRes, reportsRes]) => {
        setData({ reviews: reviewsRes.reviews || [], reports: reportsRes.reports || [] });
        setLoading(false);
      })
      .catch(() => {
        setLoading(false);
      });
  }, [adminKey]);

  if (!adminKey) {
    return (
      <div className="max-w-2xl mx-auto mt-20 p-8">
        <h1 className="font-display text-2xl font-bold mb-4">Admin Access</h1>
        <input
          type="password"
          placeholder="Admin key"
          value={adminKey}
          onChange={(e) => setAdminKey(e.target.value)}
          className="w-full p-3 rounded border bg-background"
        />
      </div>
    );
  }

  if (loading) return <div className="p-8">Loading…</div>;
  if (!data) return <div className="p-8 text-red-500">Failed to load admin data.</div>;

  return (
    <div className="max-w-5xl mx-auto p-8 space-y-8">
      <h1 className="font-display text-3xl font-bold">Admin Dashboard</h1>

      <section className="space-y-4">
        <h2 className="font-display text-xl font-semibold">
          Pending Reviews ({data.reviews.length})
        </h2>
        {data.reviews.length === 0 ? (
          <p className="text-muted-foreground text-sm">No pending reviews.</p>
        ) : (
          <div className="space-y-3">
            {data.reviews.map((r: ReviewRow) => (
              <div key={r.item_id} className="glass-card p-4 flex items-center justify-between">
                <div>
                  <p className="font-semibold">{r.item_id}</p>
                  <p className="text-xs text-muted-foreground">
                    Submitted: {new Date(r.created_at).toLocaleString()}
                  </p>
                </div>
                <div className="flex gap-2">
                  <ApproveButton itemId={r.item_id} adminKey={adminKey} />
                  <RejectButton itemId={r.item_id} adminKey={adminKey} />
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-4">
        <h2 className="font-display text-xl font-semibold">Open Reports ({data.reports.length})</h2>
        {data.reports.length === 0 ? (
          <p className="text-muted-foreground text-sm">No open reports.</p>
        ) : (
          <div className="space-y-3">
            {data.reports.map((r: ReportRow) => (
              <div key={r.id} className="glass-card p-4">
                <p className="font-semibold">{r.item_id}</p>
                <p className="text-sm">{r.reason}</p>
                {r.details && <p className="text-xs text-muted-foreground mt-1">{r.details}</p>}
                <p className="text-xs text-muted-foreground mt-1">Reporter: {r.reporter_id}</p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function ApproveButton({ itemId, adminKey }: { itemId: string; adminKey: string }) {
  const [loading, setLoading] = useState(false);
  const act = async () => {
    setLoading(true);
    await fetch("/api/admin/v1/reviews-action", {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-admin-key": adminKey },
      body: JSON.stringify({ item_id: itemId, action: "approve" }),
    });
    toast.success("Approved");
    setLoading(false);
    window.location.reload();
  };
  return (
    <button
      onClick={act}
      disabled={loading}
      className="px-3 py-1 rounded bg-green-600 text-white text-sm disabled:opacity-50"
    >
      {loading ? "…" : "Approve"}
    </button>
  );
}

function RejectButton({ itemId, adminKey }: { itemId: string; adminKey: string }) {
  const [loading, setLoading] = useState(false);
  const [reason, setReason] = useState("");
  const act = async () => {
    if (!reason.trim()) {
      toast.error("Rejection reason required");
      return;
    }
    setLoading(true);
    await fetch("/api/admin/v1/reviews-action", {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-admin-key": adminKey },
      body: JSON.stringify({ item_id: itemId, action: "reject", rejection_reason: reason }),
    });
    toast.success("Rejected");
    setLoading(false);
    window.location.reload();
  };
  return (
    <div className="flex gap-1">
      <input
        type="text"
        placeholder="Reason"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        className="px-2 py-1 rounded border bg-background text-xs"
      />
      <button
        onClick={act}
        disabled={loading}
        className="px-3 py-1 rounded bg-red-600 text-white text-sm disabled:opacity-50"
      >
        {loading ? "…" : "Reject"}
      </button>
    </div>
  );
}