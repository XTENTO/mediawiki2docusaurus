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
		// Generate _redirects file for Cloudflare Pages (301 redirects from /wiki/* to /*)
		final StringBuilder redirectsFile = new StringBuilder();
		redirectsFile.append("# Redirects from old MediaWiki URLs to new Docusaurus URLs\n");
		redirectsFile.append("# Format: /from /to [status]\n\n");
		
		// General redirect rule: /wiki/* -> /*
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
		
		final Path configPath = Paths.get(Config.BASE_PATH, "..", "docusaurus.config.js");
		Files.writeString(configPath, config);
		System.out.println("Written Docusaurus config to " + configPath);
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
