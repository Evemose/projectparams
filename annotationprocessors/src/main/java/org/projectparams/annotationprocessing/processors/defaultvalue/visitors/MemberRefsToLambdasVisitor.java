package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.visitors.AbstractVisitor;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MemberRefsToLambdasVisitor extends AbstractVisitor<Void, Void> {
    public MemberRefsToLambdasVisitor(Trees trees, Messager messager) {
        super(trees, messager);
    }

    @Override
    public Void visitMemberReference(com.sun.source.tree.MemberReferenceTree node, Void ignored) {
        var parent = getCurrentPath().getParentPath().getLeaf();
        List<JCTree.JCExpression> temp;
        var lambda = toLambda((JCTree.JCMemberReference) node, (JCTree) parent);
        switch (parent) {
            case JCTree.JCMethodInvocation meth -> {
                temp = new ArrayList<>(meth.args);
                temp.set(temp.indexOf(node), lambda);
                ((JCTree.JCMethodInvocation) parent).args = com.sun.tools.javac.util.List.from(temp);
                messager.printMessage(Diagnostic.Kind.NOTE, "Method invocation: " + meth);
            }
            case JCTree.JCNewClass cl -> {
                temp = new ArrayList<>(cl.args);
                temp.set(temp.indexOf(node), lambda);
                ((JCTree.JCNewClass) parent).args = com.sun.tools.javac.util.List.from(temp);
                messager.printMessage(Diagnostic.Kind.NOTE, "New class: " + cl);
            }
            case JCTree.JCVariableDecl varDecl -> {/*pass for now*/}
            default -> {/*pass*/}
        }
        return super.visitMemberReference(node, ignored);
    }

    private JCTree.JCLambda toLambda(JCTree.JCMemberReference memberReference, JCTree parent) {
        var args = new ArrayList<JCTree.JCExpression>();
        return ExpressionMaker.makeLambda(
                args.stream().map(JCTree.JCVariableDecl.class::cast).toList(),
                getBodyExpr(memberReference, args)
        );
    }

    private static JCTree.JCExpression getBodyExpr(JCTree.JCMemberReference memberReference, List<JCTree.JCExpression> args) {
        if (memberReference.mode == MemberReferenceTree.ReferenceMode.INVOKE) {
            return ExpressionMaker.makeMethodInvocation(
                    ExpressionMaker.makeFieldAccess(
                            memberReference.expr,
                            memberReference.name.toString()
                    ),
                    memberReference.typeargs,
                    args.toArray(new JCTree.JCExpression[0])
            );
        } else {
            return ExpressionMaker.makeNewClass(
                    null,
                    memberReference.getQualifierExpression().toString(),
                    memberReference.typeargs,
                    args.toArray(new JCTree.JCExpression[0])
            );
        }
    }

    private List<JCTree.JCExpression> getPassedArgs(JCTree.JCMemberReference ref, JCTree parent) {
        List<JCTree.JCExpression> args = switch (parent) {
            case JCTree.JCMethodInvocation meth -> meth.getArguments();
            case JCTree.JCNewClass cl -> cl.getArguments();
            case JCTree.JCVariableDecl varDecl -> throw new UnsupportedOperationException("Not implemented yet");
            default -> throw new IllegalStateException("Unexpected parent type: " + parent.getClass());
        };
        var type = (Type) args.get(args.indexOf(ref)).type;
        if (type == null) {
            TypeUtils.attributeExpression(ref, PathUtils.getEnclosingClassPath(getCurrentPath()).getLeaf());
            type = args.get(args.indexOf(ref)).type;
        }
        return IntStream.range(0, type.getParameterTypes().size())
                .mapToObj(i -> ExpressionMaker.makeIdent("arg" + i))
                .toList();
    }
}
