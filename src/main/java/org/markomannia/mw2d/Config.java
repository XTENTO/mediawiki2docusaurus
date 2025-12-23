package org.markomannia.mw2d;

import java.util.Base64;

public class Config {

	public static final String BASE_PATH = "/workspaces/mediawiki2docusaurus/out";

	public static final String MEDIAWIKI_URL = "https://support.xtento.com/wiki/";

	public static final String OPTIONAL_AUTH = Base64.getEncoder().encodeToString("username:password".getBytes());
}
