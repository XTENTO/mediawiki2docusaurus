package org.markomannia.mw2d.articles.util;

import org.markomannia.mw2d.articles.ArticleRecord;
import org.markomannia.mw2d.categories.util.CategoryUtils;
import org.markomannia.mw2d.util.UrlUtils;

public class ArticleUtils {

	public static String cleanArticleFileName(final String fileName) {
		return UrlUtils.cleanFileName(fileName);
	}

	public static String determineArticleWithCategoryPath(final ArticleRecord article) {
		// Use only the article title for the path - don't add category prefix
		// This preserves the original MediaWiki URL structure
		return "/" + cleanArticleFileName(article.fromTitle());
	}
}
