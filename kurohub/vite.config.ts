import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import tailwindcss from "@tailwindcss/vite";
import tsConfigPaths from "vite-tsconfig-paths";
import { cloudflare } from "@cloudflare/vite-plugin";

export default defineConfig({
  plugins: [
    tanstackStart({}),
    react(),
    tailwindcss(),
    tsConfigPaths(),
    cloudflare({ viteEnvironment: { name: "ssr" } }),
  ],
  build: {
    target: "es2022",
    cssMinify: true,
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          firebase: ["firebase/app", "firebase/auth", "firebase/firestore", "firebase/storage"],
          framer: ["framer-motion"],
          radix: [
            "@radix-ui/react-dialog",
            "@radix-ui/react-dropdown-menu",
            "@radix-ui/react-tabs",
            "@radix-ui/react-tooltip",
          ],
          query: ["@tanstack/react-query"],
        },
      },
    },
  },
  ssr: {
    noExternal: ["@tanstack/react-start"],
  },
  optimizeDeps: {
    include: ["firebase/app", "firebase/auth", "framer-motion"],
  },
});
