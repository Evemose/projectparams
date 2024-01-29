package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import java.lang.reflect.ParameterizedType;

public class LiteralExpression<T> implements Expression {
    private final T value;
    private final Type type;

    public static final LiteralExpression<?> NULL = new LiteralExpression<>(null, String.class);

    public LiteralExpression(T value, Class<T> clazz) {
        this.value = value;
        if (!isLiteralType(clazz)) {
            throw new IllegalArgumentException("Class " + clazz.getCanonicalName() + " is not a literal type");
        }
        this.type = TypeUtils.getTypeByName(clazz.getCanonicalName());
    }

    private static boolean isLiteralType(Class<?> type) {
        return type == String.class || type == Character.class || type == Boolean.class
                || Number.class.isAssignableFrom(type);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeLiteral(type.getTag(), value);
    }
}
