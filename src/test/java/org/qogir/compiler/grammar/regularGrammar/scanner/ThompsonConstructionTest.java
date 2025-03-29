package org.qogir.compiler.grammar.regularGrammar.scanner;

import org.qogir.compiler.grammar.regularGrammar.*;

public class ThompsonConstructionTest {
    public static void main(String[] args) {

        ParseRegex pr = new ParseRegex(new Regex("Any string including 'ab'","汉字汉字",0));

        ThompsonConstruction tc = new ThompsonConstruction();
        TNFA tN = tc.translate(pr.parse().getRoot());
        System.out.println(tN.toString());
        System.out.println(Character.isLetter('你'));
    }
}
