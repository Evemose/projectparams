package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;

import javax.annotation.processing.Messager;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CleanupVisitor extends AbstractVisitor<Void, Void> {
    private final Set<InvocableTree> allFixedMethods;
    private final TreeMaker treeMaker;
    private final Map<String, Type> fixedVarTypeNames = new HashMap<>();

    public CleanupVisitor(Set<InvocableTree> allFixedMethods, Trees trees, Messager messager, TreeMaker treeMaker) {
        super(trees, messager);
        this.allFixedMethods = allFixedMethods;
        this.treeMaker = treeMaker;
    }

    // clear fixed local variables pool on method enter
    @Override
    public Void visitMethod(MethodTree method, Void aVoid) {
        fixedVarTypeNames.clear();
        return super.visitMethod(method, aVoid);
    }

    /**
     * Fix types of variables declared with var that have an error type
     */
    @Override
    public Void visitVariable(VariableTree variableTree, Void ignored) {
        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR) {
            for (var fixedMethod : allFixedMethods) {
                var initializer = variableTree.getInitializer();
                if (initializer != null &&
                        (initializer.getKind() == Tree.Kind.METHOD_INVOCATION || initializer.getKind() == Tree.Kind.NEW_CLASS)
                        && fixedMethod.getWrapped() == initializer) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Error var initializer: " + variableTree.getInitializer());
                    var asJC = (JCTree.JCVariableDecl) variableTree;
                    asJC.vartype = treeMaker.Type(fixedMethod.getReturnType());
                    asJC.type = asJC.vartype.type;
                    fixedVarTypeNames.put(variableTree.getName().toString(), asJC.type);
                    messager.printMessage(Diagnostic.Kind.NOTE, "Fixed var type: " + asJC.type);
                    break;
                }
            }
        }
        return super.visitVariable(variableTree, ignored);
    }

    // for some reason variable type changes doesn't propagate to NewClassTree nodes, including their identifiers,
    // so we need to fix them manually
    @Override
    public Void visitNewClass(NewClassTree invocation, Void ignored) {
        updateOwnerTypeIfAnyFixMatchEnclosingExpr(invocation);
        updateArgTypeIfAnyFixMatchEnclosingExpr(invocation);
        return super.visitNewClass(invocation, ignored);
    }

    private void updateArgTypeIfAnyFixMatchEnclosingExpr(NewClassTree invocation) {
        var asJC = (JCTree.JCNewClass) invocation;
        var args = asJC.getArguments();
        for (var arg : args) {
            if (fixedVarTypeNames.containsKey(arg.toString())) {
                arg.type = fixedVarTypeNames.get(arg.toString());
                break;
            } else {
                for (var fixedMethod : allFixedMethods) {
                    if (fixedMethod.getWrapped() == arg) {
                        arg.type = fixedMethod.getReturnType();
                        break;
                    }
                }
            }
        }
    }

    private void updateOwnerTypeIfAnyFixMatchEnclosingExpr(NewClassTree invocation) {
        var asJC = (JCTree.JCNewClass) invocation;
        var enclosingExpression = asJC.getEnclosingExpression();
        if (enclosingExpression != null &&
                fixedVarTypeNames.containsKey(enclosingExpression.toString())) {
            TypeUtils.addConstructorOwnerTypeName(invocation,
                    fixedVarTypeNames.get(enclosingExpression.toString()).toString() + "." +
                            asJC.getIdentifier().toString().replaceAll(".?<.*>.?", ""));
        } else {
            for (var fixedMethod : allFixedMethods) {
                if (fixedMethod.getWrapped() == invocation.getEnclosingExpression()) {
                    TypeUtils.addConstructorOwnerTypeName(invocation, fixedMethod.getReturnType().toString()
                            + "." + asJC.getIdentifier().toString().replaceAll(".?<.*>.?", ""));
                    break;
                }
            }
        }
    }
}
