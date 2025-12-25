package org.markomannia.mw2d;

import org.markomannia.mw2d.markdown.util.MarkdownUtils;

public class TestEscaping {
    public static void main(String[] args) {
        String input = "add addUtf8Bom=\"1\" to the <file tag of your XSL Template. Check out the \"Variables in the <file> node\"";
        String output = MarkdownUtils.cleanMarkdown(input);
        System.out.println("Input:  " + input);
        System.out.println("Output: " + output);
        System.out.println();
        System.out.println("< escaped: " + (output.contains("\\<") ? "YES" : "NO"));
    }
}
