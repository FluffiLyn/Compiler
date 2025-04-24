package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.FiniteAutomaton;
import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.HashMap;

public class RDFA extends FiniteAutomaton {

    /**
     * holds the maps between DFA states and NFA state sets
     */
    private HashMap<State, HashMap<Integer, State>> StateMappingBetweenDFAAndNFA = new HashMap<>();

    public RDFA() {
        super();
        this.StateMappingBetweenDFAAndNFA = new HashMap<>();
        //this.transitTable = new LabeledDirectedGraph<>(); // No! This will override the transitTable and lead to wrong output
    }

    public RDFA(State startState) {
        this.startState = startState;
        this.StateMappingBetweenDFAAndNFA = new HashMap<>();
        this.transitTable = new LabeledDirectedGraph<>();
        this.getTransitTable().addVertex(this.startState);
    }

    public void setStateMappingBetweenDFAAndNFA(State s, HashMap<Integer, State> nfaStates) {
        this.StateMappingBetweenDFAAndNFA.put(s, nfaStates);
    }

    public HashMap<State, HashMap<Integer, State>> getStateMappingBetweenDFAAndNFA() {
        return StateMappingBetweenDFAAndNFA;
    }

    public String StateMappingBetweenDFAAndNFAToString() {
        StringBuilder str = new StringBuilder();
        int d = 0;
        for (State s : this.getStateMappingBetweenDFAAndNFA().keySet()) {
            d++;
            String mapping = "";
            for (State ns : this.getStateMappingBetweenDFAAndNFA().get(s).values()) {
                mapping += ns.getSid() + ",";
            }
            mapping = mapping.substring(0, mapping.length() - 1);
            mapping = "DFA State:" + s.toString() + "\tNFA State set:\t{" + mapping + "}" + "\n";
            str.append(mapping);
        }
        return str.toString();
    }
}
