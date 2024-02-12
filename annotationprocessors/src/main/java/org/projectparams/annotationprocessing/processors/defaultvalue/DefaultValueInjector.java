package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation.MethodInvocationExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultValueInjector {
    private final ExecutableElement invocable;
    private final InvocableInfo invocableInfo;
    public static Messager messager;

    public DefaultValueInjector(InvocableInfo invocableInfo) {
        this.invocableInfo = invocableInfo;
        this.invocable = invocableInfo.method();
    }

    public void inject() {
        var statementsToInject = new ArrayList<StatementTree>();
        for (var param : invocableInfo.parameters()) {
            var defaultValue = param.defaultValue();
            if (defaultValue == null) {
                continue;
            }
            var expression = ExpressionFactory.createExpression(
                    new CreateExpressionParams(
                            defaultValue.expression(),
                            TypeUtils.getUnboxedTypeTag(defaultValue.type()),
                            PathUtils.getElementPath(invocable)
                    ));
            if (expression instanceof LiteralExpression) {
                continue;
            }
            expression.convertInnerIdentifiersToQualified(ClassContext.of(PathUtils
                    .getElementPath(invocableInfo.method().getEnclosingElement())));
            var expressionAsJC = expression.toJcExpression();
            statementsToInject.add(assignToVar(wrapInNonNullElse(expressionAsJC, param.name()), param.name()));
        }
        var element = PathUtils.getElementPath(invocable).getLeaf();
        if (element instanceof MethodTree methodTree) {
            modifyMethodBody(methodTree, statementsToInject.toArray(StatementTree[]::new));
        } else {
            throw new IllegalStateException("Unexpected element type: " + element.getClass());
        }

    }

    private static List<Tree> modifyConstructorBody(MethodTree methodTree, StatementTree ...statementsToInject) {
        var prevStatements = methodTree.getBody().getStatements();
        var newStatements = new ArrayList<Tree>();
        if (callsOtherConstructor(prevStatements)) {
            var constrCall = (JCTree.JCMethodInvocation) ((JCTree.JCExpressionStatement) prevStatements.getFirst()).getExpression();
            for (var statement : statementsToInject) {
                var varName = ((JCTree.JCVariableDecl) statement).name.toString();
                constrCall.args.stream().filter(arg -> arg instanceof JCTree.JCIdent ident
                        && ident.name.toString().equals(varName))
                        .findFirst()
                        .ifPresent(arg ->
                                constrCall.args.set(constrCall.args.indexOf(arg), (JCTree.JCExpression) ((VariableTree) statement).getInitializer()));
            }
            newStatements.add(constrCall);
            prevStatements = prevStatements.subList(1, prevStatements.size());
        }
        newStatements.addAll(Arrays.asList(statementsToInject));
        newStatements.addAll(prevStatements);
        return newStatements;
    }

    private static boolean callsOtherConstructor(List<? extends Tree> prevStatements) {
        if (!prevStatements.isEmpty()) {
            var firstStatement = prevStatements.getFirst();
            if (firstStatement instanceof JCTree.JCExpressionStatement statement
            && statement.getExpression() instanceof JCTree.JCMethodInvocation methodInvocation) {
                return methodInvocation.getMethodSelect() instanceof IdentifierTree identifierTree
                        && (identifierTree.getName().contentEquals("this") || identifierTree.getName().contentEquals("super"));
            }
        }
        return false;
    }

    private static void modifyMethodBody(MethodTree methodTree, StatementTree ...statementsToInject) {
        List<Tree> newStatements = new ArrayList<>();
        if (methodTree.getName().contentEquals("<init>")) {
            newStatements = modifyConstructorBody(methodTree, statementsToInject);
        } else {
            newStatements.addAll(Arrays.asList(statementsToInject));
            newStatements.addAll(methodTree.getBody().getStatements());
        }
        var asJC = (JCTree.JCMethodDecl) methodTree;
        asJC.body = ExpressionMaker.makeBlock(newStatements.stream().map(JCTree.JCStatement.class::cast).toList());
        messager.printMessage(javax.tools.Diagnostic.Kind.NOTE, "Modified tree: " + asJC);
    }

    private StatementTree assignToVar(JCTree.JCExpression expression, String varName) {
        return ExpressionMaker.makeAssignment(
                ExpressionMaker.makeIdent(varName),
                expression);
    }

    private JCTree.JCExpression wrapInNonNullElse(JCTree.JCExpression expression, String varName) {
        return ExpressionMaker.makeMethodInvocation(
                ExpressionMaker.makeFieldAccess(
                        ExpressionMaker.makeIdent("java.util.Objects"),
                        "requireNonNullElse"),
                Collections.emptyList(),
                List.of(ExpressionMaker.makeIdent(varName), expression)
                        .toArray(JCTree.JCExpression[]::new));
    }
}
