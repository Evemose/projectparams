package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;

import java.util.*;

public class ReevaluateTreePositionsVisitor extends TreePathScanner<Void, Void> {
    private int position = -1;

    @Override
    public Void visitClass(ClassTree tree, Void aVoid) {
        if (position == -1) {
            position = ((JCTree) tree).pos;
        }
        ((JCTree) tree).pos = position;
        updatePositions(tree.getMembers());
        return super.visitClass(tree, aVoid);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void aVoid) {
        
        updatePositions(tree.getParameters());
        updatePositions(tree.getBody().getStatements());
        return super.visitMethod(tree, aVoid);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getTypeArguments().stream().map(Tree.class::cast).toArray(Tree[]::new)));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree,
                tree.getArguments().stream().map(Tree.class::cast).toArray(Tree[]::new)));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getMethodSelect()));
        return super.visitMethodInvocation(tree, aVoid);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void aVoid) {
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getInitializer()));
        return super.visitVariable(tree, aVoid);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void aVoid) {
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression(), tree.getIndex()));
        return super.visitArrayAccess(tree, aVoid);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void aVoid) {
        updatePositions(tree.getArguments());
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getClassBody()));
        return super.visitNewClass(tree, aVoid);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void aVoid) {
        updatePositions(tree.getDimensions());
        updatePositions(tree.getInitializers());
        return super.visitNewArray(tree, aVoid);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void aVoid) {
        
        updatePositions(tree.getParameters());
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getBody()));
        return super.visitLambdaExpression(tree, aVoid);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getQualifierExpression()));
        if (tree.getTypeArguments() != null) {
            updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getTypeArguments().stream().map(Tree.class::cast).toArray(Tree[]::new)));
        }
        return super.visitMemberReference(tree, aVoid);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitMemberSelect(tree, aVoid);
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitParenthesized(tree, aVoid);
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getType()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitTypeCast(tree, aVoid);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression(), tree.getType()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getType()));
        return super.visitInstanceOf(tree, aVoid);
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitUnary(tree, aVoid);
    }

    @Override
    public Void visitBinary(BinaryTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getLeftOperand()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getRightOperand()));
        return super.visitBinary(tree, aVoid);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getCondition()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getTrueExpression()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getFalseExpression()));
        return super.visitConditionalExpression(tree, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void aVoid) {
        
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree, tree.getVariable()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitAssignment(tree, aVoid);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void aVoid) {
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getVariable()));
        updatePositions(setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitCompoundAssignment(tree, aVoid);
    }

    private long getPos(Tree tree) {
        return ((JCTree) tree).pos;
    }

    private void updatePositions(Iterable<? extends Tree> trees) {
        if (trees == null) {
            return;
        }
        for (var tree : trees) {
            var asJC = (JCTree) tree;
            asJC.pos = ++position;
        }
    }

    @SafeVarargs
    private <T> Set<? extends T> setOfIgnoreNullsPreserveOrderExcludeParent(T parent, T... trees) {
        return Arrays.stream(trees).filter(Objects::nonNull).filter(tree -> tree != parent).collect(
                () -> Collections.newSequencedSetFromMap(new LinkedHashMap<>()), Set::add, Set::addAll);
    }
}