import { createFileRoute } from "@tanstack/react-router";

const BASE = "https://kurostream.tv";
const STATIC = [
  "",
  "marketplace",
  "platforms",
  "docs",
  "download",
  "features",
  "setup",
  "blog",
  "contribute",
  "legal",
];

export const Route = createFileRoute("/sitemap.xml")({
  server: {
    handlers: {
      GET: async () => {
        const items: { loc: string; lastmod: string; changefreq: string; priority: string }[] =
          STATIC.map((path) => ({
            loc: `${BASE}/${path}`,
            lastmod: new Date().toISOString().slice(0, 10),
            changefreq: "weekly",
            priority: path === "" ? "1.0" : "0.7",
          }));

        const xml = `${'<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'}${items
          .map(
            (i) =>
              `  <url>\n    <loc>${i.loc}</loc>\n    <lastmod>${i.lastmod}</lastmod>\n    <changefreq>${i.changefreq}</changefreq>\n    <priority>${i.priority}</priority>\n  </url>`,
          )
          .join("\n")}
</urlset>`;

        return new Response(xml, {
          headers: { "content-type": "application/xml; charset=utf-8" },
        });
      },
    },
  },
});
