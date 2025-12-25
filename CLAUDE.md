# CLAUDE.md - Entwickler-Anleitung für mediawiki2docusaurus

## Projekt-Übersicht

Java-Tool zur Konvertierung eines MediaWiki zu Docusaurus-kompatiblem Markdown.

**Quelle:** https://support.xtento.com/  
**Ausgabe:** `out/` Ordner mit `wiki/`, `sidebars.js`, `docusaurus.config.js`

---

## Build & Export

```bash
cd /workspaces/mediawiki2docusaurus
mvn clean compile exec:java
```

Output landet in `out/`.

---

## Lokale Docusaurus-Testumgebung einrichten

Um Änderungen am Converter zu testen, ohne manuell deployen zu müssen:

### 1. Temporäres Docusaurus-Projekt erstellen

```bash
cd /tmp
npx create-docusaurus@latest docusaurus-test classic --javascript
cd docusaurus-test
npm install @docusaurus/plugin-client-redirects @easyops-cn/docusaurus-search-local
```

### 2. Konvertierte Dateien kopieren

```bash
# Wiki-Dateien kopieren
rm -rf /tmp/docusaurus-test/docs/*
cp -r /workspaces/mediawiki2docusaurus/out/wiki /tmp/docusaurus-test/docs/

# Sidebars und Config ersetzen
cp /workspaces/mediawiki2docusaurus/out/sidebars.js /tmp/docusaurus-test/
cp /workspaces/mediawiki2docusaurus/out/docusaurus.config.js /tmp/docusaurus-test/
```

### 3. Build testen

```bash
cd /tmp/docusaurus-test
npm run build
```

Bei Erfolg: `[SUCCESS] Generated static files in "build".`

### 4. Lokal anschauen (optional)

```bash
npm run serve
# Oder für Development mit Hot-Reload:
npm run start
```

---

## Logo einrichten

Das Logo wird in der Docusaurus-Config unter `themeConfig.navbar.logo` konfiguriert:

```javascript
logo: {
  alt: 'XTENTO Logo',
  src: 'img/logo.png',
},
```

**Logo-Datei ablegen:**
- Pfad: `static/img/logo.png`
- Empfohlene Größe: 32x32 oder 40x40 px (wird automatisch skaliert)
- Format: PNG mit Transparenz empfohlen

**Favicon:**
- Pfad: `static/img/favicon.ico`
- Die Config referenziert: `favicon: 'img/favicon.ico'`

---

## Typische Probleme & Lösungen

### MDX Kompilierungsfehler

**Problem:** `Could not parse expression with acorn`  
**Ursache:** Unescapte `{`, `}` oder `<` Zeichen außerhalb von Code-Blöcken  
**Lösung:** `MarkdownUtils.escapeMdxCharacters()` escapet diese als `\{`, `\}`, `\<`

### Fenced Code Block Detection Bug

**Problem:** 5 Backticks (`````code`````) werden fälschlich als Code-Block erkannt  
**Ursache:** `startsWith("```")` matcht auch 4+ Backticks  
**Lösung:** Prüfe auf EXAKT 3 Backticks + optional Sprach-Identifier:

```java
boolean isFenceMarker = trimmedLine.startsWith("```") 
    && !trimmedLine.startsWith("````")
    && (trimmedLine.equals("```") || trimmedLine.matches("```[a-zA-Z0-9_-]*\\s*"));
```

### Prism Language Error

**Problem:** `Cannot find module 'prism-react-renderer/prism/prism-xml'`  
**Ursache:** `additionalLanguages` in Prism-Config mit nicht-existierenden Modulen  
**Lösung:** `additionalLanguages` entfernen oder nur gültige Sprachen verwenden

### Broken Anchor Warnings

**Problem:** `Broken anchor on source page path = .../index.md: theass "#HTTP_Server" does not exist`  
**Info:** Das sind interne TOC-Links aus MediaWiki - meist harmlos, können ignoriert werden

---

## Schnell-Test nach Änderungen

```bash
# 1. Neu exportieren
cd /workspaces/mediawiki2docusaurus
mvn clean compile exec:java

# 2. Dateien aktualisieren
rm -rf /tmp/docusaurus-test/docs/wiki
cp -r out/wiki /tmp/docusaurus-test/docs/
cp out/sidebars.js /tmp/docusaurus-test/

# 3. Build testen
cd /tmp/docusaurus-test
npm run build
```

---

## Wichtige Dateien

| Datei | Zweck |
|-------|-------|
| `MarkdownUtils.java` | MDX-Escaping, Markdown-Cleanup |
| `ArticleWriter.java` | HTML→Markdown Konvertierung, Code-Block-Handling |
| `DocusaurusConfigWriter.java` | Generiert sidebars.js & docusaurus.config.js |
| `CategoryWriter.java` | Generiert Kategorie-Index-Dateien |

---

## ZIP erstellen

```bash
cd /workspaces/mediawiki2docusaurus
rm -f out.zip && zip -r out.zip out/
```
