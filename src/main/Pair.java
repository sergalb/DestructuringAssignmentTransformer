package main;

import java.util.List;

public class Pair {
    private List<String> variables;
    private Boolean fromVariableTree;

    public Pair(List<String> variables, Boolean fromVariableTree) {
        this.variables = variables;
        this.fromVariableTree = fromVariableTree;
    }

    public List<String> getVariables() {
        return variables;
    }

    public Boolean getFromVariableTree() {
        return fromVariableTree;
    }
}
