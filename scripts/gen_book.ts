#!/usr/bin/env bun
/**
 * Converts the root wiki docs (MODIFIERS.md, LOOT.md, ...) into a MarkMeDown
 * guide book at data/randomloot/markmedown/book/.
 *
 * Run after regenerating the wiki docs:  bun scripts/gen_book.ts
 *
 * MarkMeDown constraints handled here:
 *  - images must be `item:<id>` icons or bundled textures, so remote images
 *    (screenshots, badges) are dropped; crafting-item images become item icons
 *  - internal links target `section` or `section/page` (no #heading anchors),
 *    so MODIFIERS.md#trait links point at the trait's category page
 */

import { copyFileSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = join(import.meta.dir, "..");
const OUT = join(ROOT, "src/main/resources/data/randomloot/markmedown/book");
const ASSETS = join(ROOT, ".github/assets");
const TEX_OUT = join(ROOT, "src/main/resources/assets/randomloot/textures/book");
const TEX_MAX_WIDTH = 640; // downscale bundled screenshots to keep the jar small
const DISPLAY_MAX = { w: 200, h: 115 }; // fit MarkMeDown's 208x128 page

const SECTIONS = [
  { file: "LOOT.md", id: "loot", title: "Loot & Crafting", icon: "randomloot:case", index: 1 },
  { file: "MODIFIERS.md", id: "modifiers", title: "Modifiers", icon: "minecraft:smithing_table", index: 2, split: true },
  { file: "BIOMES.md", id: "biomes", title: "Biome Traits", icon: "minecraft:prismarine_shard", index: 3 },
  { file: "PROGRESSION.md", id: "progression", title: "Progression", icon: "minecraft:experience_bottle", index: 4 },
  { file: "NAMES.md", id: "names", title: "Name Generation", icon: "minecraft:name_tag", index: 5 },
  { file: "CONFIG.md", id: "config", title: "Configuration", icon: "minecraft:comparator", index: 6 },
];

const warnings: string[] = [];
const warn = (msg: string) => warnings.push(msg);

const read = (file: string) => readFileSync(join(ROOT, file), "utf8");

/** Matches GenWiki's GitHub-style anchors: lowercase, spaces to dashes. */
const anchorSlug = (heading: string) => heading.trim().toLowerCase().replace(/\s+/g, "-");

/** Resource-location-safe page/file id. */
const fileSlug = (heading: string) => heading.trim().toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "");

/** Split a doc into { intro, chunks } on `## ` headings. */
function splitByH2(body: string) {
  const lines = body.split("\n");
  const chunks: { heading: string; lines: string[] }[] = [];
  const intro: string[] = [];
  let current: { heading: string; lines: string[] } | null = null;
  for (const line of lines) {
    const m = line.match(/^## (.+)$/);
    if (m) {
      current = { heading: m[1].trim(), lines: [] };
      chunks.push(current);
    } else if (current) {
      current.lines.push(line);
    } else {
      intro.push(line);
    }
  }
  return { intro: intro.join("\n"), chunks };
}

/** Drop the leading `# Title` line of a doc; return the rest. */
function stripH1(text: string) {
  return text.replace(/^# .+\n/, "");
}

// ---------------------------------------------------------------------------
// Anchor map: MODIFIERS.md trait heading -> modifiers/<category page>
// ---------------------------------------------------------------------------

const modifiersDoc = stripH1(read("MODIFIERS.md"));
const modifiersSplit = splitByH2(modifiersDoc);
const traitAnchors = new Map<string, string>(); // anchor slug -> "modifiers/<page>"
for (const chunk of modifiersSplit.chunks) {
  const page = `modifiers/${fileSlug(chunk.heading)}`;
  for (const line of chunk.lines) {
    const m = line.match(/^### (.+)$/);
    if (m) traitAnchors.set(anchorSlug(m[1]), page);
  }
}

// ---------------------------------------------------------------------------
// Markdown transforms
// ---------------------------------------------------------------------------

const DOC_TO_SECTION: Record<string, string> = { "README.md": "" };
for (const s of SECTIONS) DOC_TO_SECTION[s.file] = s.id;

function internalTarget(doc: string, anchor: string | undefined, sourceFile: string): string | null {
  const section = DOC_TO_SECTION[doc];
  if (section === undefined) return null;
  if (anchor) {
    const mapped = doc === "MODIFIERS.md" && traitAnchors.get(anchor.toLowerCase());
    if (mapped) return mapped;
    warn(`${sourceFile}: no page for anchor ${doc}#${anchor}; linking to section '${section}'`);
  }
  return section;
}

function rewriteLinks(text: string, sourceFile: string): string {
  // GitHub blob URLs to our own docs -> internal links
  text = text.replace(
    /\]\(https:\/\/github\.com\/TheMarstonConnell\/randomloot\/blob\/[^/)]+\/([A-Z_]+\.md)(#[^)]*)?\)/g,
    (whole, doc, hash) => {
      const target = internalTarget(doc, hash?.slice(1), sourceFile);
      return target === null ? whole : `](${target})`;
    },
  );
  // Relative links between docs -> internal links
  text = text.replace(/\]\(([A-Z_]+\.md)(#[^)]*)?\)/g, (whole, doc, hash) => {
    const target = internalTarget(doc, hash?.slice(1), sourceFile);
    return target === null ? whole : `](${target})`;
  });
  // "[CONFIG.md](config)" reads poorly in-game -> use the section title.
  for (const s of SECTIONS) {
    text = text.replaceAll(`[${s.file}](${s.id})`, `[${s.title}](${s.id})`);
  }
  // Links whose text was a dropped badge image are now empty: remove them.
  text = text.replace(/ ?\[\]\([^)]*\)/g, "");
  return text;
}

/** Reads width/height from a PNG's IHDR chunk. */
function pngSize(path: string): { w: number; h: number } {
  const buf = readFileSync(path);
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}

/**
 * Copies a README screenshot into the mod's textures (downscaled via sips when
 * available) and returns its MarkMeDown image line, or null if the source
 * image is missing locally.
 */
const copiedTextures = new Map<string, string>();
function bundleTexture(alt: string, name: string): string | null {
  let line = copiedTextures.get(name);
  if (!line) {
    const src = join(ASSETS, `${name}.png`);
    if (!existsSync(src)) return null;
    const dest = join(TEX_OUT, `${name}.png`);
    mkdirSync(TEX_OUT, { recursive: true });
    if (pngSize(src).w > TEX_MAX_WIDTH) {
      const sips = Bun.spawnSync(["sips", "--resampleWidth", String(TEX_MAX_WIDTH), src, "--out", dest]);
      if (!sips.success) {
        warn(`sips failed for ${name}.png; copying full-size`);
        copyFileSync(src, dest);
      }
    } else {
      copyFileSync(src, dest);
    }
    const { w, h } = pngSize(dest);
    const scale = Math.min(DISPLAY_MAX.w / w, DISPLAY_MAX.h / h, 1);
    line = `randomloot:textures/book/${name}.png ${Math.round(w * scale)}x${Math.round(h * scale)}`;
    copiedTextures.set(name, line);
    console.log(`  textures/book/${name}.png`);
  }
  return `![${alt}](${line})`;
}

function rewriteImages(text: string, sourceFile: string): string {
  const out: string[] = [];
  for (let line of text.split("\n")) {
    // Crafting-item sprites from the minecraft-api CDN -> inline item icons.
    line = line.replace(
      /!\[([^\]]*)\]\(https:\/\/raw\.githubusercontent\.com\/anish-shanbhag\/minecraft-api\/[^)]*\/items\/([a-z0-9_]+)\.png\)/g,
      (_whole, alt, item) => `![${alt}](item:minecraft:${item})`,
    );
    // README screenshots exist locally in .github/assets: bundle them as
    // textures and reference those instead of the remote URL.
    line = line.replace(
      /!\[([^\]]*)\]\(https:\/\/raw\.githubusercontent\.com\/TheMarstonConnell\/randomloot\/[^)]*\/\.github\/assets\/([a-z0-9_]+)\.png\)/g,
      (whole, alt, name) => {
        const bundled = bundleTexture(alt, name);
        if (bundled === null) {
          warn(`${sourceFile}: .github/assets/${name}.png not found locally; dropped image`);
          return "";
        }
        return bundled;
      },
    );
    // Any other remote image can't be rendered in-game: drop it.
    line = line.replace(/!\[([^\]]*)\]\((https?:[^)\s]+)[^)]*\)/g, (_whole, alt, url) => {
      warn(`${sourceFile}: dropped remote image '${alt}' (${url})`);
      return "";
    });
    out.push(line);
  }
  // Collapse the 3+ blank lines that dropped images can leave behind.
  return out.join("\n").replace(/\n{3,}/g, "\n\n");
}

const transform = (text: string, sourceFile: string) =>
  rewriteLinks(rewriteImages(text, sourceFile), sourceFile).trim() + "\n";

function frontmatter(fields: Record<string, string | number | boolean>) {
  const lines = Object.entries(fields).map(([k, v]) =>
    typeof v === "string" ? `${k}: ${v.includes(":") ? JSON.stringify(v) : v}` : `${k}: ${v}`,
  );
  return `---\n${lines.join("\n")}\n---\n`;
}

function writePage(relPath: string, fm: Record<string, string | number | boolean>, body: string) {
  const path = join(OUT, relPath);
  mkdirSync(join(path, ".."), { recursive: true });
  writeFileSync(path, frontmatter(fm) + body);
  console.log(`  ${relPath}`);
}

// ---------------------------------------------------------------------------
// Build the book
// ---------------------------------------------------------------------------

rmSync(OUT, { recursive: true, force: true });
rmSync(TEX_OUT, { recursive: true, force: true });
mkdirSync(OUT, { recursive: true });
console.log(`Writing book to ${OUT}`);

// Book root: README minus the Documentation section (the book appends its own
// table of contents) and minus badges/screenshots.
let readme = stripH1(read("README.md"));
const readmeSplit = splitByH2(readme);
readme = [
  readmeSplit.intro,
  ...readmeSplit.chunks
    .filter((c) => c.heading !== "Documentation")
    .map((c) => [`## ${c.heading}`, ...c.lines].join("\n")),
].join("\n");
writePage(
  "index.md",
  { title: "Random Loot", icon: "randomloot:case", give_on_first_join: true },
  transform(readme, "README.md"),
);

for (const section of SECTIONS) {
  const fm = { title: section.title, icon: section.icon, index: section.index };
  if (!section.split) {
    writePage(`${section.id}/index.md`, fm, transform(stripH1(read(section.file)), section.file));
    continue;
  }
  // MODIFIERS.md: intro becomes the section page, each ## category a page.
  writePage(`${section.id}/index.md`, fm, transform(modifiersSplit.intro, section.file));
  modifiersSplit.chunks.forEach((chunk, i) => {
    writePage(
      `${section.id}/${fileSlug(chunk.heading)}.md`,
      { title: chunk.heading.replace(/\.$/, ""), index: i + 1 },
      transform(chunk.lines.join("\n"), section.file),
    );
  });
}

if (warnings.length) {
  console.log(`\n${warnings.length} warning(s):`);
  for (const w of warnings) console.log(`  - ${w}`);
}
