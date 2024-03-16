package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.context.CUContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    private static JavacTypes javacTypes;
    private static Types types;
    private static Elements elements;
    private static Symtab symtab;
    private static Attr attr;
    private static Enter enter;
    private static MemberEnter memberEnter;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Trees trees, JavacTypes javacTypes, Elements elements, Symtab symtab, Attr attr, Enter enter,
                            MemberEnter memberEnter, Types types) {
        TypeUtils.trees = trees;
        TypeUtils.javacTypes = javacTypes;
        TypeUtils.elements = elements;
        TypeUtils.symtab = symtab;
        TypeUtils.attr = attr;
        TypeUtils.enter = enter;
        TypeUtils.memberEnter = memberEnter;
        TypeUtils.types = types;
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
            default -> getNonPrimitiveTypeByName(name);
        };
    }

    private static Type getNonPrimitiveTypeByName(String name) {
        var typeElement = elements.getTypeElement(name);
        if (typeElement == null) {
            return Type.noType;
        }
        return (Type) javacTypes.getDeclaredType(typeElement);
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
        if (type == null) {
            return null;
        }
        return switch (type.getTag()) {
            case INT -> getNonPrimitiveTypeByName("java.lang.Integer");
            case LONG -> getNonPrimitiveTypeByName("java.lang.Long");
            case FLOAT -> getNonPrimitiveTypeByName("java.lang.Float");
            case DOUBLE -> getNonPrimitiveTypeByName("java.lang.Double");
            case BOOLEAN -> getNonPrimitiveTypeByName("java.lang.Boolean");
            case VOID -> getNonPrimitiveTypeByName("java.lang.Void");
            case BYTE -> getNonPrimitiveTypeByName("java.lang.Byte");
            case SHORT -> getNonPrimitiveTypeByName("java.lang.Short");
            case CHAR -> getNonPrimitiveTypeByName("java.lang.Character");
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

    @SuppressWarnings("all")
    private static void attributeExpression(JCTree expression, TreePath methodTreePath, boolean ignored) {
        var env = memberEnter.getMethodEnv(
                (JCTree.JCMethodDecl) methodTreePath.getLeaf(),
                enter.getClassEnv(((JCTree.JCClassDecl) PathUtils.getEnclosingClassPath(methodTreePath).getLeaf()).sym)
        );
        attr.attribExpr(expression, env);
    }

    private static void attributeExpression(JCTree expression, Tree classTree) {
        var env = enter.getClassEnv(
                ((JCTree.JCClassDecl) classTree).sym
        );
        attr.attribExpr(expression, env);
    }

    public static void attributeExpression(JCTree expression, TreePath exprToAttribute) {
        TreePath encl;
        try {
            encl = PathUtils.getEnclosingMethodPath(exprToAttribute);
            attributeExpression(expression, encl, false);
        } catch (IllegalArgumentException e) {
            encl = PathUtils.getEnclosingClassPath(exprToAttribute);
            attributeExpression(expression, encl.getLeaf());
        }
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
                case JCTree.JCMethodDecl methodDecl -> {
                    var ownerType = methodDecl.type;
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
        if (toType == null || fromType == null) {
            return false;
        }
        return javacTypes.isAssignable(toType, fromType);
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
        if (literalAsString.matches("\\d+(\\.\\d*)?[fFdD]?")) {
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
        literalAsString = literalAsString.replaceAll("[lLfFbBsSdD]$", "").strip();
        return switch (tag) {
            case INT -> Integer.parseInt(literalAsString);
            case LONG -> Long.parseLong(literalAsString);
            case FLOAT -> Float.parseFloat(literalAsString);
            case DOUBLE -> Double.parseDouble(literalAsString);
            case BOOLEAN -> Boolean.parseBoolean(literalAsString);
            case CHAR -> switch (literalAsString.length()) {
                case 1 -> literalAsString.charAt(0);
                case 2 -> switch (literalAsString.charAt(1)) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    default -> throw new IllegalArgumentException("Unsupported escape sequence: " + literalAsString);
                };
                case 3 -> literalAsString.charAt(1);
                default -> throw new IllegalArgumentException("Unsupported literal: " + literalAsString);
            };
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

    public static boolean isPrimitiveOrBoxedType(Type type) {
        return switch (type.toString()) {
            case "int", "java.lang.Integer", "long", "java.lang.Long", "float", "java.lang.Float",
                    "double", "java.lang.Double", "boolean", "java.lang.Boolean",
                    "byte", "java.lang.Byte", "short", "java.lang.Short", "char", "java.lang.Character",
                    "java.lang.String" -> true;
            default -> false;
        };
    }

    public static boolean isAccessible(Symbol toAccess, Symbol from) {
        return toAccess.isAccessibleIn(from, types);
    }

    public static List<Type> getTypeArgs(Symbol sym) {
        var res = new ArrayList<>(sym.type.getTypeArguments());
        if (sym.owner != null && !sym.isStatic()) {
            res.addAll(getTypeArgs(sym.owner));
        }
        return res;
    }

    public static Symbol.MethodSymbol getFuncMethod(Symbol lambdaClassSym, Map<Name, Type> generics) {
        if (lambdaClassSym instanceof Symbol.TypeVariableSymbol) {
            if (!generics.containsKey(lambdaClassSym.name)) {
                return null;
            }
            lambdaClassSym = generics.get(lambdaClassSym.name).tsym;
        }
        if (lambdaClassSym.members() == null) {
            return null;
        }
        var funcInterfaceMethodsIter = lambdaClassSym.members().getSymbols(
                sym -> sym instanceof Symbol.MethodSymbol
                        && sym.isPublic()
                        && sym.getModifiers().stream().noneMatch(mod -> mod == Modifier.DEFAULT || mod == Modifier.STATIC)
        ).iterator();
        if (!funcInterfaceMethodsIter.hasNext()) {
            throw new IllegalStateException("No methods in functional interface: " + lambdaClassSym);
        }
        return (Symbol.MethodSymbol) funcInterfaceMethodsIter.next();
    }

    public static Map<Name, Type> inferGenerics(List<JCTree.JCExpression> args,
                                                com.sun.tools.javac.util.List<Symbol.VarSymbol> parameters,
                                                List<Type> typeArgs,
                                                TreePath currentPath) throws IllegalStateException {
        return typeArgs.stream()
                .filter(type -> type.tsym instanceof Symbol.TypeVariableSymbol)
                .map(type -> (Symbol.TypeVariableSymbol) type.tsym)
                .collect(Collectors.toMap(
                        symGeneric -> symGeneric.name,
                        symGeneric -> getBoxedType(getActualGenericType(
                                symGeneric, args, parameters, currentPath))
                ));
    }

    private static Type getActualGenericType(Symbol.TypeVariableSymbol symGeneric,
                                             List<JCTree.JCExpression> args,
                                             com.sun.tools.javac.util.List<Symbol.VarSymbol> parameters,
                                             TreePath currentPath) {
        var passedArgsTypes = args.stream().map(arg -> {
            var actualType = getActualType(arg);
            if (actualType == null && !(arg instanceof JCTree.JCMemberReference)) {
                attributeExpression(arg, currentPath);
                actualType = getActualType(arg);
            }
            return actualType;
        }).toList();
        var index = IntStream.range(0, parameters.size())
                .filter(i -> Objects.equals(parameters.get(i).type.tsym, symGeneric))
                .findFirst();
        if (index.isEmpty()) {
            return tryInferFromArgTypeArgs(symGeneric, parameters, args).orElseGet(() ->
                    tryInferFromMemberRefs(symGeneric, args, parameters, currentPath)
                            .orElseThrow(() -> new IllegalStateException("No passed type for generic: " + symGeneric)));
        }
        if (index.getAsInt() >= passedArgsTypes.size()) {
            throw new IllegalStateException("No passed type for generic: " + symGeneric);
        }
        return passedArgsTypes.get(index.getAsInt());
    }

    private static Optional<Type> tryInferFromMemberRefs(
            Symbol.TypeVariableSymbol symGeneric,
            List<JCTree.JCExpression> args,
            com.sun.tools.javac.util.List<Symbol.VarSymbol> parameters,
            TreePath currentPath
    ) {
        var memberRefsMap = getMemberRefsMap(args, currentPath);
        return memberRefsMap.entrySet().stream()
                .map(entry -> {
                    var index = args.indexOf(entry.getKey());
                    var funcMethod = Objects.requireNonNull(
                            getFuncMethod(parameters.get(index).type.tsym, Collections.emptyMap())
                    );
                    var lambdaParamTypes = funcMethod.params().map(param -> param.type);
                    var conversionMap = getTypeArgs(funcMethod).stream()
                            .collect(Collectors.toMap(
                                    type -> {
                                        var res = parameters.get(index).type.getTypeArguments().get(
                                                getTypeArgs(funcMethod).indexOf(type)
                                        );
                                        return (res instanceof Type.WildcardType
                                                ? ((Type.WildcardType) res).type
                                                : res).tsym.name;
                                    },
                                    type -> type,
                                    (a, b) -> a
                            ));
                    Map<Name, Type> inferredInLambda = new HashMap<>();
                    if (funcMethod.getReturnType() instanceof Type.TypeVar) {
                        inferredInLambda.put(
                                funcMethod.getReturnType().tsym.name,
                                entry.getValue().getReturnType()
                        );
                    }
                    inferredInLambda.putAll(lambdaParamTypes.stream()
                            .filter(type -> !inferredInLambda.containsKey(type.tsym.name))
                            .collect(Collectors.toMap(
                                    type -> type.tsym.name,
                                    type -> entry.getValue().params().get(
                                            IntStream.range(0, lambdaParamTypes.size())
                                                    .filter(i -> Objects.equals(
                                                            funcMethod.params().get(i).type, type
                                                    ))
                                                    .findFirst().orElseThrow(() -> new IllegalStateException(
                                                            "No matching parameter for type: " + type
                                                    ))
                                    ).type
                            )));
                    var inferred = inferredInLambda.get(conversionMap.getOrDefault(symGeneric.name,
                            symGeneric.type).tsym.name);
                    if (inferred == null) {
                        return null;
                    }
                    return getBoxedType(
                            inferred instanceof Type.TypeVar
                                    ? entry.getKey().getTypeArguments().get(
                                    getTypeArgs(entry.getValue()).indexOf(inferred)
                            ).type
                                    : inferred
                    );
                }).filter(Objects::nonNull).findFirst();
    }

    private static Map<JCTree.JCMemberReference, Symbol.MethodSymbol> getMemberRefsMap(
            List<JCTree.JCExpression> args,
            TreePath currentPath
    ) {
        return args.stream()
                .filter(JCTree.JCMemberReference.class::isInstance)
                .map(JCTree.JCMemberReference.class::cast)
                .collect(Collectors.toMap(
                        ref -> ref,
                        ref -> getAllAccessibleMembers((Symbol.ClassSymbol) Objects.requireNonNullElseGet(
                                ref.expr.type, () -> {
                                    var temp = ExpressionMaker.makeFieldAccess(ref.expr, ref.name.toString());
                                    attributeExpression(temp, currentPath);
                                    return temp.selected.type;
                                }).tsym, ref.getName(), null)
                                .filter(Symbol.MethodSymbol.class::isInstance)
                                .map(Symbol.MethodSymbol.class::cast)
                                .findFirst().orElseThrow()
                ));
    }

    private static Optional<Type> tryInferFromArgTypeArgs(Symbol.TypeVariableSymbol symGeneric,
                                                          List<Symbol.VarSymbol> parameters,
                                                          List<JCTree.JCExpression> args) {
        for (var i = 0; i < args.size(); i++) {
            var inferred = tryInferFromArgTypeArgsInner(symGeneric, parameters.get(i).type, args.get(i).type);
            if (inferred.isPresent()) {
                return inferred;
            }
        }
        return Optional.empty();
    }

    private static Optional<Type> tryInferFromArgTypeArgsInner(
            Symbol.TypeVariableSymbol symGeneric,
            Type correspondingDeclType,
            Type type
    ) {
        switch (type) {
            case Type.ClassType classType -> {
                var index = IntStream.range(0, classType.getTypeArguments().size())
                        .filter(i -> Objects.equals(correspondingDeclType.getTypeArguments().get(i).tsym, symGeneric))
                        .findFirst();
                if (index.isPresent()) {
                    return Optional.of(classType.getTypeArguments().get(index.getAsInt()));
                }
                for (var i = 0; i < classType.getTypeArguments().size(); i++) {
                    var inferred = tryInferFromArgTypeArgsInner(
                            symGeneric,
                            correspondingDeclType.getTypeArguments().get(i),
                            classType.getTypeArguments().get(i)
                    );
                    if (inferred.isPresent()) {
                        return inferred;
                    }
                }
            }
            case Type.WildcardType wildcardType -> {
                return tryInferFromArgTypeArgsInner(
                        symGeneric,
                        ((Type.WildcardType) correspondingDeclType).type,
                        wildcardType.type
                );
            }
            case Type.ArrayType arrayType -> {
                return tryInferFromArgTypeArgsInner(
                        symGeneric,
                        ((Type.ArrayType) correspondingDeclType).elemtype,
                        arrayType.elemtype
                );
            }
            case null, default -> {
            }
        }
        return Optional.empty();
    }

    private static Stream<Symbol> filterAccessibleMembers(
            Symbol.ClassSymbol classSymbol,
            Stream<Symbol> stream
    ) {
        return stream.filter(s -> isAccessible(s, classSymbol))
                .filter(s -> !s.isStatic())
                .filter(Symbol.MethodSymbol.class::isInstance);
    }

    private static Symbol.MethodSymbol updateMethSymbol(Symbol.ClassSymbol classSymbol, Symbol.ClassSymbol classToUnifyTypesWith, Symbol.MethodSymbol sym) {
        if (classToUnifyTypesWith == null) {
            return sym;
        }
        var newMethSym = new Symbol.MethodSymbol(
                sym.flags_field,
                sym.name,
                sym.type,
                classSymbol
        );
        com.sun.tools.javac.util.List<? extends Symbol> ownerArgs = newMethSym.owner.getTypeParameters();
        var ownerArgsToUnifyWith = classSymbol.isInterface() ?
                classToUnifyTypesWith.getInterfaces().stream()
                        .filter(iface -> iface.tsym == classSymbol)
                        .findFirst()
                        .map(Type::getTypeArguments)
                        .orElseThrow()
                : classToUnifyTypesWith.getSuperclass().getTypeArguments();
        if (ownerArgsToUnifyWith == null) {
            newMethSym.params = newMethSym.params().map(param -> new Symbol.VarSymbol(
                    param.flags_field,
                    param.name,
                    getTypeByName("java.lang.Object"),
                    newMethSym
            ));
        } else {
            newMethSym.params = newMethSym.params().map(param ->
                    new Symbol.VarSymbol(
                            param.flags_field,
                            param.name,
                            param.type instanceof Type.TypeVar
                                    ? ownerArgsToUnifyWith.get(ownerArgs.indexOf(param.type.tsym)) :
                                    replaceAllTypeVars(
                                            param.type,
                                            IntStream.range(0, ownerArgs.size())
                                                    .boxed()
                                                    .collect(Collectors.toMap(
                                                            i -> ownerArgs.get(i).name,
                                                            ownerArgsToUnifyWith::get
                                                    ))
                                    ),
                            newMethSym
                    ));
        }
        newMethSym.type = new Type.MethodType(
                newMethSym.params.map(param -> param.type),
                newMethSym.type.getReturnType(),
                newMethSym.type.getThrownTypes(),
                (Symbol.TypeSymbol) newMethSym.owner
        );
        return newMethSym;
    }

    public static Stream<Symbol> getAllAccessibleMembers(
            Symbol.ClassSymbol classSymbol,
            Name name,
            Symbol.ClassSymbol childClassSymbol
    ) {
        var result = StreamSupport.stream(classSymbol.members().getSymbolsByName(name).spliterator(), false);
        if (classSymbol.getSuperclass() != Type.noType) {
            result = Stream.concat(
                    result,
                    filterAccessibleMembers(
                            classSymbol,
                            getAllAccessibleMembers(
                                    (Symbol.ClassSymbol) classSymbol.getSuperclass().tsym,
                                    name,
                                    classSymbol
                            )
                    )

            );
        }
        result = Stream.concat(
                result,
                filterAccessibleMembers(
                        classSymbol,
                        classSymbol.getInterfaces().stream()
                                .flatMap(iface -> getAllAccessibleMembers(
                                        (Symbol.ClassSymbol) iface.tsym,
                                        name,
                                        classSymbol
                                ))
                )
        );
        return result.map(sym -> updateMethSymbol(classSymbol, childClassSymbol, (Symbol.MethodSymbol) sym));
    }

    public static Type replaceAllTypeVars(Type type, Map<Name, Type> generics) {
        return switch (type) {
            case Type.TypeVar typeVar -> {
                if (generics.containsKey(typeVar.tsym.name)) {
                    yield generics.get(typeVar.tsym.name);
                }
                yield type;
            }
            case Type.ClassType classType -> new Type.ClassType(
                    classType.getEnclosingType(),
                    classType.getTypeArguments().stream()
                            .map(typeArg -> replaceAllTypeVars(typeArg, generics))
                            .collect(com.sun.tools.javac.util.List.collector()),
                    classType.tsym
            );
            case Type.ArrayType arrayType -> new Type.ArrayType(
                    replaceAllTypeVars(arrayType.elemtype, generics),
                    arrayType.tsym
            );
            case Type.WildcardType wildcardType -> new Type.WildcardType(
                    replaceAllTypeVars(wildcardType.type, generics),
                    wildcardType.kind,
                    wildcardType.tsym
            );
            case null, default -> type;
        };
    }

    @SuppressWarnings("unchecked")
    private static boolean isRefAssignableTo(JCTree.JCMemberReference ref,
                                             Symbol.MethodSymbol lambdaMethSym,
                                             Map<Name, Type> generics,
                                             Map<Name, ? extends List<Name>> genericsConversionMap,
                                             TreePath currentPath) {
        if (lambdaMethSym == null) {
            return false;
        }
        Symbol refSym;
        if (ref.sym == null) {
            var tree = ExpressionMaker.makeFieldAccess(ref.expr, ref.name.toString());
            attributeExpression(tree, currentPath);
            var ownerType = tree.selected.type;
            refSym = StreamSupport.stream(
                    ownerType.tsym.members().getSymbolsByName(ref.name, Symbol.MethodSymbol.class::isInstance).spliterator(), false
            ).findAny().orElseThrow();
        } else {
            refSym = ref.sym;
        }
        if (refSym instanceof Symbol.MethodSymbol refMethSym
                && lambdaMethSym instanceof Symbol.MethodSymbol lambdaMeth) {
            var refMethParams = refMethSym.params();
            var lambdaMethParams = replaceAllTypeVars(
                    lambdaMeth.params(),
                    Stream.concat(
                            generics.entrySet().stream()
                                    .flatMap(e ->
                                            ((Map<Name, List<Name>>) genericsConversionMap).getOrDefault(
                                                    e.getKey(),
                                                    List.of(e.getKey())
                                            ).stream().map(name ->
                                                    Map.entry(name, e.getValue())
                                            )),
                            genericsConversionMap.entrySet().stream()
                                    .filter(e -> !generics.containsKey(e.getKey()))
                                    .flatMap(e -> e.getValue().stream().map(name -> {
                                        var ident = ExpressionMaker.makeIdent(e.getKey().toString());
                                        attributeExpression(ident, PathUtils.getElementPath(refSym));
                                        return Map.entry(name, ident.type);
                                    }))
                    ).collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    )));
            return lambdaMethParams.size() <= refMethParams.size() &&
                    IntStream.range(0, lambdaMethParams.size()).allMatch(argIndex ->
                            isAssignable(
                                    getBoxedType(lambdaMethParams.get(argIndex).type),
                                    getBoxedType(refMethParams.get(argIndex).type)
                            ));
        }
        return false;
    }

    public static List<Symbol.MethodSymbol> getMatchingMethods(Name methName,
                                                               List<JCTree.JCExpression> passedArgs,
                                                               JCTree.JCExpression parentOwner,
                                                               AtomicReference<Map<Name, Type>> genericTypes,
                                                               TreePath currentPath) {
        var parentSym = getSymbol(parentOwner, currentPath);
        parentSym.complete();
        if (parentSym instanceof Symbol.ClassSymbol parentClass) {
            return getAllAccessibleMembers(parentClass, methName, null)
                    .filter(Symbol.MethodSymbol.class::isInstance)
                    .map(Symbol.MethodSymbol.class::cast)
                    .filter(isMethodSuitable(passedArgs, genericTypes, currentPath))
                    .toList();
        } else {
            throw new IllegalStateException("Unexpected parent symbol: " + parentSym);
        }
    }

    private static Symbol getSymbol(JCTree.JCExpression parent, TreePath currentPath) {
        var parentSym = switch (parent) {
            case JCTree.JCMethodInvocation meth -> getSymbolInner(switch (meth.meth) {
                case JCTree.JCFieldAccess fieldAccess -> fieldAccess.selected;
                case JCTree.JCIdent ident -> ident;
                default -> throw new IllegalStateException("Unexpected method invocation: " + meth);
            }, currentPath);
            case JCTree.JCNewClass cl -> getSymbolInner(cl.clazz, currentPath);
            case JCTree.JCMemberReference jcMemberReference -> getSymbolInner(jcMemberReference.expr, currentPath);
            default -> getSymbolInner(parent, currentPath);
        };
        var className = CUContext.from(currentPath.getCompilationUnit())
                .getMatchingImportedOrStaticClass(parentSym.getQualifiedName().toString())
                .orElse(parentSym.getQualifiedName().toString());
        return getTypeByName(className).tsym;
    }

    private static Symbol getSymbolInner(JCTree.JCExpression parentOwner, TreePath currentPath) {
        return Objects.requireNonNullElseGet(TreeInfo.symbol(parentOwner), () -> {
            attributeExpression(parentOwner, currentPath);
            return TreeInfo.symbol(parentOwner);
        });
    }

    private static Predicate<Symbol.MethodSymbol> isMethodSuitable(List<JCTree.JCExpression> passedArgs,
                                                                   AtomicReference<Map<Name, Type>> genericTypes,
                                                                   TreePath currentPath) {
        return method -> {
            if (genericTypes.get() == null || genericTypes.get().isEmpty()) {
                try {
                    genericTypes.set(inferGenerics(
                            passedArgs,
                            method.getParameters(),
                            getTypeArgs(method),
                            currentPath)
                    );
                } catch (IllegalStateException e) {
                    return false;
                }
            }
            return method.getParameters().size() >= passedArgs.size()
                    && IntStream.range(0, passedArgs.size())
                    .allMatch(i -> isArgSuitable(
                            passedArgs.get(i),
                            i,
                            method.getParameters().get(i),
                            genericTypes.get(),
                            method,
                            currentPath
                    ));
        };
    }

    private static boolean isArgSuitable(
            JCTree.JCExpression passedArg,
            int argIndex,
            Symbol.VarSymbol reqArg,
            Map<Name, Type> generics,
            Symbol.MethodSymbol methodSym,
            TreePath currentPath) {
        return passedArg instanceof JCTree.JCMemberReference ref
                && isRefAssignableTo(
                ref,
                getFuncMethod(reqArg.type.tsym, generics),
                generics,
                getGenericsConversionMap(methodSym, argIndex),
                currentPath
        )
                || isAssignable(
                getBoxedType(passedArg.type),
                getBoxedType(reqArg.type))
                || generics.containsKey(ExpressionMaker.makeName(reqArg.type.toString()))
                || !reqArg.type.getTypeArguments().isEmpty()
                && isAssignable(
                replaceAllTypeVars(
                        reqArg.type,
                        generics
                ),
                passedArg.type
        );
    }

    private static Map<Name, ? extends List<Name>> getGenericsConversionMap(Symbol.MethodSymbol methodSym, int argIndex) {
        var funcMethod = getFuncMethod(
                methodSym.getParameters().get(argIndex).type.tsym, Collections.emptyMap()
        );
        if (funcMethod == null) {
            return Collections.emptyMap();
        }
        var funcMethTypeArgs = getTypeArgs(funcMethod);
        var atomicCounter = new AtomicInteger(0);
        return methodSym.getParameters().get(argIndex).type.getTypeArguments().stream()
                .collect(Collectors.toMap(
                        type -> type instanceof Type.WildcardType
                                ? ((Type.WildcardType) type).type.tsym.name
                                : type.tsym.name,
                        type -> new ArrayList<>(
                                List.of(funcMethTypeArgs.get(atomicCounter.getAndIncrement()).tsym.name)
                        ),
                        (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        }
                ));
    }

    private static List<Symbol.VarSymbol> replaceAllTypeVars(List<Symbol.VarSymbol> symbols, Map<Name, Type> generics) {
        return symbols.stream().map(sym -> new Symbol.VarSymbol(
                sym.flags_field,
                sym.name,
                replaceAllTypeVars(sym.type, generics),
                sym.owner
        )).toList();
    }
}
