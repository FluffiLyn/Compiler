package org.qogir.compiler.grammar.regularGrammar;


import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabelEdge;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StateMinimization {

    /*
     * 1. Start with an initial partition `P` with two groups:
     *   - `F`: The set of accepting states.
     *   - `S`: The set of non-accepting states.
     *
     * 2. Loop:
     *    Construct P_new by refining the groups in `P`.
     *    For each group `G` in `P`, split `G` into subgroups `G1, G2, ..., Gn` such that:
     *      Two states `s` and `t` belong to the same subgroup if and only if, for every input symbol `a`,
     *      the states they transition to on `a` belong to the same group in the current partition `P`.
     *    Replace `G` in `P_new` with the subgroups `G1, G2, ..., Gn`.
     *
     * 3. If `P_new` == `P` (i.e., no further refinement is possible), algorithm terminates.
     *    Otherwise, set `P = P_new` and repeat step 2.
     *
     * 4. Final Partition `P_Final` contains the equivalence classes.
     *    Each group in `P_Final` represents a set of equivalent states.
     */

    /**
     * Distinguish non-equivalent states in the given DFA.
     *
     * @param dfa the original dfa.
     * @return distinguished equivalent state groups
     */
    private HashMap<Integer, HashMap<Integer, State>> distinguishEquivalentState(RDFA dfa) {
        // Initialize the step queue to record partitioning steps
        ArrayDeque<String> stepQueue = new ArrayDeque<>();

        // Initialize the partition with accepting and non-accepting states
        HashMap<Integer, HashMap<Integer, State>> P = new HashMap<>();
        HashMap<Integer, State> F = new HashMap<>();
        HashMap<Integer, State> S = new HashMap<>();

        for (State s : dfa.getTransitTable().vertexSet()) {
            if (s.getType() == 2) {
                F.put(s.getId(), s); // Accepting states
            } else {
                S.put(s.getId(), s); // Non-accepting states
            }
        }

        if (!F.isEmpty()) P.put(0, F);
        if (!S.isEmpty()) P.put(P.size(), S);

        // Record the initial partition
        recordDistinguishSteps(stepQueue, P, "Initial Partition");

        ArrayList<Character> a = dfa.getAlphabet();
        LabeledDirectedGraph<State> transitionTable = dfa.getTransitTable();

        boolean changed;
        do {
            changed = false;
            HashMap<Integer, HashMap<Integer, State>> P_new = new HashMap<>();

            for (Map.Entry<Integer, HashMap<Integer, State>> G_entry : P.entrySet()) {
                HashMap<Integer, State> G = G_entry.getValue();

                if (G.size() == 1) {
                    P_new.put(P_new.size(), G); // Groups with a single state remain unchanged
                    continue;
                }

                HashMap<Integer, HashMap<Integer, State>> subgroups = new HashMap<>();
                for (State s : G.values()) {
                    int splitKey = getSplitKey(s, a, P, transitionTable);
                    subgroups.computeIfAbsent(splitKey, k -> new HashMap<>()).put(s.getId(), s);
                }

                if (subgroups.size() > 1) changed = true; // Refinement occurred
                P_new.putAll(subgroups);
            }

            P = P_new;

            // Record the refined partition
            recordDistinguishSteps(stepQueue, P, "Refined Partition");
        } while (changed);

        // Output the partitioning steps
        showDistinguishSteps(stepQueue);

        return P;
    }

    private int groupCounter = 0;

    /**
     * Generate a unique key for a state based on its transitions and the current group set.
     * This key is used to determine which subgroup the state belongs to.
     *
     * @param state           the state to generate the key for.
     * @param alphabet        the DFA's input alphabet.
     * @param groupSet        the current group set.
     * @param transitionTable the DFA's transition table.
     * @return an integer identifying the group the state belongs to.
     */
    private int getSplitKey(State state, ArrayList<Character> alphabet,
                            HashMap<Integer, HashMap<Integer, State>> groupSet,
                            LabeledDirectedGraph<State> transitionTable) {
        StringBuilder keyBuilder = new StringBuilder();
        for (Character ch : alphabet) {
            State targetState = null;
            // Find the target state for ch
            for (LabelEdge edge : transitionTable.edgeSet()) {
                if (edge.getSource().equals(state) && edge.getLabel().equals(ch)) {
                    targetState = (State) edge.getTarget();
                    break;
                }
            }
            if (targetState != null) {
                // Ensure the target state is in the group set
                for (Map.Entry<Integer, HashMap<Integer, State>> groupEntry : groupSet.entrySet()) {
                    if (groupEntry.getValue().containsKey(targetState.getId())) {
                        keyBuilder.append(groupEntry.getKey()).append(",");
                        break;
                    }
                }
            } else {
                keyBuilder.append("-1,");
            }
        }

        // check if the key already exists in the group set
        String keyString = keyBuilder.toString();
        for (Map.Entry<Integer, HashMap<Integer, State>> groupEntry : groupSet.entrySet()) {
            if (groupEntry.getValue().containsKey(state.getId())) {
                return groupEntry.getKey();
            }
        }

        // if the group is new, assign a new identifier
        return groupCounter++;
    }

    /**
     * Minimize the given DFA.
     * Choose one state from each group in `P_Final` as the representative for that group.
     * These representative states form the states of the minimized DFA.
     *
     * @param dfa the original dfa.
     * @return the minimized DFA.
     */
    public RDFA minimize(RDFA dfa) {
        if (dfa == null) return null;

        // Step 1: Get equivalent state groups
        HashMap<Integer, HashMap<Integer, State>> groupSet = distinguishEquivalentState(dfa);

        // Step 2: Create the minimized DFA
        RDFA minimizedDFA = new RDFA();
        HashMap<Integer, State> groupIdToState = new HashMap<>();
        HashMap<State, State> stateMapping = new HashMap<>();

        // Step 3: Create states for each group
        for (Map.Entry<Integer, HashMap<Integer, State>> group : groupSet.entrySet()) {
            State representative = group.getValue().values().iterator().next();
            State newState;

            if (representative.getType() == 0) { // Initial state
                newState = minimizedDFA.getStartState();
            } else {
                newState = new State();
                newState.setType(representative.getType());
                minimizedDFA.getTransitTable().addVertex(newState);
            }

            groupIdToState.put(group.getKey(), representative);
            stateMapping.put(representative, newState);
        }

        // Step 4: Add transitions
        for (Map.Entry<Integer, State> entry : groupIdToState.entrySet()) {
            State sourceState = entry.getValue();
            for (LabelEdge edge : dfa.getTransitTable().edgeSet()) {
                if (edge.getSource().equals(sourceState)) {
                    State targetState = (State) edge.getTarget();
                    for (Map.Entry<Integer, HashMap<Integer, State>> group : groupSet.entrySet()) {
                        if (group.getValue().containsKey(targetState.getId())) {
                            State minimizedSource = stateMapping.get(sourceState);
                            State minimizedTarget = stateMapping.get(groupIdToState.get(group.getKey()));
                            minimizedDFA.getTransitTable().addEdge(new LabelEdge(minimizedSource, minimizedTarget, edge.getLabel()));
                            break;
                        }
                    }
                }
            }
        }

        return minimizedDFA;
    }

    /**
     * Used for showing the distinguishing process of state miminization algorithm
     *
     * @param stepQueue holds all distinguishing steps
     * @param GroupSet  is the set of equivalent state groups
     * @param memo      remarks
     */
    private void recordDistinguishSteps(ArrayDeque<String> stepQueue, HashMap<Integer, HashMap<Integer, State>> GroupSet, String memo) {
        String str = "";
        str = GroupSetToString(GroupSet);
        str += ":" + memo;
        stepQueue.add(str);
    }

    /**
     * Display the equivalent state groups
     *
     * @param stepQueue holds all distinguishing steps
     */
    private void showDistinguishSteps(ArrayDeque<String> stepQueue) {
        int step = 0;
        String str = "";
        while (!stepQueue.isEmpty()) {
            str = stepQueue.poll();
            System.out.println("Step" + step++ + ":\t" + str + "\r");
        }
    }

    private String GroupSetToString(HashMap<Integer, HashMap<Integer, State>> GroupSet) {
        StringBuilder str = new StringBuilder();
        for (Integer g : GroupSet.keySet()) {
            String tmp = GroupToString(GroupSet.get(g));
            str.append(g).append(":").append(tmp).append("\t");
        }
        return str.toString();
    }

    private String GroupToString(HashMap<Integer, State> group) {
        StringBuilder str = new StringBuilder();
        for (Integer k : group.keySet()) {
            str.append(group.get(k).getId()).append(":").append(group.get(k).getType()).append(",");
        }
        if (!str.isEmpty()) str = new StringBuilder(str.substring(0, str.length() - 1));
        str = new StringBuilder("{" + str + "}");
        return str.toString();
    }
}
