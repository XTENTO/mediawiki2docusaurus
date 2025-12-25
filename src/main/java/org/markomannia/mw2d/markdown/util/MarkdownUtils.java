package org.markomannia.mw2d.markdown.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownUtils {

	public static String cleanMarkdown(final String markdown) {
		final String htmlEntitiesDecoded = markdown.replace("%C3%B6", "oe").replace("%C3%BC", "ue").replace("%2C", "")
				.replace("%27", "").replace("%C3%A4", "").replace("%C3%9F", "ss").replace("%C3%B6", "oe")
				.replace("%C3%84", "Ä");

		final String linkReplaced1 = htmlEntitiesDecoded.replace(
				"http://www.markomannia.org/index.php?target=kvvereindetail&verein=",
				"https://www.markomannia.org/index.php?pid=verein&id=");

		final String linkReplaced2 = linkReplaced1.replace("http://www.markomannia.org/index.php?target=kvvereine",
				"https://www.markomannia.org/index.php?pid=dachverband_vereine");

		final String linkReplaced3 = linkReplaced2.replace("http://www.markomannia.org", "https://www.markomannia.org");

		final String linkReplaced4 = linkReplaced3.replace(
				"https://www.markomannia.org/index.php?target=intranetchargierkalendershow",
				"https://www.markomannia.org/index.php?pid=intranet_chargierkalender");

		final String linkReplaced5 = linkReplaced4.replace("https://www.markomannia.org/index.php?target=zimmerangebot",
				"https://www.markomannia.org/index.php?pid=zimmer");

		final String linkReplaced6 = linkReplaced5.replace(
				"https://www.markomannia.org/index.php?target=semesterhistory",
				"https://www.markomannia.org/index.php?pid=intranet_home");

		final String linkReplaced7 = linkReplaced6.replace("http://www.markomannenwiki.de/Dokumente/", "");

		final String colonFixed = linkReplaced7.replace(") :", "):");
		
		// Note: fixMultilineInlineCode is no longer needed since we handle <pre> tags
		// directly in ArticleWriter and preserve fenced code blocks
		
		// Escape MDX-problematic characters (curly braces are interpreted as JSX expressions)
		final String mdxEscaped = escapeMdxCharacters(colonFixed);

		return mdxEscaped;
	}
	
	/**
	 * Escapes characters that are problematic in MDX (JSX in Markdown).
	 * - { and } are interpreted as JSX expressions
	 * - < can be interpreted as JSX tags
	 * 
	 * Only escapes outside of fenced code blocks to preserve code formatting.
	 */
	public static String escapeMdxCharacters(final String markdown) {
		final StringBuilder sb = new StringBuilder();
		boolean inFencedCodeBlock = false;
		
		final String[] lines = markdown.split("\n", -1);
		
		for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
			String line = lines[lineIdx];
			
			// Check for fenced code block markers (``` or ~~~)
			final String trimmedLine = line.trim();
			if (trimmedLine.startsWith("```") || trimmedLine.startsWith("~~~")) {
				inFencedCodeBlock = !inFencedCodeBlock;
				sb.append(line);
			} else if (inFencedCodeBlock) {
				// Inside fenced code block - don't escape anything
				sb.append(line);
			} else {
				// Outside code block - escape { and } and ALL < characters
				// Note: We no longer track inline code because it's too error-prone
				// with multi-line inline code and the escapes don't hurt inside inline code
				
				for (int i = 0; i < line.length(); i++) {
					char c = line.charAt(i);
					
					if (c == '{') {
						sb.append("\\{");
					} else if (c == '}') {
						sb.append("\\}");
					} else if (c == '<') {
						// Check if this is a valid Markdown autolink <https://...> or <mailto:...>
						String rest = line.substring(i);
						if (rest.matches("^<(https?://|mailto:|ftp://)[^>]+>.*")) {
							// Valid autolink - don't escape
							sb.append(c);
						} else {
							// Escape ALL other < to prevent MDX/JSX interpretation
							sb.append("\\<");
						}
					} else {
						sb.append(c);
					}
				}
			}
			
			if (lineIdx < lines.length - 1) {
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	public static String fixRemainingAssetLinks(final String markdown) {
		return markdown.replaceAll("\\/images\\/[a-f0-9]\\/[a-f0-9]{2}\\/", "");
	}
}
