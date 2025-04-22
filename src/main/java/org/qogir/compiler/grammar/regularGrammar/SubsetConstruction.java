package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabelEdge;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.*;

/**
 * The subset construction Algorithm for converting an NFA to a DFA.
 * The subset construction Algorithm takes an NFA N as input and output a DFA D accepting the same language as N.
 * The main mission is to eliminate ε-transitions and multi-transitions in NFA and construct a transition table for D.
 * The algorithm can be referred to {@see }
 */
public class SubsetConstruction {

    /**
     * Eliminate all ε-transitions reachable from a single state in NFA through the epsilon closure operation.
     *
     * @param s  a single state of NFA
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state s on ε-transition
     * @author xuyang
     */
    private HashMap<Integer, State> epsilonClosures(State s, LabeledDirectedGraph<State> tb) {
        if (!tb.vertexSet().contains(s)) { //if vertex s not in the transition table
            return null;
        }

        HashMap<Integer, State> nfaStates = new HashMap<>();

        //Record the state that has been reached by ε-transitions
        Stack<State> stateStack = new Stack<>();

        //Start
        stateStack.push(s);
        //Add new state to nfaStates
        while (!stateStack.isEmpty()) {
            State current_state = stateStack.pop();
            nfaStates.put(current_state.getId(), current_state);
            for (LabelEdge edge : tb.edgeSet()) {
                /*
                 * When matching these 3 conditions:
                 * 1. The source state of the edge in the transition table matches the current analysis state.
                 * 2. The transition symbol of the current edge is 'ε'.
                 * 3. The target state of this edge is not yet in nfaStates.
                 *
                 * Add the target state of this edge to the stateStack,
                 * and in the next loop, continue to compute the ε-closure for this target state.
                 */
                if (tb.getEdgeSource(edge).equals(current_state) && edge.getLabel() == 'ε' && !nfaStates.containsKey(tb.getEdgeTarget(edge).getId()))
                    stateStack.push(tb.getEdgeTarget(edge));
            }
        }
        return nfaStates;
    }

    /**
     * ε-closure(ss)
     * Eliminate all ε-transitions reachable from a state set in NFA through the epsilon closure operation
     *
     * @param ss a state set of NFA
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state set on ε-transition
     * @author xuyang
     */
    public HashMap<Integer, State> epsilonClosure(HashMap<Integer, State> ss, LabeledDirectedGraph<State> tb) {
        HashMap<Integer, State> nfaStates = new HashMap<>();
        for (State s : ss.values()) {
            nfaStates.putAll(epsilonClosures(s, tb));
        }
        return nfaStates;
    }

    /**
     * moves(s,ch)
     *
     * @param s a single state of NFA
     * @param ch the transition symbol
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state s on ch-transition
     */
    private HashMap<Integer, State> moves(State s, Character ch, LabeledDirectedGraph<State> tb) {
        HashMap<Integer, State> nfaStates = new HashMap<>();
        //Find the target state of the corresponding edge
        for (LabelEdge edge : tb.edgeSet()) {
            if (tb.getEdgeSource(edge).equals(s) && edge.getLabel() == ch)
                nfaStates.put(tb.getEdgeTarget(edge).getId(), tb.getEdgeTarget(edge));
        }
        return nfaStates;
    }

    /**
     * move(ss,ch)
     * Apply moves(s,ch) to each state in ss
     *
     * @param ss a state set of NFA
     * @param ch the transition symbol
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state set on ch-transition
     */
    public HashMap<Integer, State> move(HashMap<Integer, State> ss, Character ch, LabeledDirectedGraph<State> tb) {
        HashMap<Integer, State> nfaStates = new HashMap<>();
        for (State s : ss.values()) {
            nfaStates.putAll(moves(s, ch, tb));
        }
        return nfaStates;
    }

    /**
     * move(ss,ch) then apply ε-transitions
     * Apply moves(s,ch) to each state in ss and then apply epsilonClosure
     *
     * @param ss a state set of NFA
     * @param ch the transition symbol
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state set on ch-transition and ε-transition
     */
    public HashMap<Integer, State> epsilonClosureWithMove(HashMap<Integer, State> ss, Character ch, LabeledDirectedGraph<State> tb) {
        return new HashMap<>(epsilonClosure(move(ss, ch, tb), tb));
    }


    /*
     * Initially, ε-closure(start) is the only state in Dstates, and it's unmarked
     * while (exists an unmarked state T in Dstates)
     * {
     *     mark T；
     *     for (every input character ch)
     *     {
     *         U = ε-closure(move(T,ch));
     *         if (U isn't in Dstates)
     *             add U to Dstates, and don't mark it；
     *         Dtran[T,ch] = U;
     *     }
     * }
     */
    /**
     * The main function of the subset construction algorithm.
     *
     * @param tnfa the original TNFA
     * @return a DFA that accepts the same language as the original NFA
     */
    public RDFA subSetConstruct(TNFA tnfa) {
        RDFA dfa = new RDFA();
        LabeledDirectedGraph<State> tb = tnfa.getTransitTable();
        HashMap<State, HashMap<Integer, State>> dfa_map = dfa.getStateMappingBetweenDFAAndNFA();
        ArrayList<Character> alphabet = tnfa.getAlphabet();

        // Start state and its ε-closure
        HashMap<Integer, State> start_states_epsilon_closure = epsilonClosures(tnfa.getStartState(), tb);

        // If the accepting state of the NFA is reachable from the initial state of the NFA through ε-transitions,
        // then the initial state of the DFA should also be an accepting state
        State startState = dfa.getStartState();
        if (start_states_epsilon_closure != null && start_states_epsilon_closure.containsKey(tnfa.getAcceptingState().getId())) {
            startState.setType(2);
        }

        dfa.setStateMappingBetweenDFAAndNFA(startState, start_states_epsilon_closure);

        // Queue for storing initial DFA states
        // Initially, ε-closure(start) is the only state in Dstates and it's unmarked
        Queue<HashMap<Integer, State>> Dstates = new LinkedList<>();
        Dstates.add(start_states_epsilon_closure);

        // While there exists an unmarked state in Dstates
        while (!Dstates.isEmpty()) {
            // Dequeue the unprocessed state
            HashMap<Integer, State> current_nfa_set = Dstates.poll();
            State current_state = null;

            // Find the corresponding DFA state for the current NFA state set
            for (Map.Entry<State, HashMap<Integer, State>> entry : dfa_map.entrySet()) {
                if (entry.getValue().equals(current_nfa_set)) {
                    current_state = entry.getKey();
                    break;
                }
            }

            // For any input character
            for (Character ch : alphabet) {
                // U = ε-closure(move(T, ch)), the epsilon closure of move(T, ch)
                HashMap<Integer, State> U = epsilonClosureWithMove(current_nfa_set, ch, tb);

                // If the state set is empty, continue to the next character
                if (U.isEmpty()) {
                    continue;
                }

                // Determine if U is already in Dstates
                State T = null;
                boolean is_in = false;
                for (Map.Entry<State, HashMap<Integer, State>> entry : dfa_map.entrySet()) {
                    if (entry.getValue().equals(U)) {
                        T = entry.getKey();
                        is_in = true;
                        break;
                    }
                }

                // If U is not in Dstates, add it to Dstates, don't mark it, and put it into DTran[T, ch]
                if (!is_in) {
                    T = new State();
                    T.setType(U.containsKey(tnfa.getAcceptingState().getId()) ? 2 : 1);
                    dfa.getTransitTable().addVertex(T);
                    dfa_map.put(T, U);
                    Dstates.add(U);
                }

                // Add the transition to the DFA transition table
                dfa.getTransitTable().addEdge(current_state, T, ch);
            }
        }

        return dfa;
    }
}
