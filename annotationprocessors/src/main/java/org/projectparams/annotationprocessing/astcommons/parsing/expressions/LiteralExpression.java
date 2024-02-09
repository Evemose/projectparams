package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.Nullable;
import java.util.Objects;

public class LiteralExpression implements Expression {
    private final Object value;
    private final Type type;
    public static final LiteralExpression NULL = new LiteralExpression(null);

    public LiteralExpression(Object value) {
        this.value = value;
        if (value == null) {
            // any primitive type will do
            this.type = TypeUtils.getTypeByName("java.lang.String");
            return;
        }
        if (!isLiteralType(value.getClass())) {
            throw new IllegalArgumentException("Class " + value.getClass().getCanonicalName() + " is not a literal type");
        }
        this.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
    }

    private static boolean isLiteralType(Class<?> type) {
        return type == String.class || type == Character.class || type == Boolean.class
                || Number.class.isAssignableFrom(type);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeLiteral(type.getTag(), value);
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        // Do nothing
    }
}
