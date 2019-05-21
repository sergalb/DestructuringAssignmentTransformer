package main;

import jdk.nashorn.api.tree.*;

import java.util.*;

public class DestructuringAssignmentVisitor implements TreeVisitor<StringBuilder, Boolean> {
    private final String LINE_SEPARATOR = "\n";
    private Map<String, FunctionProperties> changedFunction;
    public DestructuringAssignmentVisitor() {
        changedFunction = new HashMap<>();
    }

    @Override
    public StringBuilder visitCompilationUnit(CompilationUnitTree node, Boolean r) {
        StringBuilder result = new StringBuilder();
        node.getSourceElements().forEach((tree) -> {
            result.append(tree.accept(this, false));
        });
        result.append(printNewFunctions());
        return result;
    }

    @Override
    public StringBuilder visitVariable(VariableTree node, Boolean r) {
        StringBuilder variableCode = new StringBuilder("var ");
        variableCode.append(node.getBinding().accept(this, r));
        variableCode.append(" = ");
        variableCode.append(node.getInitializer().accept(this, r));
        variableCode.append(";").append(LINE_SEPARATOR);
        return variableCode;
    }

    @Override
    public StringBuilder visitBlock(BlockTree node, Boolean r) {
        StringBuilder blockCode = new StringBuilder("{").append(LINE_SEPARATOR);
        node.getStatements().forEach((tree) -> {
            blockCode.append(tree.accept(this, r));
        });
        blockCode.append("}").append(LINE_SEPARATOR);
        return blockCode;
    }

    @Override
    public StringBuilder visitExpressionStatement(ExpressionStatementTree node, Boolean r) {
        return node.getExpression().accept(this, r).append(";").append(LINE_SEPARATOR);
    }

    @Override
    public StringBuilder visitAssignment(AssignmentTree node, Boolean r) {
        StringBuilder assigmentCode = node.getVariable().accept(this, r);
        assigmentCode.append(" = ");
        assigmentCode.append(node.getExpression().accept(this, r));
        return assigmentCode;
    }

    @Override
    public StringBuilder visitBinary(BinaryTree node, Boolean r) {
        StringBuilder binaryCode = new StringBuilder("(");
        binaryCode.append(node.getLeftOperand().accept(this, r)).append(")");
        StringBuilder operator = new StringBuilder();
        switch (node.getKind()) {
            case PLUS:
                operator.append(" + ");
                break;
            case MINUS:
                operator.append(" - ");
                break;
            case AND:
                operator.append(" & ");
                break;
            case COMMA:
                operator.append(" , ");
                break;
            case CONDITIONAL_AND:
                operator.append(" && ");
                break;
            case CONDITIONAL_OR:
                operator.append(" || ");
                break;
            case DIVIDE:
                operator.append(" / ");
                break;
            case EQUAL_TO:
                operator.append(" == ");
                break;
            case GREATER_THAN:
                operator.append(" > ");
                break;
            case GREATER_THAN_EQUAL:
                operator.append(" >= ");
                break;
            case IN:
                operator.append(" in ");
                break;
            case LESS_THAN:
                operator.append(" < ");
                break;
            case LESS_THAN_EQUAL:
                operator.append(" <= ");
                break;
            case MULTIPLY:
                operator.append(" * ");
                break;
            case NOT_EQUAL_TO:
                operator.append(" != ");
                break;
            case OR:
                operator.append(" | ");
                break;
            case REMAINDER:
                operator.append(" % ");
                break;
            case RIGHT_SHIFT:
                operator.append(" >> ");
                break;
            case STRICT_EQUAL_TO:
                operator.append(" === ");
                break;
            case STRICT_NOT_EQUAL_TO:
                operator.append(" !== ");
                break;
            case UNSIGNED_RIGHT_SHIFT:
                operator.append(" >>> ");
                break;
            case XOR:
                operator.append(" ^ ");
                break;
            default:
                throw new IllegalArgumentException("Binary operator \"" + node.getKind() + "\" doesn't supported");
        }
        binaryCode.append(operator).append("(");
        binaryCode.append(node.getRightOperand().accept(this, r)).append(")");
        return binaryCode;
    }

    @Override
    public StringBuilder visitFunctionDeclaration(FunctionDeclarationTree node, Boolean isNestedFunction) {
        String functionName = node.getName().accept(this, isNestedFunction).toString();
        StringBuilder functionBody = node.getBody().accept(this, true);
        List<String> oldArguments = getListArguments(node.getParameters(), isNestedFunction);

        StringBuilder functionDeclaration = new StringBuilder("function ");
        if (!isNestedFunction) {
            functionDeclaration.append(functionName);
            functionDeclaration.append("(");
            functionDeclaration.append(printArguments(oldArguments));
            functionDeclaration.append(") ");
            functionDeclaration.append(functionBody);
            return functionDeclaration;
        } else {
            List<String> internalUsedVariables = new ArrayList<>();
            node.accept(new CollectorIdentifiersVisitor(), new Pair(internalUsedVariables, false));
            changedFunction.put(functionName, new FunctionProperties(internalUsedVariables, functionBody));
            return new StringBuilder();
        }
    }

    @Override
    public StringBuilder visitFunctionCall(FunctionCallTree node, Boolean r) {
        StringBuilder functionName = node.getFunctionSelect().accept(this, r);
        StringBuilder functionCallCode = new StringBuilder(functionName).append("(");
        List<String> requiredArguments = getListArguments(node.getArguments(), r);
        functionCallCode.append(printArguments(requiredArguments));
        if (changedFunction.containsKey(functionName.toString())) {
            List<String> newArguments = changedFunction.get(functionName.toString()).getArguments();
            List<String> additionalArguments = newArguments.subList(requiredArguments.size(), newArguments.size());
            if (!requiredArguments.isEmpty() && !additionalArguments.isEmpty()) {
                functionCallCode.append(", ");
            }
            functionCallCode.append(printArguments(additionalArguments));
        }
        functionCallCode.append(")");
        return functionCallCode;
    }

    @Override
    public StringBuilder visitIdentifier(IdentifierTree node, Boolean r) {
        return new StringBuilder(node.getName());
    }

    @Override
    public StringBuilder visitLiteral(LiteralTree node, Boolean r) {
        return new StringBuilder(node.getValue().toString());
    }

    @Override
    public StringBuilder visitParenthesized(ParenthesizedTree node, Boolean r) {
        StringBuilder parenthesizedCode = new StringBuilder("(");
        parenthesizedCode.append(node.getExpression().accept(this, r));
        parenthesizedCode.append(")");
        return parenthesizedCode;
    }

    @Override
    public StringBuilder visitReturn(ReturnTree node, Boolean r) {
        StringBuilder returnCode = new StringBuilder("return ");
        returnCode.append(node.getExpression().accept(this, r));
        returnCode.append(";").append(LINE_SEPARATOR);
        return returnCode;
    }

    @Override
    public StringBuilder visitUnary(UnaryTree node, Boolean r) {
        boolean isPrefixOperator = true;
        StringBuilder operator = new StringBuilder();
        switch (node.getKind()){
            case BITWISE_COMPLEMENT:
                operator.append(" ~ ");
                break;
            case DELETE:
                operator.append("delete  ");
                break;
            case LOGICAL_COMPLEMENT:
                operator.append("!");
                break;
            case POSTFIX_DECREMENT:
                operator.append("--");
                isPrefixOperator = false;
                break;
            case POSTFIX_INCREMENT:
                operator.append("++");
                isPrefixOperator = false;
                break;
            case PREFIX_DECREMENT:
                operator.append("--");
                break;
            case PREFIX_INCREMENT:
                operator.append("++");
                break;
            case TYPEOF:
                operator.append("typeof ");
                break;
            case UNARY_MINUS:
                operator.append("-");
                break;
            case UNARY_PLUS:
                operator.append("+");
                break;
            case VOID:
                operator.append("void ");
                break;
            default:
                throw new IllegalArgumentException("Unary operator \"" + node.getKind() + "\" doesn't supported");
        }

        StringBuilder unaryCode = new StringBuilder();
        if (isPrefixOperator) {
            unaryCode.append(operator);
            unaryCode.append(node.getExpression().accept(this, r));
        } else {
            unaryCode.append(node.getExpression().accept(this, r));
            unaryCode.append(operator);
        }
        return unaryCode;
    }

    private StringBuilder printArguments(List<String> arguments) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < arguments.size(); ++i) {
            result.append(arguments.get(i));
            if (i < arguments.size() - 1) {
                result.append(", ");
            }
        }
        return result;
    }

    private List<String> getListArguments(List<? extends ExpressionTree> arguments, Boolean r) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < arguments.size(); ++i) {
            result.add(arguments.get(i).accept(this, r).toString());
        }
        return result;
    }

    private StringBuilder printNewFunctions() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, FunctionProperties> newFunction: changedFunction.entrySet()) {
            result.append("function ").append(newFunction.getKey()).append("(");
            FunctionProperties functionProperties = newFunction.getValue();
            result.append(printArguments(functionProperties.getArguments()));
            result.append(") ").append(functionProperties.getBody());
        }
        return result;
    }

    public class FunctionProperties {
        private List<String> arguments;
        private StringBuilder body;

        public FunctionProperties(List<String> arguments, StringBuilder body) {
            this.arguments = arguments;
            this.body = body;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public StringBuilder getBody() {
            return body;
        }
    }


    //throw exceptions from unsupported methods
    @Override
    public StringBuilder visitCompoundAssignment(CompoundAssignmentTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitBreak(BreakTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitCase(CaseTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitCatch(CatchTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitClassDeclaration(ClassDeclarationTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitClassExpression(ClassExpressionTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitConditionalExpression(ConditionalExpressionTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitContinue(ContinueTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitDebugger(DebuggerTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitDoWhileLoop(DoWhileLoopTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitErroneous(ErroneousTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitForLoop(ForLoopTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitForInLoop(ForInLoopTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitForOfLoop(ForOfLoopTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitFunctionExpression(FunctionExpressionTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitIf(IfTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitArrayAccess(ArrayAccessTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitArrayLiteral(ArrayLiteralTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitLabeledStatement(LabeledStatementTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitMemberSelect(MemberSelectTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitNew(NewTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitObjectLiteral(ObjectLiteralTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitProperty(PropertyTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitRegExpLiteral(RegExpLiteralTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitTemplateLiteral(TemplateLiteralTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitEmptyStatement(EmptyStatementTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitSpread(SpreadTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitSwitch(SwitchTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitThrow(ThrowTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitModule(ModuleTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitExportEntry(ExportEntryTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitImportEntry(ImportEntryTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitTry(TryTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitInstanceOf(InstanceOfTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitWhileLoop(WhileLoopTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitWith(WithTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitYield(YieldTree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }

    @Override
    public StringBuilder visitUnknown(Tree node, Boolean aBoolean) {
        throw new UnsupportedStatementException("doesnt support statement " +node.getKind().toString());
    }
}
