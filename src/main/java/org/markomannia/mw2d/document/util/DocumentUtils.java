package org.markomannia.mw2d.document.util;

import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.markomannia.mw2d.articles.ArticleRecord;
import org.markomannia.mw2d.articles.util.ArticleUtils;
import org.markomannia.mw2d.util.UrlUtils;

public class DocumentUtils {

	public static void cleanDocument(final Document document) {
		// remove toc
		document.select("#toc").remove();

		// remove Tondateibeschreibungsseite mit Lizenzangabe around files
		document.select(".mw-parser-output sup a").remove();

		// remove loudspeaker images
		document.select(".mw-parser-output .image[href*=\"Loudspeaker\"]").remove();

		// remove magnify/enlarge icons from thumbnails
		document.select(".magnify").remove();
		document.select("a[title=Enlarge]").remove();
		document.select("img[src*=magnify-clip]").remove();

		// remove Vorlage_Begriffsklaerung
		document.select("#Vorlage_Begriffsklaerung").remove();

		// remove a href around images
		document.select(".mw-parser-output .image").removeAttr("href");
		
		// remove "Retrieved from" footer
		document.select("#catlinks").remove();
		document.select(".printfooter").remove();
		
		// remove "(Redirected from ...)" notice
		document.select(".mw-redirectedfrom").remove();
		
		// Clean syntax-highlighted code blocks - strip span tags but keep text
		// MediaWiki uses spans like <span class="re0">$order</span> for syntax highlighting
		for (Element pre : document.select("pre")) {
			// Get text content only, stripping all HTML tags
			String plainCode = pre.text();
			pre.html(plainCode);
		}
	}

	public static Element getFirst(final Elements elements) {
		return elements.stream().findFirst().orElse(null);
	}

	public static void rewriteLinks(final Elements elements, final Map<String, ArticleRecord> articlesByTitles) {
		// rewrite a href to /index.php?title=
		elements.select("a[href*=\"/index.php?title=\"]").forEach(a -> {
			final String url = UrlUtils.urlDecode(a.attr("href"));
			final String query = url.replace("/index.php?", "");

			final Map<String, String> queryMap = UrlUtils.getQueryMapForQuery(query);
			final String title = queryMap.get("title");

			final ArticleRecord article = articlesByTitles.get(title);

			if (article == null) {
				a.removeAttr("href");
			} else {
				final String newPath = ArticleUtils.determineArticleWithCategoryPath(article);
				a.attr("href", newPath);
			}
		});
		
		// Rewrite /wiki/ links to root links
		elements.select("a[href^=\"/wiki/\"]").forEach(a -> {
			String href = a.attr("href");
			// Remove /wiki/ prefix
			href = href.replace("/wiki/", "/");
			// Convert Category: links to normal paths
			href = href.replace("Category:", "");
			a.attr("href", href);
		});
	}
}
