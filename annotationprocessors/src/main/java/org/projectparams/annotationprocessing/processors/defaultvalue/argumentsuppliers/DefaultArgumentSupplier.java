package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

import java.util.ArrayList;

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
            args.add(makeLiteral(getTypeTag(invocableInfo.parameterTypeQualifiedNames().get(i)), defaultValue));
        }
        return List.from(args);
    }

    private JCTree.JCExpression makeLiteral(TypeTag tag, Object value) {
        if (value.equals(InvocableInfo.NULL)) {
            return treeMaker.Literal(TypeTag.BOT, null);
        }
        // javac doesn't support short and byte literals directly, so we need to cast them to int
        if (tag == TypeTag.SHORT || tag == TypeTag.BYTE) {
            var cast = treeMaker.TypeCast(treeMaker.TypeIdent(tag), treeMaker.Literal(TypeTag.INT, value));
            cast.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
            return cast;
        }
        var literal = treeMaker.Literal(tag, value);
        if (tag == TypeTag.BOOLEAN) {
            literal.type = TypeUtils.getTypeByName("java.lang.Boolean");
        } else {
            literal.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
        }
        return literal;
    }
}
