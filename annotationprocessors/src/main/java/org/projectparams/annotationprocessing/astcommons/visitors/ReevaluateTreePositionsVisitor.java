package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.Messager;
import java.util.*;

public class ReevaluateTreePositionsVisitor extends AbstractVisitor<Void, Void> {
    public ReevaluateTreePositionsVisitor(Trees trees, Messager messager) {
        super(trees, messager);
    }

    @Override
    public Void visitClass(ClassTree tree, Void aVoid) {
        var classPosition = getPos(tree);
        updatePositions(classPosition, tree.getMembers());
        return super.visitClass(tree, aVoid);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, tree.getParameters());
        updatePositions(parentPos, tree.getBody().getStatements());
        return super.visitMethod(tree, aVoid);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getTypeArguments().stream().map(Tree.class::cast).toArray(Tree[]::new)));
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree,
                tree.getArguments().stream().map(Tree.class::cast).toArray(Tree[]::new)));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getMethodSelect()));
        return super.visitMethodInvocation(tree, aVoid);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getInitializer()));
        return super.visitVariable(tree, aVoid);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression(), tree.getIndex()));
        return super.visitArrayAccess(tree, aVoid);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, tree.getArguments());
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getClassBody()));
        return super.visitNewClass(tree, aVoid);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, tree.getDimensions());
        updatePositions(parentPos, tree.getInitializers());
        return super.visitNewArray(tree, aVoid);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, tree.getParameters());
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getBody()));
        return super.visitLambdaExpression(tree, aVoid);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getQualifierExpression()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getTypeArguments().stream().map(Tree.class::cast).toArray(Tree[]::new)));
        return super.visitMemberReference(tree, aVoid);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitMemberSelect(tree, aVoid);
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitParenthesized(tree, aVoid);
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getType()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitTypeCast(tree, aVoid);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression(), tree.getType()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getType()));
        return super.visitInstanceOf(tree, aVoid);
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitUnary(tree, aVoid);
    }

    @Override
    public Void visitBinary(BinaryTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getLeftOperand()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getRightOperand()));
        return super.visitBinary(tree, aVoid);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getCondition()));
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getTrueExpression()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getFalseExpression()));
        return super.visitConditionalExpression(tree, aVoid);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree, tree.getVariable()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitAssignment(tree, aVoid);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void aVoid) {
        var parentPos = getPos(tree);
        parentPos = updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getVariable()));
        updatePositions(parentPos, setOfIgnoreNullsPreserveOrderExcludeParent(tree, tree.getExpression()));
        return super.visitCompoundAssignment(tree, aVoid);
    }

    private long getPos(Tree tree) {
        return ((JCTree) tree).pos;
    }

    private long updatePositions(long parentPosition, Iterable<? extends Tree> trees) {
        if (trees == null) {
            return parentPosition;
        }
        for (var tree : trees) {
            var asJC = (JCTree) tree;
            var treePosition = this.getPos(asJC);
            if (treePosition <= parentPosition) {
                asJC.pos = (int) ++parentPosition;
            }
        }
        return parentPosition;
    }
    
    @SafeVarargs
    private <T> Set<? extends T> setOfIgnoreNullsPreserveOrderExcludeParent(T parent, T... trees) {
        return Arrays.stream(trees).filter(Objects::nonNull).filter(tree -> tree != parent).<Set<T>>collect(
                () -> Collections.newSequencedSetFromMap(new LinkedHashMap<>()), Set::add, Set::addAll);
    }
}