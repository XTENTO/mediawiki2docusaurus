package org.markomannia.mw2d;

import io.github.furstenheim.CopyDown;
import io.github.furstenheim.OptionsBuilder;
import io.github.furstenheim.Options;
import io.github.furstenheim.CodeBlockStyle;

public class TestCopyDownOutput {
    public static void main(String[] args) {
        // Test 1: pre with code
        String html1 = "<p>Some text</p><pre><code>if (isset($_SERVER['MAGE_IS_DEVELOPER_MODE'])) {\n    error_reporting(E_ALL);\n}</code></pre><p>More text</p>";
        
        Options options = OptionsBuilder.anOptions()
            .withCodeBlockStyle(CodeBlockStyle.FENCED)
            .build();
        
        System.out.println("=== Test 1: <pre><code> ===");
        System.out.println(new CopyDown(options).convert(html1));
        System.out.println("=== End Test 1 ===\n");
        
        // Test 2: just pre without code
        String html2 = "<p>Some text</p><pre>if (isset($_SERVER['MAGE_IS_DEVELOPER_MODE'])) {\n    error_reporting(E_ALL);\n}</pre><p>More text</p>";
        
        System.out.println("=== Test 2: just <pre> ===");
        System.out.println(new CopyDown(options).convert(html2));
        System.out.println("=== End Test 2 ===\n");
        
        // Test 3: just code tag
        String html3 = "<p>Some text</p><code>if (isset($_SERVER['MAGE_IS_DEVELOPER_MODE'])) {\n    error_reporting(E_ALL);\n}</code><p>More text</p>";
        
        System.out.println("=== Test 3: just <code> ===");
        System.out.println(new CopyDown(options).convert(html3));
        System.out.println("=== End Test 3 ===");
    }
}
