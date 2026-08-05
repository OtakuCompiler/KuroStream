// Content Safety Pipeline - Auto-reject and review logic
// This runs server-side on submission to block policy violations

export interface SubmissionInput {
  id: string;
  name: string;
  author: string;
  description: string;
  long_description?: string;
  category: string;
  price: number;
  manifest?: Record<string, unknown>;
  screenshots?: string[];
  legal_basis: string;
  aup_accepted: boolean;
}

export interface AutoRejectResult {
  rejected: boolean;
  reasons: string[];
  flags: string[];
}

const PIRACY_KEYWORDS = [
  "torrent",
  "magnet",
  "p2p",
  "dht",
  "peer-to-peer",
  "multi-source aggregation",
  "various sources",
  "unlicensed",
  "unauthorized",
  "cracked",
  "pirated",
  "warez",
];

const COPYRIGHT_KEYWORDS = [
  "naruto",
  "sasuke",
  "one piece",
  "luffy",
  "zoro",
  "nami",
  "bleach",
  "ichigo",
  "rukia",
  "jujutsu kaisen",
  "gojo",
  "sukuna",
  "demon slayer",
  "tanjiro",
  "nezuko",
  "attack on titan",
  "eren",
  "mikasa",
  "my hero academia",
  "deku",
  "bakugo",
  "chainsaw man",
  "denji",
  "makima",
  "spy x family",
  "anya",
  "yor",
  "loyde",
  "comedy central",
  "netflix",
  "hulu",
  "disney",
  "crunchyroll",
  "funimation",
  "hbo",
  "max",
  "paramount",
  "peacock",
  "pokemon",
  "digimon",
  "yu-gi-oh",
  "dragon ball",
  "goku",
  "vegeta",
  "sailor moon",
  "evangelion",
  "shinji",
  "asuka",
  "rei",
  "fullmetal alchemist",
  "edward elric",
  "alphonse",
  "death note",
  "light yagami",
  "l lawliet",
  "hunter x hunter",
  "gon",
  "killua",
  "fairy tail",
  "natsu",
  "lucy",
  "tokyo ghoul",
  "kaneki",
  "one punch man",
  "saitama",
  "mob psycho",
  "reigen",
  "overlord",
  "ainz",
  "rezero",
  "subaru",
  "rem",
  "emilia",
  "konosuba",
  "kazuma",
  "aqua",
  "violet evergarden",
  "made in abyss",
  "riko",
  "reg",
  "cyberpunk edgerunners",
  "david martinez",
  "lucy",
  "chainsaw man",
  "denji",
  "power",
  "makima",
  "spy x family",
  "anya",
  "yor",
  "loyde",
  "blue lock",
  "isagi",
  "bachira",
  "jujutsu kaisen",
  "gojo",
  "geto",
  "megumi",
  "nobara",
  "demon slayer",
  "tanjiro",
  "nezuko",
  "zenitsu",
  "inosuke",
  "attack on titan",
  "eren",
  "mikasa",
  "levi",
  "armin",
  "my hero academia",
  "deku",
  "bakugo",
  "todoroki",
  "uraka",
  "one piece",
  "luffy",
  "zoro",
  "nami",
  "sanji",
  "chopper",
  "naruto",
  "sasuke",
  "sakura",
  "kakashi",
  "bleach",
  "ichigo",
  "rukia",
  "byakuya",
  "dragon ball",
  "goku",
  "vegeta",
  "piccolo",
  "frieza",
  "sailor moon",
  "usagi",
  "mamoru",
  "evangelion",
  "shinji",
  "asuka",
  "rei",
  "misato",
  "fullmetal alchemist",
  "edward",
  "alphonse",
  "roy mustang",
  "death note",
  "light",
  "l",
  "misa",
  "hunter x hunter",
  "gon",
  "killua",
  "kurapika",
  "leorio",
  "fairy tail",
  "natsu",
  "lucy",
  "gray",
  "erza",
  "tokyo ghoul",
  "kaneki",
  "touka",
  "one punch man",
  "saitama",
  "genos",
  "mob psycho",
  "mob",
  "reigen",
  "rits",
  "overlord",
  "ainz",
  "albedo",
  "rezero",
  "subaru",
  "rem",
  "emilia",
  "beatrice",
  "konosuba",
  "kazuma",
  "aqua",
  "megumin",
  "darkness",
  "violet evergarden",
  "violet",
  "gilbert",
  "made in abyss",
  "riko",
  "reg",
  "nanachi",
  "cyberpunk",
  "edgerunners",
  "david",
  "lucy",
  "rebecca",
];

const EXCESSIVE_PERMISSION_KEYWORDS = [
  "filesystem",
  "file_system",
  "full filesystem",
  "network proxy",
  "network_proxy",
  "proxy",
  "contacts",
  "location",
  "precise_location",
  "media_library",
  "full_media",
  "full_library",
  "camera",
  "microphone",
  "geolocation",
];

function normalizeText(text: string): string {
  return text.toLowerCase().replace(/[^a-z0-9\s]/g, " ");
}

function checkKeywords(text: string, keywords: string[]): string[] {
  const normalized = normalizeText(text);
  const matches: string[] = [];
  for (const keyword of keywords) {
    if (normalized.includes(keyword.toLowerCase())) {
      matches.push(keyword);
    }
  }
  return matches;
}

function checkManifestPermissions(manifest: Record<string, unknown> | undefined): string[] {
  if (!manifest) return [];
  const flags: string[] = [];
  const perms = manifest.permissions || manifest.scopes || manifest.required_permissions || [];
  if (Array.isArray(perms)) {
    for (const perm of perms) {
      const permStr = String(perm).toLowerCase();
      for (const keyword of EXCESSIVE_PERMISSION_KEYWORDS) {
        if (permStr.includes(keyword.toLowerCase())) {
          flags.push(`excessive_permission:${permStr}`);
        }
      }
    }
  }
  return flags;
}

export function autoScreenSubmission(input: SubmissionInput): AutoRejectResult {
  const reasons: string[] = [];
  const flags: string[] = [];

  const searchableText = `${input.name} ${input.description} ${input.long_description || ""} ${input.legal_basis}`;

  // 1. Piracy / torrent / P2P detection
  const piracyMatches = checkKeywords(searchableText, PIRACY_KEYWORDS);
  if (piracyMatches.length > 0) {
    reasons.push(`Piracy-related content detected: ${piracyMatches.join(", ")}`);
    flags.push(...piracyMatches.map((m) => `piracy:${m}`));
  }

  // 2. Copyright/trademark references in name/description
  const copyrightMatches = checkKeywords(searchableText, COPYRIGHT_KEYWORDS);
  if (copyrightMatches.length > 0) {
    reasons.push(
      `Potential copyrighted character/franchise references: ${copyrightMatches.join(", ")}`,
    );
    flags.push(...copyrightMatches.map((m) => `copyright:${m}`));
  }

  // 3. Missing or empty legal_basis
  if (!input.legal_basis || input.legal_basis.trim().length < 10) {
    reasons.push("Legal basis for content source is missing or too short (minimum 10 characters)");
    flags.push("missing_legal_basis");
  }

  // 4. Missing AUP acceptance
  if (!input.aup_accepted) {
    reasons.push("Acceptable Use Policy not accepted");
    flags.push("missing_aup_acceptance");
  }

  // 5. Excessive permissions in manifest
  const permFlags = checkManifestPermissions(input.manifest);
  if (permFlags.length > 0) {
    reasons.push(`Extension requests excessive permissions: ${permFlags.join(", ")}`);
    flags.push(...permFlags);
  }

  // 6. Category mismatch for streaming sources claiming to be free/legit without named API
  if (input.category === "source" || input.category === "streaming" || input.category === "addon") {
    const hasNamedAPI =
      /(official|api|tmdb|themoviedb|youtube|google|drive|dropbox|onedrive|radio|opensubtitles|subdl|anilist|mal|trakt|tvdb|imdb|musicbrainz|last\.fm|audioscrobbler)/i.test(
        input.legal_basis,
      );
    const claimsAggregation = /(multi.source|aggregat|various source|multiple source)/i.test(
      searchableText,
    );
    if (claimsAggregation && !hasNamedAPI) {
      reasons.push("Claims multi-source aggregation without naming a licensed API in legal_basis");
      flags.push("unlicensed_aggregation");
    }
  }

  return {
    rejected: reasons.length > 0,
    reasons,
    flags,
  };
}

export function generatePermissionsList(manifest: Record<string, unknown> | undefined): string[] {
  if (!manifest) return ["No manifest provided"];
  const perms = manifest.permissions || manifest.scopes || manifest.required_permissions || [];
  if (!Array.isArray(perms) || perms.length === 0) return ["No permissions declared"];

  const permissionDescriptions: Record<string, string> = {
    network: "Access to internet/network",
    storage: "Read/write local storage",
    filesystem: "Full filesystem access",
    network_proxy: "Network proxy/VPN capabilities",
    contacts: "Access to contacts",
    location: "Access to device location",
    precise_location: "Access to precise GPS location",
    camera: "Access to camera",
    microphone: "Access to microphone",
    media_library: "Access to media library",
    full_media: "Full media library indexing",
    notifications: "Send notifications",
    background: "Run in background",
    startup: "Run at startup",
  };

  return perms.map((p: unknown) => {
    const permStr = String(p).toLowerCase();
    for (const [key, desc] of Object.entries(permissionDescriptions)) {
      if (permStr.includes(key)) return desc;
    }
    return `Custom permission: ${p}`;
  });
}
