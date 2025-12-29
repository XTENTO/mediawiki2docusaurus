package org.markomannia.mw2d.docusaurus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.markomannia.mw2d.Config;
import org.markomannia.mw2d.articles.ArticleRecord;
import org.markomannia.mw2d.client.MediaWikiCategoryRecord;

public class DocusaurusConfigWriter {

	public static void writeSidebar(final List<MediaWikiCategoryRecord> categories,
			final Map<String, List<ArticleRecord>> articlesByCategory) throws IOException {
		
		final StringBuilder sb = new StringBuilder();
		
		// Sidebar JavaScript/TypeScript structure
		sb.append("// @ts-check\n\n");
		sb.append("/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */\n");
		sb.append("const sidebars = {\n");
		sb.append("  mainSidebar: [\n");
		
		// Define hierarchical sidebar structure
		// Group related categories together - ORDERED: General Info, Magento 2, Magento 1, Integration Suite, Product Feeds
		final java.util.LinkedHashMap<String, List<String>> sidebarGroups = new java.util.LinkedHashMap<>();
		sidebarGroups.put("General Information", List.of("General Information", "Troubleshooting", "Private"));
		sidebarGroups.put("Magento 2", List.of("Magento 2 Extensions", "Magento 2"));
		sidebarGroups.put("Magento 1", List.of("Magento Extensions", "Magento 1"));
		sidebarGroups.put("Integration Suite", List.of("Magento Integration Suite", "Connectors"));
		sidebarGroups.put("Product Feeds", List.of("Product Feed Setup", "Feed Wizard"));
		
		// Track which categories have been used
		final java.util.Set<String> usedCategories = new java.util.HashSet<>();
		
		for (final java.util.Map.Entry<String, List<String>> group : sidebarGroups.entrySet()) {
			final String groupLabel = group.getKey();
			final List<String> categoryNames = group.getValue();
			
			// Collect all articles for this group
			final List<ArticleRecord> groupArticles = new java.util.ArrayList<>();
			for (final String catName : categoryNames) {
				groupArticles.addAll(articlesByCategory.getOrDefault(catName, List.of()));
				usedCategories.add(catName);
			}
			
			if (groupArticles.isEmpty()) continue;
			
			// Sort articles alphabetically
			groupArticles.sort((a, b) -> a.fromHeading().compareToIgnoreCase(b.fromHeading()));
			
			// Filter out Main_Page - it will be the homepage
			final List<ArticleRecord> filteredArticles = groupArticles.stream()
				.filter(a -> !a.fromTitle().equals("Main_Page") && !a.fromTitle().equals("Main Page"))
				.collect(Collectors.toList());
			
			if (filteredArticles.isEmpty()) continue;
			
			sb.append("    {\n");
			sb.append("      type: 'category',\n");
			sb.append("      label: '").append(escapeJavaScript(groupLabel)).append("',\n");
			sb.append("      collapsible: true,\n");
			sb.append("      collapsed: true,\n");
			sb.append("      items: [\n");
			
			for (final ArticleRecord article : filteredArticles) {
				final String cleanedTitle = cleanPathForSidebar(article.fromTitle());
				// Clean the label - remove category prefix if present
				String readableTitle = article.fromHeading();
				for (final String catName : categoryNames) {
					if (readableTitle.startsWith(catName + ":")) {
						readableTitle = readableTitle.substring(catName.length() + 1).trim();
						break;
					}
				}
				
				sb.append("        {\n");
				sb.append("          type: 'doc',\n");
				sb.append("          id: '").append(cleanedTitle).append("/index',\n");
				sb.append("          label: '").append(escapeJavaScript(readableTitle)).append("',\n");
				sb.append("        },\n");
			}
			
			sb.append("      ],\n");
			sb.append("    },\n");
		}
		
		// Add any remaining uncategorized articles
		final List<ArticleRecord> uncategorized = new java.util.ArrayList<>();
		uncategorized.addAll(articlesByCategory.getOrDefault("", List.of()));
		
		// Also add articles from categories not in our defined groups
		for (final java.util.Map.Entry<String, List<ArticleRecord>> entry : articlesByCategory.entrySet()) {
			if (!usedCategories.contains(entry.getKey()) && !entry.getKey().isEmpty()) {
				uncategorized.addAll(entry.getValue());
			}
		}
		
		// Filter out Main_Page and redirects
		final List<ArticleRecord> filteredUncategorized = uncategorized.stream()
			.filter(a -> !a.fromTitle().equals("Main_Page") && !a.fromTitle().equals("Main Page"))
			.filter(a -> a.fromCategory() == null || !a.fromCategory().text().equals("Weiterleitung"))
			.collect(Collectors.toList());
		
		if (!filteredUncategorized.isEmpty()) {
			filteredUncategorized.sort((a, b) -> a.fromHeading().compareToIgnoreCase(b.fromHeading()));
			
			sb.append("    {\n");
			sb.append("      type: 'category',\n");
			sb.append("      label: 'Other',\n");
			sb.append("      collapsible: true,\n");
			sb.append("      collapsed: true,\n");
			sb.append("      items: [\n");
			
			for (final ArticleRecord article : filteredUncategorized) {
				final String cleanedTitle = cleanPathForSidebar(article.fromTitle());
				final String readableTitle = article.fromHeading();
				sb.append("        {\n");
				sb.append("          type: 'doc',\n");
				sb.append("          id: '").append(cleanedTitle).append("/index',\n");
				sb.append("          label: '").append(escapeJavaScript(readableTitle)).append("',\n");
				sb.append("        },\n");
			}
			
			sb.append("      ],\n");
			sb.append("    },\n");
		}
		
		sb.append("  ],\n");
		sb.append("};\n\n");
		sb.append("export default sidebars;\n");
		
		final Path sidebarPath = Paths.get(Config.BASE_PATH, "..", "sidebars.js");
		Files.writeString(sidebarPath, sb.toString());
		System.out.println("Written Docusaurus sidebar to " + sidebarPath);
	}
	
	public static void writeDocusaurusConfig(final List<ArticleRecord> articles) throws IOException {
		// Build redirects list for Docusaurus plugin (creates actual HTML files at old paths)
		final StringBuilder redirectsArray = new StringBuilder();
		redirectsArray.append("[\n");
		
		// Generate redirects from original MediaWiki URLs to new clean URLs
		for (final ArticleRecord article : articles) {
			final String fromUrl = article.fromUrl();
			final String fromTitle = article.fromTitle();
			
			// Main_Page is the homepage, so redirect to /
			String newPath;
			if (fromTitle.equals("Main_Page") || fromTitle.equals("Main Page")) {
				newPath = "/";
			} else {
				newPath = "/" + org.markomannia.mw2d.util.UrlUtils.cleanFileName(fromTitle);
			}
			
			// Extract the original wiki path from the URL (e.g., /wiki/Magento_2_Extensions:Advanced_Order_Status)
			String originalWikiPath = null;
			try {
				final java.net.URL url = new java.net.URL(fromUrl);
				String path = url.getPath();
				// Remove &redirect=no query parameter path
				if (path.contains("/wiki/")) {
					originalWikiPath = path; // Keep the full /wiki/... path
				} else if (url.getQuery() != null && url.getQuery().contains("title=")) {
					// Handle /index.php?title=... format
					String title = fromUrl.substring(fromUrl.indexOf("title=") + 6);
					if (title.contains("&")) {
						title = title.substring(0, title.indexOf("&"));
					}
					originalWikiPath = "/wiki/" + title;
				}
			} catch (final Exception e) {
				System.out.println("Warning: Could not parse URL for redirects: " + fromUrl);
			}
			
			if (originalWikiPath != null) {
				// Add redirect from original wiki path (with colons) to new path (with slashes)
				redirectsArray.append("      { from: '").append(escapeJavaScript(originalWikiPath)).append("', to: '").append(escapeJavaScript(newPath)).append("' },\n");
				
				// Also add URL-encoded version (: -> %3A) for browser compatibility
				if (originalWikiPath.contains(":")) {
					final String encodedWikiPath = originalWikiPath.replace(":", "%3A");
					redirectsArray.append("      { from: '").append(escapeJavaScript(encodedWikiPath)).append("', to: '").append(escapeJavaScript(newPath)).append("' },\n");
				}
				
				// Also add redirect without /wiki/ prefix
				final String pathWithoutWiki = originalWikiPath.replace("/wiki/", "/");
				if (!pathWithoutWiki.equals(newPath)) {
					redirectsArray.append("      { from: '").append(escapeJavaScript(pathWithoutWiki)).append("', to: '").append(escapeJavaScript(newPath)).append("' },\n");
					
					// Also URL-encoded version without /wiki/
					if (pathWithoutWiki.contains(":")) {
						final String encodedPath = pathWithoutWiki.replace(":", "%3A");
						redirectsArray.append("      { from: '").append(escapeJavaScript(encodedPath)).append("', to: '").append(escapeJavaScript(newPath)).append("' },\n");
					}
				}
			}
		}
		
		redirectsArray.append("    ]");
		
		// Also write _redirects for Cloudflare (backup)
		final StringBuilder redirectsFile = new StringBuilder();
		redirectsFile.append("# Redirects from old MediaWiki URLs to new Docusaurus URLs\n");
		redirectsFile.append("# Format: /from /to [status]\n\n");
		redirectsFile.append("# General redirect rule\n");
		redirectsFile.append("/wiki/* /:splat 301\n");
		
		final Path redirectsPath = Paths.get(Config.BASE_PATH, "..", "static", "_redirects");
		Files.createDirectories(redirectsPath.getParent());
		Files.writeString(redirectsPath, redirectsFile.toString());
		System.out.println("Written _redirects file to " + redirectsPath);
		
		final String config = """
// @ts-check
// `@type` JSDoc annotations allow editor autocompletion and type checking
// (when paired with `@ts-check`).

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'XTENTO Support Wiki',
  tagline: 'Support documentation for XTENTO Magento Extensions',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  url: 'https://support.xtento.com',
  baseUrl: '/',

  // Remove trailing slashes from URLs
  trailingSlash: false,

  organizationName: 'xtento',
  projectName: 'support-wiki',

  onBrokenLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  themes: [
    [
      require.resolve('@easyops-cn/docusaurus-search-local'),
      /** @type {import('@easyops-cn/docusaurus-search-local').PluginOptions} */
      ({
        hashed: true,
        language: ['en'],
        indexDocs: true,
        indexBlog: false,
        docsRouteBasePath: '/',
        highlightSearchTermsOnTargetPage: true,
        searchResultLimits: 10,
        searchBarShortcutHint: true,
      }),
    ],
  ],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          path: 'wiki',
          routeBasePath: '/',
          sidebarPath: './sidebars.js',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  plugins: [
    [
      '@docusaurus/plugin-client-redirects',
      {
        redirects: REDIRECTS_PLACEHOLDER,
      },
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'XTENTO Support Wiki',
        logo: {
          alt: 'XTENTO Logo',
          src: 'img/logo.png',
        },
        items: [
          {
            type: 'search',
            position: 'right',
          },
          {
            href: 'http://www.xtento.com',
            label: 'XTENTO.com',
            position: 'right',
          },
          {
            href: 'http://www.xtento.com/contacts',
            label: 'Contact Us',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'General Information',
            items: [
              {
                label: 'Main Page',
                to: '/',
              },
              {
                label: 'Contact Us',
                href: 'http://www.xtento.com/contacts',
              },
            ],
          },
          {
            title: 'Magento 2',
            items: [
              {
                label: 'Setup & Configuration',
                to: '/Installing_and_setting_up_a_Magento_2_extension',
              },
              {
                label: 'License Keys',
                to: '/License_Keys',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'XTENTO Website',
                href: 'http://www.xtento.com',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} XTENTO. All rights reserved.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
      },
    }),
};

export default config;
""";
		
		// Replace placeholder with actual redirects array
		final String configWithRedirects = config.replace("REDIRECTS_PLACEHOLDER", redirectsArray.toString());
		
		final Path configPath = Paths.get(Config.BASE_PATH, "..", "docusaurus.config.js");
		Files.writeString(configPath, configWithRedirects);
		System.out.println("Written Docusaurus config to " + configPath);
		
		// Generate custom.css with styling adjustments
		final String customCss = """
/* Custom CSS for XTENTO Support Wiki */

/* Reduce H1 font size by 25% */
.markdown h1:first-child {
  font-size: 1.5rem;
}

article h1 {
  font-size: 1.5rem;
}

/* Also reduce other heading sizes proportionally */
.markdown h2 {
  font-size: 1.3rem;
}

.markdown h3 {
  font-size: 1.1rem;
}

/* Primary color customization (optional) */
:root {
  --ifm-color-primary: #2e8555;
  --ifm-color-primary-dark: #29784c;
  --ifm-color-primary-darker: #277148;
  --ifm-color-primary-darkest: #205d3b;
  --ifm-color-primary-light: #33925d;
  --ifm-color-primary-lighter: #359962;
  --ifm-color-primary-lightest: #3cad6e;
}
""";
		
		final Path cssPath = Paths.get(Config.BASE_PATH, "..", "src", "css", "custom.css");
		Files.createDirectories(cssPath.getParent());
		Files.writeString(cssPath, customCss);
		System.out.println("Written custom.css to " + cssPath);
	}
	
	private static String escapeJavaScript(final String str) {
		return str.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}
	
	private static String cleanPathForSidebar(final String title) {
		// Use the same logic as ArticleUtils.cleanArticleFileName to match actual file paths
		return org.markomannia.mw2d.util.UrlUtils.cleanFileName(title);
	}
}
