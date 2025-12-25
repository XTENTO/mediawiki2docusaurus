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
		
		// Add main page - use cleaned filename
		sb.append("    {\n");
		sb.append("      type: 'doc',\n");
		sb.append("      id: 'Main_Page/index',\n");
		sb.append("      label: 'Main Page',\n");
		sb.append("    },\n");
		
		// Group articles by category
		for (final MediaWikiCategoryRecord category : categories) {
			final String categoryText = category.text();
			if (categoryText == null || categoryText.isEmpty()) {
				continue;
			}
			
			final List<ArticleRecord> articlesInCategory = articlesByCategory.getOrDefault(categoryText, List.of());
			
			if (articlesInCategory.isEmpty()) {
				continue;
			}
			
			sb.append("    {\n");
			sb.append("      type: 'category',\n");
			sb.append("      label: '").append(escapeJavaScript(categoryText)).append("',\n");
			sb.append("      items: [\n");
			
			for (final ArticleRecord article : articlesInCategory) {
				// Use cleaned article title for the path
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
		
		// Add uncategorized articles
		final List<ArticleRecord> uncategorized = articlesByCategory.getOrDefault("", List.of());
		if (!uncategorized.isEmpty()) {
			sb.append("    {\n");
			sb.append("      type: 'category',\n");
			sb.append("      label: 'General',\n");
			sb.append("      items: [\n");
			
			for (final ArticleRecord article : uncategorized) {
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
	
	public static void writeDocusaurusConfig() throws IOException {
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
                to: '/Main_Page',
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
        additionalLanguages: ['php', 'bash', 'json', 'xml'],
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
