package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.IdentifierExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.LiteralExpression;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
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
            var expression = ExpressionFactory.createExpression(defaultValue.expression(),
                    TypeUtils.getUnboxedTypeTag(defaultValue.type()),
                    PathUtils.getElementPath(invocable));
            if (expression instanceof LiteralExpression) {
                continue;
            }
            var root = expression.getRootOwner();
            if (root instanceof IdentifierExpression ident) {
                var classContext = ClassContext.from(PathUtils.getEnclosingClassPath(PathUtils.getElementPath(invocable)));
                var matchingField = classContext.getMatchingField(ident.name());
                if (matchingField.isPresent()) {
                    expression = ExpressionFactory.createExpression(
                            (matchingField.get().isStatic() ? matchingField.get().className() + '.'
                                    : "this.") + defaultValue.expression(),
                            TypeUtils.getUnboxedTypeTag(defaultValue.type()),
                            PathUtils.getElementPath(invocable));
                }
            }
            var expressionAsJC = expression.toJcExpression();
            statementsToInject.add(assignToVar(wrapInNonNullElseGet(expressionAsJC, param.name()), param.name()));
        }
        var methodTree = (MethodTree) PathUtils.getElementPath(invocable).getLeaf();
        var prevStatements = methodTree.getBody().getStatements();
        statementsToInject.addAll(prevStatements);
        var newStatements = com.sun.tools.javac.util.List.from(statementsToInject);
        var asJC = (JCTree.JCMethodDecl) methodTree;
        asJC.body = ExpressionMaker.makeBlock(newStatements);
        messager.printMessage(javax.tools.Diagnostic.Kind.NOTE, "Modified method tree: " + asJC);
    }

    private StatementTree assignToVar(JCTree.JCExpression expression, String varName) {
        return ExpressionMaker.makeAssignment(
                ExpressionMaker.makeIdent(varName),
                expression);
    }

    private JCTree.JCExpression wrapInNonNullElseGet(JCTree.JCExpression expression, String varName) {
        return ExpressionMaker.makeMethodInvocation(
                ExpressionMaker.makeFieldAccess(
                        ExpressionMaker.makeIdent("java.util.Objects"),
                        "requireNonNullElse"),
                List.of(ExpressionMaker.makeIdent(varName), expression)
                        .toArray(JCTree.JCExpression[]::new));
    }
}
