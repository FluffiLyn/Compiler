package org.qogir.compiler.grammar.regularGrammar;


import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabelEdge;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.*;

public class StateMinimization {

    /*
     * 1. Start with an initial partition `P` with two groups:
     *   - `A`: The group of accepting states.
     *   - `NA`: The group non-accepting states.
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
        // Queue for recording distinguish steps
        ArrayDeque<String> stepQueue = new ArrayDeque<>();

        // Initial partition P
        HashMap<Integer, HashMap<Integer, State>> P = new HashMap<>();

        // Accepting states and non-accepting states
        HashMap<Integer, State> A = new HashMap<>();
        HashMap<Integer, State> NA = new HashMap<>();

        // Init A and NA
        for (State s : dfa.getTransitTable().vertexSet()) {
            if (s.getType() == 2)
                A.put(s.getId(), s);
            else
                NA.put(s.getId(), s);
        }

        // Add the initial groups to P
        if (!A.isEmpty())
            P.put(0, A);
        if (!NA.isEmpty())
            P.put(1, NA);

        recordDistinguishSteps(stepQueue, P, "Initial Partition");

        // DFA alphabet and transition table
        ArrayList<Character> alphabet = dfa.getAlphabet();
        LabeledDirectedGraph<State> tb = dfa.getTransitTable();

        // Construct a map to track which group each state belongs to
        HashMap<Integer, Integer> stateToGroupMap = new HashMap<>();
        for (Map.Entry<Integer, HashMap<Integer, State>> entry : P.entrySet()) {
            for (Integer stateId : entry.getValue().keySet()) {
                stateToGroupMap.put(stateId, entry.getKey());
            }
        }

        // Loop until no more partitioning
        boolean change = true;
        while (change) {
            HashMap<Integer, HashMap<Integer, State>> P_new = new HashMap<>();
            change = false;

            for (Map.Entry<Integer, HashMap<Integer, State>> G : P.entrySet()) {
                HashMap<Integer, State> group = G.getValue();

                if (group.size() == 1) {
                    P_new.put(P_new.size(), group);
                    continue;
                }

                HashMap<Integer, HashMap<Integer, State>> G_new = new HashMap<>();

                for (Character a : alphabet) {
                    HashMap<Integer, HashMap<Integer, State>> G_split = new HashMap<>();

                    for (Map.Entry<Integer, State> s : group.entrySet()) {
                        boolean noTransition = true;

                        for (LabelEdge e : tb.edgeSet()) {
                            if (((State) e.getSource()).getId() == s.getKey() && e.getLabel() == a) {
                                int targetId = ((State) e.getTarget()).getId();
                                Integer targetGroup = stateToGroupMap.get(targetId);

                                G_split
                                        .computeIfAbsent(targetGroup != null ? targetGroup : -1, x -> new HashMap<>())
                                        .put(s.getKey(), s.getValue());
                                noTransition = false;
                                break;
                            }
                        }

                        if (noTransition) {
                            G_split.computeIfAbsent(-1, x -> new HashMap<>()).put(s.getKey(), s.getValue());
                        }
                    }

                    if (G_split.size() > 1) {
                        G_new = G_split;
                        change = true;
                        break;
                    } else {
                        G_new = G_split;
                    }
                }

                for (Map.Entry<Integer, HashMap<Integer, State>> subgroup : G_new.entrySet()) {
                    P_new.put(P_new.size(), subgroup.getValue());
                }
            }

            P = P_new;

            // Update stateToGroupMap
            stateToGroupMap.clear();
            for (Map.Entry<Integer, HashMap<Integer, State>> entry : P.entrySet()) {
                for (Integer stateId : entry.getValue().keySet()) {
                    stateToGroupMap.put(stateId, entry.getKey());
                }
            }

            recordDistinguishSteps(stepQueue, P, "Refined Partition");
        }

        showDistinguishSteps(stepQueue);

        return P;
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
