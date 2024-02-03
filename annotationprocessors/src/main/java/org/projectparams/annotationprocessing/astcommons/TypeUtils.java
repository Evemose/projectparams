package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.CUContext;


import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Utility class for working with types
 * <p>
 * !!! THIS CLASS IS THE ONLY SOURCE OF TRUTH FOR TYPES !!!
 */
public class TypeUtils {
    // for some reason, types of NewClassTree nodes are not resolved during annotation processing
    // and any attempt to resolve them manually results in an error, while attribution does not affect types at all
    private static final Map<NewClassTree, String> effectiveConstructorOwnerTypeNames = new IdentityHashMap<>();
    private static Trees trees;
    private static JavacTypes types;
    private static Elements elements;
    private static Symtab symtab;
    private static Attr attr;
    private static Enter enter;
    private static MemberEnter memberEnter;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Trees trees, JavacTypes types, Elements elements, Symtab symtab, Attr attr, Enter enter,
                            MemberEnter memberEnter) {
        TypeUtils.trees = trees;
        TypeUtils.types = types;
        TypeUtils.elements = elements;
        TypeUtils.symtab = symtab;
        TypeUtils.attr = attr;
        TypeUtils.enter = enter;
        TypeUtils.memberEnter = memberEnter;
    }

    public static Type getTypeByName(String name) {
        return switch (name) {
            case "int", "java.lang.Integer" -> symtab.intType;
            case "long", "java.lang.Long" -> symtab.longType;
            case "float", "java.lang.Float" -> symtab.floatType;
            case "double", "java.lang.Double" -> symtab.doubleType;
            case "boolean", "java.lang.Boolean" -> symtab.booleanType;
            case "void", "java.lang.Void" -> symtab.voidType;
            case "byte", "java.lang.Byte" -> symtab.byteType;
            case "short", "java.lang.Short" -> symtab.shortType;
            case "char", "java.lang.Character" -> symtab.charType;
            default -> {
                var typeElement = elements.getTypeElement(name);
                if (typeElement == null) {
                    yield Type.noType;
                }
                var type = types.getDeclaredType(typeElement);
                yield (Type) type;
            }
        };
    }


    public static String getBoxedTypeName(String name) {
        return switch (name) {
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "float" -> "java.lang.Float";
            case "double" -> "java.lang.Double";
            case "boolean" -> "java.lang.Boolean";
            case "void" -> "java.lang.Void";
            case "byte" -> "java.lang.Byte";
            case "short" -> "java.lang.Short";
            case "char" -> "java.lang.Character";
            default -> name;
        };
    }

    public static Type getBoxedType(Type type) {
        return switch (type.getTag()) {
            case INT -> getTypeByName("java.lang.Integer");
            case LONG -> getTypeByName("java.lang.Long");
            case FLOAT -> getTypeByName("java.lang.Float");
            case DOUBLE -> getTypeByName("java.lang.Double");
            case BOOLEAN -> getTypeByName("java.lang.Boolean");
            case VOID -> getTypeByName("java.lang.Void");
            case BYTE -> getTypeByName("java.lang.Byte");
            case SHORT -> getTypeByName("java.lang.Short");
            case CHAR -> getTypeByName("java.lang.Character");
            default -> type;
        };
    }

    public static TypeKind getTypeKind(TreePath path) {
        var type = trees.getTypeMirror(path);
        if (type == null) {
            return TypeKind.ERROR;
        }
        var kind = trees.getTypeMirror(path).getKind();
        if (kind == TypeKind.BYTE || kind == TypeKind.SHORT) {
            kind = TypeKind.INT;
        }
        return kind;
    }

    public static String getOwnerTypeName(MethodInvocationTree invocation, TreePath path) {
        String ownerQualifiedName;
        if (invocation.getMethodSelect() instanceof MemberSelectTree memberSelectTree) {
            ownerQualifiedName = getOwnerNameFromMemberSelect(memberSelectTree, path);
        } else if (invocation.getMethodSelect() instanceof IdentifierTree identifierTree) {
            ownerQualifiedName = getOwnerNameFromIdentifier(identifierTree, path);
        } else {
            throw new IllegalArgumentException("Unsupported method select type: "
                    + invocation.getMethodSelect().getClass().getCanonicalName());
        }
        return ownerQualifiedName;
    }

    public static String getOwnerTypeName(NewClassTree newClassTree) {
        var effectiveOwnerTypeName = effectiveConstructorOwnerTypeNames.get(newClassTree);
        if (effectiveOwnerTypeName != null) {
            return effectiveOwnerTypeName;
        }
        var ownerType = ((JCTree.JCExpression) newClassTree.getIdentifier()).type;
        if (ownerType != null) {
            return ownerType.toString();
        }
        return "<any>";
    }

    public static void addConstructorOwnerTypeName(NewClassTree newClassTree, String ownerTypeName) {
        effectiveConstructorOwnerTypeNames.put(newClassTree, ownerTypeName);
    }

    public static String getOwnerNameFromIdentifier(IdentifierTree tree, TreePath path) {
        var cuContext = CUContext.from(path.getCompilationUnit());
        var matchingImport = cuContext.getMatchingImportedStaticMethod(tree.getName().toString());
        return matchingImport.map(name -> name.substring(0, name.lastIndexOf('.')))
                .orElseGet(() -> {
                    var classDecl = (JCTree.JCClassDecl) PathUtils.getEnclosingClassPath(path).getLeaf();
                    var classElement = classDecl.sym;
                    return classElement.getQualifiedName().toString();
                });
    }

    @SuppressWarnings("unused")
    public static void attributeExpression(JCTree expression, TreePath methodTreePath) {
        var env = memberEnter.getMethodEnv(
                (JCTree.JCMethodDecl) methodTreePath.getLeaf(),
                enter.getClassEnv(((JCTree.JCClassDecl) PathUtils.getEnclosingClassPath(methodTreePath).getLeaf()).sym)
        );
        attr.attribExpr(expression, env);
    }

    public static void attributeExpression(JCTree expression, Tree classTree) {
        var env = enter.getClassEnv(
                ((JCTree.JCClassDecl) classTree).sym
        );
        attr.attribExpr(expression, env);
    }

    private static String getOwnerNameFromMemberSelect(MemberSelectTree memberSelectTree, TreePath path) {
        var expression = memberSelectTree.getExpression();
        var ownerTree = trees.getTree(trees.getElement(new TreePath(path, expression)));
        String ownerQualifiedName = null;
        if (ownerTree != null) {
            switch (ownerTree) {
                case JCTree.JCExpression expr -> ownerQualifiedName = getActualType(expr).toString();
                case JCTree.JCClassDecl staticRef -> {
                    var ownerType = staticRef.sym.type;
                    if (ownerType != null) {
                        ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                    }
                }
                case JCTree.JCVariableDecl variableDecl -> {
                    var ownerType = variableDecl.type;
                    if (ownerType != null) {
                        ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                    }
                }
                case JCTree.JCMethodDecl methodInvocation -> {
                    var ownerType = methodInvocation.type;
                    if (ownerType != null) {
                        ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported owner type: "
                        + ownerTree.getClass().getCanonicalName() + " " + ownerTree
                        + " " + memberSelectTree);
            }
        } else {
            // in case owner is return type of fixed method, we won`t be able to access its tree
            // so retrieve type from method invocation manually
            if (expression instanceof JCTree.JCExpression expr) {
                var ownerType = getActualType(expr);
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else {
                throw new IllegalArgumentException("Unsupported owner type: " + expression.getClass().getCanonicalName());
            }
        }
        return ownerQualifiedName;
    }

    public static Type getActualType(ExpressionTree tree) {
        if (tree instanceof NewClassTree newClassTree) {
            return getTypeByName(getOwnerTypeName(newClassTree));
        }
        return ((JCTree.JCExpression) tree).type;
    }

    public static boolean isAssignable(Type toType, Type fromType) {
        return types.isAssignable(toType, fromType);
    }

    public static TypeTag geLiteralTypeTag(String literalAsString) {
        if (literalAsString.equals("superSecretDefaultValuePlaceholder")) {
            return TypeTag.BOT;
        }
        if (literalAsString.matches("true|false")) {
            return TypeTag.BOOLEAN;
        }
        if (literalAsString.matches("\\d+[lLsSbB]?")) {
            if (literalAsString.endsWith("l") || literalAsString.endsWith("L")) {
                return TypeTag.LONG;
            }
            if (literalAsString.endsWith("s") || literalAsString.endsWith("S")) {
                return TypeTag.SHORT;
            }
            if (literalAsString.endsWith("b") || literalAsString.endsWith("B")) {
                return TypeTag.BYTE;
            }
            return TypeTag.INT;
        }
        if (literalAsString.matches("\\d+(\\.\\d+)?[fFdD]?")) {
            if (literalAsString.endsWith("f") || literalAsString.endsWith("F")) {
                return TypeTag.FLOAT;
            }
            return TypeTag.DOUBLE;
        }
        if (literalAsString.matches("'.'")) {
            return TypeTag.CHAR;
        }
        // fallback option
        return TypeTag.CLASS;
    }

    public static Object literalValueFromStr(TypeTag tag, String literalAsString) {
        if (literalAsString == null) {
            return null;
        }
        literalAsString = literalAsString.replaceAll("[lLfFbBsSdD]", "");
        return switch (tag) {
            case INT -> Integer.parseInt(literalAsString);
            case LONG -> Long.parseLong(literalAsString);
            case FLOAT -> Float.parseFloat(literalAsString);
            case DOUBLE -> Double.parseDouble(literalAsString);
            case BOOLEAN -> Boolean.parseBoolean(literalAsString);
            case CHAR -> literalAsString.charAt(1);
            case CLASS -> {
                if (literalAsString.matches("\".*\"")) {
                    yield literalAsString.substring(1, literalAsString.length() - 1);
                }
                throw new IllegalArgumentException("Unsupported literal: " + literalAsString);
            }
            case BYTE -> Byte.parseByte(literalAsString);
            case SHORT -> Short.parseShort(literalAsString);
            case BOT -> null;
            default -> throw new IllegalArgumentException("Unsupported literal: " + literalAsString);
        };
    }

    public static TypeTag getUnboxedTypeTag(Type type) {
        return switch (type.toString()) {
            case "java.lang.Integer", "int" -> TypeTag.INT;
            case "java.lang.Long", "long" -> TypeTag.LONG;
            case "java.lang.Float", "float" -> TypeTag.FLOAT;
            case "java.lang.Double", "double" -> TypeTag.DOUBLE;
            case "java.lang.Boolean", "boolean" -> TypeTag.BOOLEAN;
            case "java.lang.Void", "void" -> TypeTag.VOID;
            case "java.lang.Byte", "byte" -> TypeTag.BYTE;
            case "java.lang.Short", "short" -> TypeTag.SHORT;
            case "java.lang.Character", "char" -> TypeTag.CHAR;
            case null -> TypeTag.BOT;
            default -> TypeTag.CLASS;
        };
    }
}
