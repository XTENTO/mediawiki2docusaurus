package org.markomannia.mw2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.markomannia.mw2d.articles.ArticleRecord;
import org.markomannia.mw2d.articles.ArticleWriter;
import org.markomannia.mw2d.assets.AssetRecord;
import org.markomannia.mw2d.assets.util.AssetUtils;
import org.markomannia.mw2d.categories.CategoryWriter;
import org.markomannia.mw2d.categories.util.CategoryUtils;
import org.markomannia.mw2d.client.MediaWikiCategoryRecord;
import org.markomannia.mw2d.client.MediaWikiClient;
import org.markomannia.mw2d.client.MediaWikiPageRecord;
import org.markomannia.mw2d.document.util.DocumentUtils;
import org.markomannia.mw2d.docusaurus.DocusaurusConfigWriter;
import org.markomannia.mw2d.util.UrlUtils;

public class Main {

	public static void main(final String[] args) throws Exception {
		final List<MediaWikiCategoryRecord> categories = MediaWikiClient.getCategories();
		final List<MediaWikiCategoryRecord> categoriesSorted = CategoryUtils.sortCategories(categories);

		System.out.println("Found " + categories.size() + " categories");

		final List<MediaWikiPageRecord> pages = MediaWikiClient.getPages();

		// Filter out Special: and Category: pages (based on URL pattern)
		final List<MediaWikiPageRecord> realPages = pages.stream()
				.filter(p -> !p.isRedirect())
				.filter(p -> !p.url().contains("/Special:"))
				.filter(p -> !p.url().contains("/Category:"))
				.filter(p -> !p.url().contains("title=Special:"))
				.filter(p -> !p.url().contains("title=Category:"))
				.collect(Collectors.toList());
		final List<MediaWikiPageRecord> redirects = pages.stream()
				.filter(p -> p.isRedirect())
				.filter(p -> !p.url().contains("/Special:"))
				.filter(p -> !p.url().contains("/Category:"))
				.filter(p -> !p.url().contains("title=Special:"))
				.filter(p -> !p.url().contains("title=Category:"))
				.collect(Collectors.toList());

		System.out.println("Found " + realPages.size() + " articles");
		System.out.println("Found " + redirects.size() + " redirects");

		final List<ArticleRecord> articles = new ArrayList<>();
		final Map<String, ArticleRecord> articlesByTitles = new HashMap<>();

		for (final MediaWikiPageRecord page : realPages) {
			final String url = page.url();

			final String html = MediaWikiClient.getHtml(url);
			final ArticleRecord article = onPageCreateArticle(html, url, false, categoriesSorted);

			articlesByTitles.put(article.fromTitle(), article);
			articles.add(article);

			ArticleWriter.writeArticle(article);
		}

		for (final MediaWikiPageRecord page : redirects) {
			final String url = page.url() + "&redirect=no";

			final String html = MediaWikiClient.getHtml(url);
			final ArticleRecord article = onPageCreateArticle(html, url, true, categoriesSorted);

			articlesByTitles.put(article.fromTitle(), article);
			articles.add(article);

			ArticleWriter.writeArticle(article);
		}

		final Set<MediaWikiCategoryRecord> categoriesUsed = articles.stream().map(ArticleRecord::fromCategory)
				.collect(Collectors.toSet());

		for (final MediaWikiCategoryRecord category : categoriesUsed) {
			CategoryWriter.writeCategory(category);
		}

		for (final ArticleRecord article : articles) {
			DocumentUtils.rewriteLinks(article.elements(), articlesByTitles);

			ArticleWriter.writeArticle(article);
		}
		
		// Group articles by category for Docusaurus sidebar
		final Map<String, List<ArticleRecord>> articlesByCategory = new HashMap<>();
		for (final ArticleRecord article : articles) {
			final String categoryText = article.fromCategory() == null || article.fromCategory().text() == null
					? ""
					: article.fromCategory().text();
			articlesByCategory.computeIfAbsent(categoryText, k -> new ArrayList<>()).add(article);
		}
		
		// Write Docusaurus configuration files
		DocusaurusConfigWriter.writeSidebar(categoriesSorted, articlesByCategory);
		DocusaurusConfigWriter.writeDocusaurusConfig(articles);
		
		System.out.println("Migration completed successfully!");
	}

	private static ArticleRecord onPageCreateArticle(final String html, final String fromUrl, final boolean isRedirect,
			final List<MediaWikiCategoryRecord> categories) {
		final Document document = Jsoup.parse(html, fromUrl);
		DocumentUtils.cleanDocument(document);

		/*
		 * title
		 */
		final Map<String, String> queryMap = UrlUtils.getQueryMapForUrl(fromUrl);

		String fromTitle = queryMap.get("title");
		
		// If no title in query params, extract from URL path (e.g., /wiki/Page_Title)
		if (fromTitle == null || fromTitle.isBlank()) {
			try {
				final String path = new java.net.URL(fromUrl).getPath();
				// Extract title from /wiki/PageTitle format
				if (path.contains("/wiki/")) {
					fromTitle = path.substring(path.lastIndexOf("/wiki/") + 6);
					// URL decode and replace underscores with spaces
					fromTitle = java.net.URLDecoder.decode(fromTitle, java.nio.charset.StandardCharsets.UTF_8)
							.replace("_", " ");
				}
			} catch (final Exception e) {
				System.out.println("Warning: Could not extract title from URL: " + fromUrl);
			}
		}
		
		Objects.requireNonNull(fromTitle);

		/*
		 * heading
		 */
		final Elements firstHeadings = document.select("#firstHeading");
		Objects.requireNonNull(firstHeadings);

		final Element firstHeading = DocumentUtils.getFirst(firstHeadings);
		Objects.requireNonNull(firstHeading);

		final String fromFirstHeadingText = firstHeading.text();
		Objects.requireNonNull(fromFirstHeadingText);

		/*
		 * primary category - extract from page or infer from title structure
		 */
		final List<String> pageCategories = document.select(".mw-normal-catlinks ul > li > a").stream().map(c -> {
			return c.text();
		}).filter(c -> {
			return !CategoryUtils.CATEGORIES_EXCLUDED.contains(c);
		}).collect(Collectors.toList());
		
		// Also try to extract category from title structure (e.g., "Magento_2_Extensions/Order_Export")
		String inferredCategory = inferCategoryFromTitle(fromTitle);

		final MediaWikiCategoryRecord fallback = isRedirect
				? new MediaWikiCategoryRecord("Weiterleitung", "Weiterleitung", 0)
				: new MediaWikiCategoryRecord(inferredCategory != null ? inferredCategory : "General Information", 
				                              inferredCategory != null ? inferredCategory : "General Information", 0);

		final MediaWikiCategoryRecord fromCategory = categories.stream().filter(c -> {
			return pageCategories.contains(c.text());
		}).findFirst().orElseGet(() -> {
			// If no category found in page, try to match inferred category
			if (inferredCategory != null) {
				return categories.stream()
					.filter(c -> c.text() != null && c.text().equalsIgnoreCase(inferredCategory))
					.findFirst()
					.orElse(fallback);
			}
			return fallback;
		});

		/*
		 * article body
		 */
		final Elements parserOutput = document.select(".mw-parser-output");
		if (parserOutput.isEmpty()) {
			// Fallback: Try to find content in #bodyContent if .mw-parser-output is not found
			final Element bodyContent = document.selectFirst("#bodyContent");
			if (bodyContent != null) {
				final Elements bodyContentElements = new org.jsoup.select.Elements(bodyContent);
				final Map<String, AssetRecord> assetRecords = AssetUtils.rewriteAssets(bodyContentElements);
				
				return new ArticleRecord(fromTitle, fromUrl, fromFirstHeadingText, fromCategory, 
						bodyContentElements, assetRecords.values());
			}
		}
		Objects.requireNonNull(parserOutput);

		final Map<String, AssetRecord> assetRecords = AssetUtils.rewriteAssets(parserOutput);

		/*
		 * markdown
		 */
		return new ArticleRecord(fromTitle, fromUrl, fromFirstHeadingText, fromCategory, parserOutput,
				assetRecords.values());
	}
	
	/**
	 * Infer category from title structure.
	 * Titles like "Magento_2_Extensions/Order_Export" should be categorized as "Magento 2 Extensions"
	 */
	private static String inferCategoryFromTitle(final String title) {
		if (title == null) return null;
		
		// Check for known category prefixes in title
		final String normalizedTitle = title.replace("_", " ");
		
		// Known category mappings
		final String[][] knownCategories = {
			{"Magento 2 Extensions", "Magento 2 Extensions"},
			{"Magento Extensions", "Magento Extensions"},
			{"Magento Integration Suite", "Magento Integration Suite"},
			{"Product Feed Setup", "Product Feed Setup"},
			{"Connectors", "Connectors"},
			{"Private", "Private"},
			{"Feed Wizard", "Feed Wizard"},
			{"Troubleshooting", "Troubleshooting"},
			{"FTP", "General Information"},
			{"Order Export", "General Information"},
			{"Error", "Troubleshooting"},
			{"AOE Scheduler", "General Information"},
		};
		
		for (final String[] mapping : knownCategories) {
			if (normalizedTitle.startsWith(mapping[0] + "/") || normalizedTitle.startsWith(mapping[0] + ":")) {
				return mapping[1];
			}
		}
		
		return null;
	}
}
