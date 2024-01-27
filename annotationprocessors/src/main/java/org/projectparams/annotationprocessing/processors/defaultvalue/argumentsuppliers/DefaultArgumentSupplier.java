package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.parsing.ParsedExpression;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

import java.util.ArrayList;
import java.util.Collections;

public class DefaultArgumentSupplier implements ArgumentSupplier {
    private final TreeMaker treeMaker;

    public DefaultArgumentSupplier(TreeMaker treeMaker) {
        this.treeMaker = treeMaker;
    }

    private static TypeTag getTypeTag(String type) {
        return TypeUtils.getTypeByName(type).getTag();
    }

    @Override
    public List<JCTree.JCExpression> getModifiedArguments(InvocableTree invocation, InvocableInfo invocableInfo)
            throws UnsupportedSignatureException {
        var args = new ArrayList<>(invocation.getArguments().stream().map(arg -> (JCTree.JCExpression) arg).toList());
        for (var i = invocation.getArguments().size(); i < invocableInfo.parameterTypeQualifiedNames().size(); i++) {
            var defaultValue = invocableInfo.paramIndexToDefaultValue().get(i);
            if (defaultValue == null) {
                throw new UnsupportedSignatureException(invocableInfo.parameterTypeQualifiedNames().get(i), i, invocableInfo);
            }
            var value = switch (defaultValue) {
                case Short s -> defaultValue + "S";
                case Byte b -> defaultValue + "B";
                case Character c -> "'" + defaultValue + "'";
                case String s when !defaultValue.equals("superSecretDefaultValuePlaceholder") -> "\"" + defaultValue + "\"";
                case Float v -> defaultValue + "F";
                default -> defaultValue;
            };
            args.add(ExpressionMaker.makeExpr(
                    new ParsedExpression(
                            ParsedExpression.Type.LITERAL,
                            String.valueOf(value),
                            null,
                            Collections.emptyList()
                    )));
        }
        return List.from(args);
    }


}
