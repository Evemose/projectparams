package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpressionType;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

import java.util.ArrayList;

public class DefaultArgumentSupplier implements ArgumentSupplier {

    @Override
    public List<JCTree.JCExpression> getModifiedArguments(InvocableTree invocation,
                                                          InvocableInfo invocableInfo,
                                                          TreePath path)
            throws UnsupportedSignatureException {
        var args = new ArrayList<>(invocation.getArguments().stream().map(JCTree.JCExpression.class::cast).toList());
        for (var i = invocation.getArguments().size(); i < invocableInfo.parameters().size(); i++) {
            var defaultValue = invocableInfo.parameters().get(i).defaultValue();
            if (defaultValue == null) {
                throw new UnsupportedSignatureException(invocableInfo.parameters().get(i).name(), i, invocableInfo);
            }
            if (defaultValue.expression() != null && defaultValue.expression().equals("c")) {
                var a = 1;
            }
            if (!TypeUtils.isPrimitiveOrBoxedType(defaultValue.type())) {
                args.add(LiteralExpression.NULL.toJcExpression());
                continue;
            }
            try {
                args.add(LiteralExpressionType.getInstance().parse(
                        new CreateExpressionParams(
                                defaultValue.expression(),
                                TypeUtils.getUnboxedTypeTag(defaultValue.type()),
                                path
                        )).toJcExpression());
            } catch (Exception e) {
                args.add(LiteralExpression.NULL.toJcExpression());
            }
        }
        return List.from(args);
    }

}
