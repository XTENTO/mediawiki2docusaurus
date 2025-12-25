package org.markomannia.mw2d.articles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.markomannia.mw2d.Config;
import org.markomannia.mw2d.articles.util.ArticleUtils;
import org.markomannia.mw2d.assets.AssetRecord;
import org.markomannia.mw2d.client.MediaWikiClient;
import org.markomannia.mw2d.extensions.youtube.YoutubeRewriter;
import org.markomannia.mw2d.markdown.util.MarkdownUtils;

import io.github.furstenheim.CopyDown;
import io.github.furstenheim.OptionsBuilder;
import io.github.furstenheim.Options;

public class ArticleWriter {

	private static final String CODE_BLOCK_MARKER = "___CODEBLOCK_MARKER_";
	private static final java.util.Map<String, String> codeBlockMap = new java.util.concurrent.ConcurrentHashMap<>();
	private static int codeBlockCounter = 0;

	/**
	 * Extract <pre> tags and replace with markers before CopyDown processing.
	 * Returns the modified HTML.
	 */
	private static String extractCodeBlocks(final String html) {
		codeBlockMap.clear();
		String result = html;
		
		java.util.regex.Pattern prePattern = java.util.regex.Pattern.compile(
			"<pre[^>]*>([\\s\\S]*?)</pre>",
			java.util.regex.Pattern.CASE_INSENSITIVE
		);
		
		java.util.regex.Matcher matcher = prePattern.matcher(result);
		StringBuffer sb = new StringBuffer();
		
		while (matcher.find()) {
			String content = matcher.group(1);
			// Clean up the content - remove inner <code> tags if present
			content = content.replaceAll("</?code[^>]*>", "");
			// HTML entity decode common entities
			content = content.replace("&lt;", "<").replace("&gt;", ">")
			                 .replace("&amp;", "&").replace("&quot;", "\"");
			
			String markerId = CODE_BLOCK_MARKER + (codeBlockCounter++) + "___";
			codeBlockMap.put(markerId, content.trim());
			
			matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("\n\n" + markerId + "\n\n"));
		}
		matcher.appendTail(sb);
		
		return sb.toString();
	}
	
	/**
	 * Restore code blocks from markers to fenced markdown code blocks.
	 */
	private static String restoreCodeBlocks(final String markdown) {
		String result = markdown;
		
		for (java.util.Map.Entry<String, String> entry : codeBlockMap.entrySet()) {
			String marker = entry.getKey();
			String code = entry.getValue();
			String fencedBlock = "\n```\n" + code + "\n```\n";
			result = result.replace(marker, fencedBlock);
		}
		
		return result;
	}

	private static String createMarkdown(final ArticleRecord article) {
		final String rawHtml = article.elements().html();
		
		// Extract code blocks before CopyDown processing
		final String html = extractCodeBlocks(rawHtml);
		
		// Configure CopyDown to use fenced code blocks instead of indented
		final Options options = OptionsBuilder.anOptions()
				.withCodeBlockStyle(io.github.furstenheim.CodeBlockStyle.FENCED)
				.build();
		final String parserOutputMarkdown = new CopyDown(options).convert(html);
		
		// Remove remaining HTML tags but preserve the content
		// Be careful not to remove content inside code blocks - skip lines starting with ```
		final String parserOutputMarkdownWithoutHtml = removeHtmlTagsPreservingCodeBlocks(parserOutputMarkdown);
		final String parserOutputMarkdownWithFixedUnderline = parserOutputMarkdownWithoutHtml.replace("\\_", "_");

		// Create Docusaurus frontmatter
		final StringBuilder frontmatter = new StringBuilder();
		frontmatter.append("---\n");
		frontmatter.append("title: \"").append(article.fromHeading().replace("\"", "\\\"")).append("\"\n");
		
		if (article.fromCategory() != null && article.fromCategory().text() != null && !article.fromCategory().text().isEmpty()) {
			frontmatter.append("sidebar_label: \"").append(article.fromHeading().replace("\"", "\\\"")).append("\"\n");
		}
		
		frontmatter.append("---\n\n");
		
		final String markdown = frontmatter.toString() + "# " + article.fromHeading() + "\n\n" + parserOutputMarkdownWithFixedUnderline;
		final String markdownWithYoutube = YoutubeRewriter.rewriteYoutubeLinks(markdown);
		final String markdownWithFixedAssetLinks = MarkdownUtils.fixRemainingAssetLinks(markdownWithYoutube);
		
		// Restore code blocks from markers BEFORE MDX escaping
		final String markdownWithCodeBlocks = restoreCodeBlocks(markdownWithFixedAssetLinks);

		return MarkdownUtils.cleanMarkdown(markdownWithCodeBlocks);
	}

	public static String determineDirectoryPath(final ArticleRecord article) {
		return Config.BASE_PATH + ArticleUtils.determineArticleWithCategoryPath(article);
	}

	/**
	 * Remove HTML tags from markdown, but preserve content inside fenced code blocks.
	 */
	private static String removeHtmlTagsPreservingCodeBlocks(final String markdown) {
		final StringBuilder result = new StringBuilder();
		boolean inCodeBlock = false;
		
		for (final String line : markdown.split("\n", -1)) {
			if (line.startsWith("```")) {
				inCodeBlock = !inCodeBlock;
				result.append(line).append("\n");
			} else if (inCodeBlock) {
				// Inside code block - don't modify
				result.append(line).append("\n");
			} else {
				// Outside code block - remove HTML tags
				result.append(line.replaceAll("<([^>]+)>", "$1")).append("\n");
			}
		}
		
		// Remove trailing newline if original didn't have one
		if (!markdown.endsWith("\n") && result.length() > 0) {
			result.setLength(result.length() - 1);
		}
		
		return result.toString();
	}

	public static Path determineFilePath(final ArticleRecord article) {
		final String directoryPath = determineDirectoryPath(article);
		return Paths.get(directoryPath, "index.md");
	}

	public static void writeArticle(final ArticleRecord article) throws IOException, InterruptedException {
		final String markdown = createMarkdown(article);
		final Collection<AssetRecord> assets = article.assets();

		final String directoryPath = determineDirectoryPath(article);
		new File(directoryPath).mkdirs();

		final byte[] markdownBytes = markdown.getBytes();

		for (final AssetRecord asset : assets) {
			final String assetAbsUrl = asset.absUrl();
			final String assetFileName = asset.fileName();
			final Path assetFilePath = Path.of(directoryPath, assetFileName);

			if (!assetFilePath.toFile().exists()) {
				final byte[] assetBytes = MediaWikiClient.getAsset(assetAbsUrl);

				if (assetBytes == null) {
				} else {
					System.out.println("Writing asset " + assetFilePath);

					Files.write(assetFilePath, assetBytes);
				}
			}
		}

		final Path path = determineFilePath(article);

		System.out.println("Writing article " + path);

		Files.write(path, markdownBytes);
	}
}
