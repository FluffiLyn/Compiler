package org.qogir.compiler.grammar.regularGrammar.scanner;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.grammar.regularGrammar.RegularGrammar;
import org.qogir.compiler.grammar.regularGrammar.TNFA;
import org.qogir.simulation.scanner.Scanner;
import org.qogir.compiler.grammar.regularGrammar.RDFA;

public class ScannerTest {
    public static void main(String[] args) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        String[] regexes = new String[]{"regex0 := c(a|b)*"};
        //{"regex1 := a|ε", "regex2 := d(f|ea*(g|h))b","regex3 := c(a|b)*|de|fg|hi"}

        //test invalid regex
        //RegularGrammar wrongrg = new RegularGrammar(new String[]{"regex0 := *a","regex1 := (a"});
        //System.out.println(wrongrg);
        //System.out.println(new Scanner(wrongrg).constructRegexTrees().toString());

        //test defining a regular grammar
        RegularGrammar rg = new RegularGrammar(regexes);
        System.out.println(rg);

        //test building a grammar for the grammar
        Scanner scanner = new Scanner(rg);

        //test constructing the regex tree
        System.out.println(scanner.constructRegexTrees().toString());

        //test constructing the NFA
        System.out.println("Show the NFA:");
        TNFA nfa = scanner.constructNFA();
        System.out.println(nfa.toString());

        //test constructing the DFA
        System.out.println("Show the DFA:");
        RDFA dfa = scanner.constructDFA(nfa);
        System.out.println(dfa.StateMappingBetweenDFAAndNFAToString());
        System.out.println(dfa.toString());

        //test minimizing the DFA
        System.out.println("Show the miniDFA:");
        State.STATE_ID = 0;
        System.out.println(scanner.minimizeDFA(dfa).toString());

        long endTime = System.currentTimeMillis();

        // Output the time taken
        System.out.println("运行时间: " + (endTime - startTime) + " 毫秒");
    }
}