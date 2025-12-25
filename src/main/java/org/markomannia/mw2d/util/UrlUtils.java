package org.markomannia.mw2d.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

	public static String cleanFileName(final String fileName) {
		// Extract file extension (only for actual files, not directories)
		String extension = "";
		String nameWithoutExtension = fileName;
		final int lastDotIndex = fileName.lastIndexOf('.');
		// Only treat as extension if there's content after the dot and it looks like a file extension
		if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
			final String possibleExtension = fileName.substring(lastDotIndex);
			// Only common file extensions (2-4 chars after dot, no special chars)
			if (possibleExtension.matches("\\.[a-zA-Z0-9]{2,4}")) {
				extension = possibleExtension;
				nameWithoutExtension = fileName.substring(0, lastDotIndex);
			}
		}
		
		// Remove problematic special characters that cause issues in file paths and Docusaurus sidebar IDs
		final String specialCharsRemoved = nameWithoutExtension
				.replace("'", "").replace("´", "").replace("´", "").replace(",", "")
				.replace("!", "").replace("?", "").replace("Â", "")
				.replace(":", "/")   // Doppelpunkt -> Subpfad (z.B. "Magento_2_Extensions:Custom_SMTP" -> "Magento_2_Extensions/Custom_SMTP")
				.replace("&", "_")   // Ampersand -> Unterstrich (z.B. "Royal_Mail_Click_&_Drop")
				.replace(".", "_")   // Punkt -> Unterstrich (z.B. "Xtento/.../Test")
				.replace("\"", "")   // Anführungszeichen entfernen
				.replace("<", "")    // Kleiner-als entfernen
				.replace(">", "")    // Größer-als entfernen
				.replace("|", "_")   // Pipe -> Unterstrich
				.replace("*", "")    // Stern entfernen
				.replace("\\", "/"); // Backslash -> Subpfad

		final String germanCharsReplaced = specialCharsRemoved.replace("ü", "ue").replace("ä", "ae").replace("ö", "oe")
				.replace("Ü", "Ue").replace("Ä", "Ae").replace("Ö", "Oe").replace("ß", "ss");

		final String specialCharsReplaced = germanCharsReplaced.replace("(", "_").replace(")", "_").replace(" ", "_");
		
		// Normalize multiple underscores to single ones
		String normalized = specialCharsReplaced;
		while (normalized.contains("_____")) {
			normalized = normalized.replace("_____", "_");
		}
		while (normalized.contains("____")) {
			normalized = normalized.replace("____", "_");
		}
		while (normalized.contains("___")) {
			normalized = normalized.replace("___", "_");
		}
		while (normalized.contains("__")) {
			normalized = normalized.replace("__", "_");
		}
		
		// Also normalize multiple slashes
		while (normalized.contains("//")) {
			normalized = normalized.replace("//", "/");
		}

		// Remove leading/trailing underscores and slashes from each path segment
		String result = normalized;
		// Handle path segments separately
		if (result.contains("/")) {
			final String[] segments = result.split("/");
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < segments.length; i++) {
				String segment = segments[i];
				while (segment.startsWith("_")) {
					segment = segment.substring(1);
				}
				while (segment.endsWith("_")) {
					segment = segment.substring(0, segment.length() - 1);
				}
				if (!segment.isEmpty()) {
					if (sb.length() > 0) {
						sb.append("/");
					}
					sb.append(segment);
				}
			}
			result = sb.toString();
		} else {
			while (result.startsWith("_")) {
				result = result.substring(1);
			}
			while (result.endsWith("_")) {
				result = result.substring(0, result.length() - 1);
			}
		}

		return result + extension;
	}

	public static Map<String, String> getQueryMapForQuery(final String query) {
		final Pattern pat = Pattern.compile("([^&=]+)=([^&]*)");
		final Matcher matcher = pat.matcher(query);
		final Map<String, String> result = new HashMap<>();

		while (matcher.find()) {
			result.put(matcher.group(1), urlDecode(matcher.group(2)));
		}

		return result;
	}

	public static Map<String, String> getQueryMapForUrl(final String urlString) {
		final URL url = parse(urlString);
		final String query = url.getQuery();

		return query != null ? getQueryMapForQuery(query) : new HashMap<>();
	}

	public static URL parse(final String url) {
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static String urlDecode(final String url) {
		try {
			return URLDecoder.decode(url, StandardCharsets.UTF_8.name());
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
