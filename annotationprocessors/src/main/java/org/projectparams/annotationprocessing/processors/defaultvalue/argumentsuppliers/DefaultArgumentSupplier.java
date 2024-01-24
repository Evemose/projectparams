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

    @Override
    public List<JCTree.JCExpression> getModifiedArguments(InvocableTree invocation, InvocableInfo invocableInfo)
            throws UnsupportedSignatureException {
        var args = new ArrayList<>(invocation.getArguments().stream().map(arg -> (JCTree.JCExpression) arg).toList());
        for (var i = invocation.getArguments().size(); i < invocableInfo.parameterTypeQualifiedNames().length; i++) {
             var defaultValue = invocableInfo.paramIndexToDefaultValue().get(i);
             if (defaultValue == null) {
                 throw new UnsupportedSignatureException(invocableInfo.parameterTypeQualifiedNames()[i], i, invocableInfo);
             }
             args.add(makeLiteral(getTypeTagOfParam(invocableInfo.parameterTypeQualifiedNames()[i]), defaultValue));
        }
        return List.from(args);
    }


    private JCTree.JCLiteral makeLiteral(TypeTag tag, Object value) {
        if (value.equals(InvocableInfo.NULL)) {
            return treeMaker.Literal(TypeTag.BOT, null);
        }
        var literal = treeMaker.Literal(tag, value);
        literal.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
        return literal;
    }

    private static TypeTag getTypeTagOfParam(String paramType) {
        return TypeUtils.getTypeByName(paramType).getTag();
    }
}
