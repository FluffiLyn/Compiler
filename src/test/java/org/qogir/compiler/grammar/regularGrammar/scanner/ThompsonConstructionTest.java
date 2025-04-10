package org.qogir.compiler.grammar.regularGrammar.scanner;

import org.qogir.compiler.grammar.regularGrammar.*;

public class ThompsonConstructionTest {
    public static void main(String[] args) {

        ParseRegex pr = new ParseRegex(new Regex("Correct", "c(a|b)*", 0));
        //一个正确的输入
        System.out.println(pr.parse().toString());
        pr = new ParseRegex(new Regex("Error", "c(a|b))*", 0));
        //一个错误的输入，存在一个右括号，没有左括号与之匹配
        System.out.println(pr.parse().toString());
        //RE2NFA转换
        pr = new ParseRegex(new Regex("Correct", "c(a|b)*", 0));
        ThompsonConstruction tc = new ThompsonConstruction();
        TNFA tN = tc.translate(pr.parse().getRoot());
        System.out.println(tN.toString());
}
}
