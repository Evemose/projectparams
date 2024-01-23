package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;

import java.util.ArrayList;

public class DefaultArgumentSupplier implements ArgumentSupplier {
    private final TreeMaker treeMaker;

    public DefaultArgumentSupplier(TreeMaker treeMaker) {
        this.treeMaker = treeMaker;
    }

    @Override
    public List<JCTree.JCExpression> getModifiedArguments(MethodInvocationTree invocation, MethodInfo methodInfo)
            throws UnsupportedSignatureException {
        var args = new ArrayList<>(invocation.getArguments().stream().map(arg -> (JCTree.JCExpression) arg).toList());
        for (var i = invocation.getArguments().size(); i < methodInfo.parameterTypeQualifiedNames().length; i++) {
             var defaultValue = methodInfo.paramIndexToDefaultValue().get(i);
             if (defaultValue == null) {
                 throw new UnsupportedSignatureException(methodInfo.parameterTypeQualifiedNames()[i], i, methodInfo);
             }
             args.add(makeLiteral(getTypeTagOfParam(methodInfo.parameterTypeQualifiedNames()[i]), defaultValue));
        }
        return List.from(args);
    }


    private JCTree.JCLiteral makeLiteral(TypeTag tag, Object value) {
        if (value.equals(MethodInfo.NULL)) {
            var nullLiteral = treeMaker.Literal(TypeTag.BOT, null);
            nullLiteral.type = TypeUtils.getTypeByName("java.lang.Object");
            return nullLiteral;
        }
        var literal = treeMaker.Literal(tag, value);
        literal.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
        return literal;
    }

    private static TypeTag getTypeTagOfParam(String paramType) {
        return TypeUtils.getTypeByName(paramType).getTag();
    }
}
