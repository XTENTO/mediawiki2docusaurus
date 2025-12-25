package org.markomannia.mw2d;

import io.github.furstenheim.CopyDown;
import io.github.furstenheim.OptionsBuilder;
import io.github.furstenheim.Options;
import io.github.furstenheim.CodeBlockStyle;

public class TableConversionTest {
    public static void main(String[] args) {
        // MediaWiki style table
        String html = "<table class=\"wikitable\"><tr><th>Version</th><th>Status</th></tr><tr><td>2.4.8</td><td>OK</td></tr><tr><td>2.4.7</td><td>OK</td></tr></table>";
        
        Options options = OptionsBuilder.anOptions()
            .withCodeBlockStyle(CodeBlockStyle.FENCED)
            .build();
        
        System.out.println("=== Input HTML ===");
        System.out.println(html);
        System.out.println("\n=== CopyDown Output ===");
        String result = new CopyDown(options).convert(html);
        System.out.println("Result: '" + result + "'");
        System.out.println("\n=== With line breaks visible ===");
        System.out.println(result.replace("\n", "\\n\n"));
    }
}
