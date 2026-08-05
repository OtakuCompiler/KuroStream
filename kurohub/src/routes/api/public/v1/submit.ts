import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { json, preflight, requireUser, getDB, getKV, checkRateLimit } from "@/lib/kuro-api";
import { autoScreenSubmission } from "@/lib/content-safety";
import { insertMarketplaceItem } from "@/integrations/cloudflare/db";

const submitSchema = z.object({
  id: z.string().min(1).max(120),
  name: z.string().min(1).max(120),
  author: z.string().min(1).max(120),
  description: z.string().min(10).max(500),
  long_description: z.string().max(5000).optional(),
  category: z.enum(["skin", "pack", "addon", "pass"]),
  price: z.number().min(0).max(1000),
  manifest: z.record(z.unknown()).optional(),
  screenshots: z.array(z.string().url()).max(10).optional(),
  legal_basis: z.string().min(10).max(2000),
  aup_accepted: z.boolean(),
});

export const Route = createFileRoute("/api/public/v1/submit")({
  server: {
    handlers: {
      OPTIONS: async () => preflight(),
      POST: async ({ request, env }: any) => {
        const auth = await requireUser(request, env);
        if ("error" in auth) return auth.error;

        const kv = getKV(env);
        const rate = await checkRateLimit(kv, `submit:${auth.userId}`, 5, 3600);
        if (!rate.allowed) return json({ error: "rate_limited", reset: rate.reset }, 429);

        const db = getDB(env);
        const parsed = submitSchema.safeParse(await request.json().catch(() => null));
        if (!parsed.success)
          return json({ error: "invalid_body", details: parsed.error.flatten() }, 400);

        const input = parsed.data;
        const screening = autoScreenSubmission({ ...input, aup_accepted: input.aup_accepted } as import("@/lib/content-safety").SubmissionInput);

        const itemId = input.id;
        const now = new Date().toISOString();

        await db
          .prepare(
            `
          INSERT INTO submission_reviews (item_id, submitter_id, manifest, description, screenshots, legal_basis, aup_accepted, aup_accepted_at, status, auto_reject_reasons, created_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `,
          )
          .bind(
            itemId,
            auth.userId,
            JSON.stringify(input.manifest || {}),
            input.description,
            JSON.stringify(input.screenshots || []),
            input.legal_basis,
            input.aup_accepted ? 1 : 0,
            input.aup_accepted ? now : null,
            screening.rejected ? "auto_rejected" : "pending_review",
            JSON.stringify(screening.flags),
            now,
          )
          .run();

        if (screening.rejected) {
          return json(
            {
              error: "submission_rejected",
              reasons: screening.reasons,
              flags: screening.flags,
            },
            400,
          );
        }

        await insertMarketplaceItem(db, {
          id: itemId,
          name: input.name,
          author: input.author,
          description: input.description,
          long_description: input.long_description || "",
          category: input.category,
          price: input.price,
          rating: 0,
          installs: 0,
          emoji: null,
          palette: null,
          particle: null,
          is_premium: input.price > 0 ? 1 : 0,
          file_url: "#",
          screenshots: JSON.stringify(input.screenshots || []),
          status: "pending_review",
          submitter_id: auth.userId,
          legal_basis: input.legal_basis,
          reviewed_by: null,
          reviewed_at: null,
          rejection_reason: null,
          created_at: now,
          updated_at: now,
        });

        return json({
          ok: true,
          item_id: itemId,
          status: "pending_review",
          message: "Submission received and pending manual review",
        });
      },
    },
  },
});