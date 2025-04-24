package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.tree.DefaultTreeNode;

/**
 * An implementation of the Thompson construction algorithm for converting a RegexTree into an NFA.
 * It takes a RegexTreeNode as input and returns a TNFA.
 *
 * @author FluffiLyn,
 * @author hatimi-master
 */
public class ThompsonConstruction {

    /**
     * The main function of the Thompson construction algorithm.
     *
     * @param node The root node of the regex tree.
     * @return A TNFA representing the regex.
     */
    public TNFA translate(RegexTreeNode node) {

        if (node == null)
            return null;

        TNFA tnfa = new TNFA();
        // Type 0: Any char
        if (node.getType() == 0) {
            // add edge
            tnfa.getTransitTable().addEdge(tnfa.getStartState(), tnfa.getAcceptingState(), node.getValue());
        }
        // Type 1：concatenation
        else if (node.getType() == 1) {
            // Construct NFA
            DefaultTreeNode left_node = node.getFirstChild();
            DefaultTreeNode right_node = node.getFirstChild().getNextSibling();
            TNFA left_NFA = translate((RegexTreeNode) left_node);
            TNFA right_NFA = translate((RegexTreeNode) right_node);
            int count = 0;
            while (right_node != null) {
                // Left accepting state connects to right start state
                tnfa.getTransitTable().addEdge(left_NFA.getAcceptingState(), right_NFA.getStartState(), 'ε');
                // Add transit table for left and right node
                tnfa.getTransitTable().merge(left_NFA.getTransitTable());
                tnfa.getTransitTable().merge(right_NFA.getTransitTable());

                if (count == 0) { //Connection between the first child and the second child
                    left_NFA.getStartState().setType(State.START);
                    left_NFA.getAcceptingState().setType(State.MIDDLE);
                    right_NFA.getStartState().setType(State.MIDDLE);
                    right_NFA.getAcceptingState().setType(State.ACCEPT);
                    tnfa.setStartState(left_NFA.getStartState());
                    tnfa.setAcceptingState(right_NFA.getAcceptingState());
                    count++;
                } else { //Connection between children except the first one
                    left_NFA.getStartState().setType(State.MIDDLE);
                    left_NFA.getAcceptingState().setType(State.MIDDLE);
                    right_NFA.getStartState().setType(State.MIDDLE);
                    right_NFA.getAcceptingState().setType(State.ACCEPT);
                    tnfa.setAcceptingState(right_NFA.getAcceptingState());
                }


                right_node = right_node.getNextSibling();
                left_NFA = right_NFA;
                right_NFA = translate((RegexTreeNode) right_node);
            }
        }
        // Type 2: union
        else if (node.getType() == 2) {
            // Construct new NFA
            DefaultTreeNode left_node = node.getFirstChild();
            DefaultTreeNode right_node = node.getFirstChild().getNextSibling();
            TNFA left_NFA = translate((RegexTreeNode) left_node);
            TNFA right_NFA = translate((RegexTreeNode) right_node);
            while (right_node != null) {
                // Connect NFAs
                tnfa.getTransitTable().addEdge(tnfa.getStartState(), left_NFA.getStartState(), 'ε');
                tnfa.getTransitTable().addEdge(tnfa.getStartState(), right_NFA.getStartState(), 'ε');
                tnfa.getTransitTable().addEdge(left_NFA.getAcceptingState(), tnfa.getAcceptingState(), 'ε');
                tnfa.getTransitTable().addEdge(right_NFA.getAcceptingState(), tnfa.getAcceptingState(), 'ε');
                // Change states
                left_NFA.getStartState().setType(State.MIDDLE);
                left_NFA.getAcceptingState().setType(State.MIDDLE);
                right_NFA.getStartState().setType(State.MIDDLE);
                right_NFA.getAcceptingState().setType(State.MIDDLE);
                // Update transit table
                tnfa.getTransitTable().merge(left_NFA.getTransitTable());
                tnfa.getTransitTable().merge(right_NFA.getTransitTable());

                left_NFA = right_NFA;
                right_node = right_node.getNextSibling();
                right_NFA = translate((RegexTreeNode) right_node);
            }
        }
        // Type 3: closure
        else if (node.getType() == 3) {
            // Construct new NFA
            TNFA closure_NFA = translate((RegexTreeNode) node.getFirstChild());
            closure_NFA.getTransitTable().addEdge(closure_NFA.getAcceptingState(), closure_NFA.getStartState(), 'ε');
            // Connect NFAs
            tnfa.getTransitTable().addEdge(tnfa.getStartState(), closure_NFA.getStartState(), 'ε');
            tnfa.getTransitTable().addEdge(closure_NFA.getAcceptingState(), tnfa.getAcceptingState(), 'ε');
            tnfa.getTransitTable().addEdge(tnfa.getStartState(), tnfa.getAcceptingState(), 'ε');
            // Merge transit tables
            tnfa.getTransitTable().merge(closure_NFA.getTransitTable());
            // Change states
            closure_NFA.getStartState().setType(State.MIDDLE);
            closure_NFA.getAcceptingState().setType(State.MIDDLE);
        }
        return tnfa;
    }
}
