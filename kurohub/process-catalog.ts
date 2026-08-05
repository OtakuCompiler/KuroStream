// Script to process extracted skins and extensions from Download.zip
// Filters out pirated content and renames copyrighted skins to "inspired by" versions

import { readFileSync, readdirSync, existsSync } from "fs";
import { join } from "path";

const EXTRACT_DIR = "/tmp/kuro_extract";
const CATALOG_PATH = join(EXTRACT_DIR, "catalog.json");

interface CatalogExtension {
  id: string;
  package_id: string;
  name: string;
  version: string;
  description: string;
  category: string;
  type: string;
  format: string;
  price: string;
  tier: string;
  rating: number;
  icon_url: string;
  manifest_path: string;
  config_path: string;
  features: string[];
}

interface CatalogSkin {
  id: string;
  name: string;
  version: string;
  description: string;
  inspired_by: string;
  price: string;
  tier: string;
  preview_url: string;
  theme_path: string;
}

interface Catalog {
  catalog_version: string;
  target_app: string;
  min_app_version: string;
  extensions: CatalogExtension[];
  skins: CatalogSkin[];
}

// Copyrighted character/franchise mapping for renaming
const COPYRIGHT_MAPPING: Record<string, { newName: string; newDescription: string }> = {
  "shadow-shinobi": {
    newName: "Shadow Shinobi (Inspired)",
    newDescription: "Black and orange ninja atmosphere with chakra energy effects and smoke particles. Dark cinematic UI inspired by hidden village aesthetics from popular anime."
  },
  "eternal-rival": {
    newName: "Eternal Rival (Inspired)",
    newDescription: "Black, purple and crimson with lightning effects and dark warrior aesthetic. Premium glass cards with electric accents inspired by rival ninja characters."
  },
  "pirate-emperor": {
    newName: "Pirate Captain (Inspired)",
    newDescription: "Red and gold with ocean effects and adventure feeling. Energetic animations inspired by pirate anime protagonists."
  },
  "hollow-eclipse": {
    newName: "Hollow Eclipse (Inspired)",
    newDescription: "Black and red with hollow energy effects and sharp cinematic transitions. Dark combat style UI inspired by soul reaper anime."
  },
  "infinity-void": {
    newName: "Infinity Void (Inspired)",
    newDescription: "Blue and white with space/infinity visuals and neon glow. Futuristic glass interface with infinite depth inspired by strongest sorcerer characters."
  },
  "crimson-control": {
    newName: "Crimson Control (Inspired)",
    newDescription: "Dark red luxury appearance with minimal elegant UI and gold highlights. Premium control aesthetic inspired by dominant anime antagonists."
  },
  "explosive-rose": {
    newName: "Explosive Rose (Inspired)",
    newDescription: "Pink and red with dynamic particles and energetic animations. Stylish cinematic look with explosive energy inspired by bomb devil characters."
  },
  "king-of-curses": {
    newName: "King of Curses (Inspired)",
    newDescription: "Black and crimson with ancient patterns and fire/cursed energy effects. Powerful dark UI inspired by king of curses characters."
  },
  "sun-breathing": {
    newName: "Sun Breathing (Inspired)",
    newDescription: "Red and black with Japanese traditional patterns, fire and water effects. Warm cinematic visuals inspired by sun breathing technique users."
  },
  "eternal-mangekyo": {
    newName: "Eternal Mangekyo (Inspired)",
    newDescription: "Black and crimson with legendary warrior feeling and meteor/space effects. Premium dark mode inspired by eternal mangekyo sharingan wielders."
  },
  "perfect-illusion": {
    newName: "Perfect Illusion (Inspired)",
    newDescription: "White, purple and gold with elegant luxury design and illusion-like animations. High-class interface inspired by master illusionist characters."
  },
};

const PIRATED_EXTENSIONS = new Set([
  "torrentstream-pro", // Torrent/magnet/P2P functionality
]);

const COPYRIGHTED_EXTENSIONS = new Set([
  "animestream-ultimate", // Anime streaming from unlicensed sources
  "cinemastream-pro", // Movie streaming from unlicensed sources
  "streamvault-premium", // Multi-source aggregation without named API
  "global-stream-hub", // International streaming aggregator
  "sports-hub", // Live sports from unlicensed sources
  "news-hub", // News aggregation from unspecified sources
  "comedy-central", // Trademarked brand name
  "basic-stream-source", // Generic unlicensed streaming
]);

function parsePrice(priceStr: string): number {
  if (priceStr.toLowerCase() === "free") return 0;
  const match = priceStr.match(/[\d.]+/);
  return match ? parseFloat(match[0]) : 0;
}

function readThemeJson(skinDir: string): any {
  const themePath = join(EXTRACT_DIR, "skins", skinDir, "theme.json");
  if (existsSync(themePath)) {
    return JSON.parse(readFileSync(themePath, "utf-8"));
  }
  return null;
}

function generatePalette(theme: any): string {
  if (!theme) return '{"primary":"#BB86FC","secondary":"#03DAC6","bg":"#121212","accent":"#CF6679"}';
  const colors = theme.colors || {};
  return JSON.stringify({
    primary: colors.primary || "#BB86FC",
    secondary: colors.secondary || "#03DAC6",
    bg: colors.background || "#121212",
    accent: colors.accent || "#CF6679",
  });
}

function generateParticle(theme: any): string | null {
  if (!theme) return null;
  if (theme.particles) return JSON.stringify(theme.particles);
  if (theme.effects) return JSON.stringify(theme.effects);
  return null;
}

function processCatalog() {
  const catalog: Catalog = JSON.parse(readFileSync(CATALOG_PATH, "utf-8"));
  
  console.log("=== EXTENSIONS ===");
  const safeExtensions: CatalogExtension[] = [];
  
  for (const ext of catalog.extensions) {
    if (PIRATED_EXTENSIONS.has(ext.package_id)) {
      console.log(`❌ EXCLUDED (piracy): ${ext.name} (${ext.package_id})`);
      continue;
    }
    if (COPYRIGHTED_EXTENSIONS.has(ext.package_id)) {
      console.log(`❌ EXCLUDED (copyright/unlicensed): ${ext.name} (${ext.package_id})`);
      continue;
    }
    safeExtensions.push(ext);
    console.log(`✅ SAFE: ${ext.name} (${ext.package_id}) - ${ext.price}`);
  }
  
  console.log(`\nTotal safe extensions: ${safeExtensions.length}/${catalog.extensions.length}`);
  
  console.log("\n=== SKINS ===");
  const safeSkins: CatalogSkin[] = [];
  
  for (const skin of catalog.skins) {
    const mapping = COPYRIGHT_MAPPING[skin.id];
    if (mapping) {
      console.log(`🔄 RENAMED: ${skin.name} -> ${mapping.newName}`);
      safeSkins.push({ ...skin, name: mapping.newName, description: mapping.newDescription });
    } else {
      console.log(`✅ SAFE: ${skin.name} (${skin.id}) - ${skin.price}`);
      safeSkins.push(skin);
    }
  }
  
  console.log(`\nTotal safe skins: ${safeSkins.length}/${catalog.skins.length}`);
  
  // Generate SQL inserts
  console.log("\n=== SQL INSERTS FOR marketplace_items ===");
  
  for (const ext of safeExtensions) {
    const price = parsePrice(ext.price);
    const isPremium = price > 0 ? 1 : 0;
    const category = ext.type === "SOURCE" ? "addon" : ext.type.toLowerCase();
    const emoji = getEmojiForCategory(ext.category);
    
    console.log(`('${ext.package_id}', '${escapeSql(ext.name)}', 'KuroStream Labs', '${escapeSql(ext.description)}', '${escapeSql(ext.description)}', '${category}', ${price}, ${ext.rating}, 0, '${emoji}', NULL, NULL, ${isPremium}, '#', '[]'),`);
  }
  
  for (const skin of safeSkins) {
    const theme = readThemeJson(skin.id);
    const palette = generatePalette(theme);
    const particle = generateParticle(theme);
    const price = parsePrice(skin.price);
    const isPremium = price > 0 ? 1 : 0;
    const emoji = getEmojiForSkin(skin.id);
    
    console.log(`('${skin.id}', '${escapeSql(skin.name)}', 'KuroStream Studio', '${escapeSql(skin.description)}', '${escapeSql(skin.description)}', 'skin', ${price}, 4.5, 0, '${emoji}', '${palette}', ${particle ? `'${particle}'` : 'NULL'}, ${isPremium}, '#', '[]'),`);
  }
}

function getEmojiForCategory(category: string): string {
  const map: Record<string, string> = {
    "Streaming": "📺",
    "Movies & Series": "🎬",
    "Anime": "🎌",
    "Torrent Streaming": "🧲",
    "International": "🌍",
    "Utility": "🔧",
    "Subtitles": "💬",
    "Sync": "🔄",
    "Library": "📚",
    "Parental Control": "🔒",
    "Sports": "⚽",
    "Cloud": "☁️",
    "UI Enhancement": "✨",
    "Audio": "🔊",
    "Social": "🎉",
    "Download": "⬇️",
    "AI/ML": "🧠",
    "Retro/Archive": "🎞️",
    "Developer Tools": "🛠️",
    "Local Playback": "📁",
    "YouTube": "▶️",
    "Audio": "📻",
    "News": "📰",
    "Documentary": "🌍",
    "Comedy": "😂",
    "Fitness": "🧘",
    "Metadata": "🧠",
  };
  return map[category] || "🔧";
}

function getEmojiForSkin(skinId: string): string {
  const map: Record<string, string> = {
    "crimson-control": "🔴",
    "crystal-clear": "💎",
    "eternal-mangekyo": "👁️",
    "eternal-rival": "⚡",
    "explosive-rose": "🌹",
    "forest-green": "🌲",
    "hollow-eclipse": "🌑",
    "infinity-void": "🌌",
    "king-of-curses": "👑",
    "midnight-blue": "🌌",
    "neon-cyberpunk": "🌆",
    "perfect-illusion": "✨",
    "pirate-emperor": "🏴‍☠️",
    "shadow-shinobi": "🥷",
    "sun-breathing": "☀️",
    "sunset-orange": "🌇",
  };
  return map[skinId] || "🎨";
}

function escapeSql(str: string): string {
  return str.replace(/'/g, "''");
}

processCatalog();