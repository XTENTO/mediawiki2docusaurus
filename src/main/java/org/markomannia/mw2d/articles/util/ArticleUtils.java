package org.markomannia.mw2d.articles.util;

import org.markomannia.mw2d.articles.ArticleRecord;
import org.markomannia.mw2d.categories.util.CategoryUtils;
import org.markomannia.mw2d.util.UrlUtils;

public class ArticleUtils {

	public static String cleanArticleFileName(final String fileName) {
		return UrlUtils.cleanFileName(fileName);
	}

	public static String determineArticleWithCategoryPath(final ArticleRecord article) {
		final String categoryText = article.fromCategory() == null || article.fromCategory().text() == null || article.fromCategory().text().isEmpty()
				? ""
				: article.fromCategory().text();
		
		final String categoryDirectory = categoryText.isEmpty() ? "" : "/" + categoryText;

		return CategoryUtils.cleanCategoryDirectoryName(categoryDirectory) + "/"
				+ cleanArticleFileName(article.fromTitle());
	}
}
