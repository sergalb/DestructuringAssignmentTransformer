package main;

import jdk.nashorn.api.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectorIdentifiersVisitor extends SimpleTreeVisitorES6<Void, Pair> {
    private List<Set<String>> ownVariables;
    private Set<String> putedVariables;
    CollectorIdentifiersVisitor() {
        super();
        ownVariables = new ArrayList<>();
        putedVariables = new HashSet<>();
        ownVariables.add(new HashSet<>());
    }

    @Override
    public Void visitVariable(VariableTree node, Pair r) {
        node.getBinding().accept(this, new Pair(r.getVariables(), true));
        node.getInitializer().accept(this, r);
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Pair r) {
        String name = node.getName();
        if (r.getFromVariableTree()) {
            ownVariables.get(ownVariables.size() - 1).add(name);
        } else if (!putedVariables.contains(name)){
            boolean isInternalVariable = false;
            for (Set set : ownVariables) {
                if (set.contains(name)) {
                    isInternalVariable = true;
                    break;
                }
            }
            if (!isInternalVariable) {
                r.getVariables().add(name);
                putedVariables.add(name);
            }
        }
        return null;
    }

    @Override
    public Void visitBlock(BlockTree node, Pair r) {
        ownVariables.add(new HashSet<>());
        node.getStatements().forEach((tree) -> {
            tree.accept(this, r);
        });
        ownVariables.remove(ownVariables.size() - 1);
        return null;
    }


    //pass function name identifier
    @Override
    public Void visitFunctionCall(FunctionCallTree node, Pair r) {
        node.getArguments().forEach((tree) -> {
            tree.accept(this, r);
        });
        return null;
    }

    @Override
    public Void visitFunctionDeclaration(FunctionDeclarationTree node, Pair r) {
        node.getParameters().forEach((tree) -> {
            tree.accept(this, new Pair(r.getVariables(), true));
        });
        node.getBody().accept(this, r);
        return null;
    }
}