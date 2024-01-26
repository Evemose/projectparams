package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.CUContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
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
    private static final Map<Tree, String> effectiveConstructorArgTypes = new IdentityHashMap<>();
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
            case "int"  -> symtab.intType;
            case "long" -> symtab.longType;
            case "float" -> symtab.floatType;
            case "double" -> symtab.doubleType;
            case "boolean" -> symtab.booleanType;
            case "void" -> symtab.voidType;
            case "byte" -> symtab.byteType;
            case "short" -> symtab.shortType;
            case "char" -> symtab.charType;
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
                .orElse(getFullyQualifiedName((ClassTree) getEnclosingClassPath(path).getLeaf()));
    }

    public static String getFullyQualifiedName(ClassTree classTree) {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) classTree;
        Element classElement = classDecl.sym;
        PackageElement packageElement = elements.getPackageOf(classElement);
        return packageElement.getQualifiedName().toString() + "." + classElement.getSimpleName().toString();
    }

    public static void attributeExpression(JCTree expression, TreePath methodTree) {
        var env = memberEnter.getMethodEnv(
                (JCTree.JCMethodDecl) methodTree.getLeaf(),
                enter.getClassEnv(((JCTree.JCClassDecl) getEnclosingClassPath(getEnclosingMethodPath(methodTree)).getLeaf()).sym)
        );
        attr.attribExpr(expression, env);
    }

    public static TreePath getEnclosingClassPath(TreePath path) {
        while (path != null && !(path.getLeaf() instanceof ClassTree)) {
            path = path.getParentPath();
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is not enclosed in class");
        }
        return path;
    }

    public static TreePath getEnclosingMethodPath(TreePath path) {
        while (path != null && !(path.getLeaf() instanceof MethodTree)) {
            path = path.getParentPath();
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is not enclosed in method");
        }
        return path;
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
                    var ownerType = methodInvocation.getReturnType().type;
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
        } else if (effectiveConstructorArgTypes.containsKey(tree)) {
            return getTypeByName(effectiveConstructorArgTypes.get(tree));
        }
        return ((JCTree.JCExpression) tree).type;
    }

    public static boolean isAssignable(Type fromType, Type toType) {
        return types.isAssignable(fromType, toType);
    }
}
