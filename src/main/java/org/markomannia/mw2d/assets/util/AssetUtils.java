package org.markomannia.mw2d.assets.util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jsoup.select.Elements;
import org.markomannia.mw2d.assets.AssetRecord;
import org.markomannia.mw2d.util.UrlUtils;

public class AssetUtils {

	public static String cleanAssetFileName(final String fileName) {
		// For assets, we don't want subpaths - replace : with _ instead of /
		// Extract file extension
		String extension = "";
		String nameWithoutExtension = fileName;
		final int lastDotIndex = fileName.lastIndexOf('.');
		// Only treat as extension if there's content after the dot and it looks like a file extension
		if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
			final String possibleExtension = fileName.substring(lastDotIndex);
			// Only common file extensions (2-4 chars after dot, no special chars)
			if (possibleExtension.matches("\\.[a-zA-Z0-9]{2,5}")) {
				extension = possibleExtension;
				nameWithoutExtension = fileName.substring(0, lastDotIndex);
			}
		}
		
		// Remove problematic special characters - for assets, ALL special chars become _ (no subpaths)
		String cleaned = nameWithoutExtension
				.replace("'", "").replace("´", "").replace("´", "").replace(",", "")
				.replace("!", "").replace("?", "").replace("Â", "")
				.replace(":", "_")   // Doppelpunkt -> Unterstrich (keine Subpfade für Assets)
				.replace("/", "_")   // Schrägstrich -> Unterstrich
				.replace("&", "_")
				.replace(".", "_")
				.replace("\"", "")
				.replace("<", "")
				.replace(">", "")
				.replace("|", "_")
				.replace("*", "")
				.replace("\\", "_")
				.replace("(", "_").replace(")", "_").replace(" ", "_");
		
		// German chars
		cleaned = cleaned.replace("ü", "ue").replace("ä", "ae").replace("ö", "oe")
				.replace("Ü", "Ue").replace("Ä", "Ae").replace("Ö", "Oe").replace("ß", "ss");
		
		// Normalize multiple underscores
		while (cleaned.contains("__")) {
			cleaned = cleaned.replace("__", "_");
		}
		
		// Remove leading/trailing underscores
		while (cleaned.startsWith("_")) {
			cleaned = cleaned.substring(1);
		}
		while (cleaned.endsWith("_")) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}
		
		return cleaned + extension;
	}

	public static Map<String, AssetRecord> rewriteAssets(final Elements parserOutput) {
		/*
		 * assets
		 */
		final Map<String, AssetRecord> result = new HashMap<>();

		/*
		 * imgs
		 */
		final Elements assets = parserOutput.select("img");
		Objects.requireNonNull(assets);

		assets.forEach(a -> {
			final String assetRelUrl = a.attr("src");
			final String assetAbsUrl = a.absUrl("src");

			final String assetFileName = rewriteAssetUrlToFileName(assetRelUrl);
			final AssetRecord asset = new AssetRecord(assetRelUrl, assetAbsUrl, assetFileName);

			result.put(assetRelUrl, asset);
		});

		/*
		 * files
		 */
		final Elements files = parserOutput
				.select("a[href*=\".mid\"],a[href*=\".png\"],a[href*=\".jpg\"],a[href*=\".jpeg\"],a[href*=\".gif\"],a[href*=\".svg\"],a[href*=\".webp\"],a[href*=\".pdf\"],a[href*=\".zip\"],a[href*=\".gz\"],a[href*=\".tar\"]");

		files.forEach(f -> {
			final String assetRelUrl = f.attr("href");
			final String assetAbsUrl = f.absUrl("href");

			if (!assetRelUrl.contains("Datei:") && !assetRelUrl.isBlank()) {
				final String assetFileName = rewriteAssetUrlToFileName(assetRelUrl);
				final AssetRecord asset = new AssetRecord(assetRelUrl, assetAbsUrl, assetFileName);

				result.put(assetRelUrl, asset);
			}
		});

		/*
		 * clean HTML for conversion to markdown
		 */

		// fix img URLs
		parserOutput.select("img").forEach(img -> {
			final String src = img.attr("src");
			if (src != null && !src.isBlank()) {
				final AssetRecord asset = result.get(src);
				if (asset != null) {
					img.attr("src", asset.fileName());
				} else {
					System.out.println("Warning: Could not find asset for image: " + src);
				}
			}
		});

		// fix media file URLs
		parserOutput.select("a[href*=\".mid\"],a[href*=\".png\"],a[href*=\".jpg\"],a[href*=\".jpeg\"],a[href*=\".gif\"],a[href*=\".svg\"],a[href*=\".webp\"],a[href*=\".pdf\"],a[href*=\".zip\"],a[href*=\".gz\"],a[href*=\".tar\"]").forEach(link -> {
			final String href = link.attr("href");
			if (href != null && !href.isBlank()) {
				final AssetRecord asset = result.get(href);
				if (asset != null) {
					link.attr("href", asset.fileName());
				} else {
					System.out.println("Warning: Could not find asset for file: " + href);
				}
			}
		});

		return result;
	}

	public static String rewriteAssetUrlToFileName(final String url) {
		final String assetRelUrlFileName = Path.of(url).getFileName().toString();

		return cleanAssetFileName(UrlUtils.urlDecode(assetRelUrlFileName));
	}
}
