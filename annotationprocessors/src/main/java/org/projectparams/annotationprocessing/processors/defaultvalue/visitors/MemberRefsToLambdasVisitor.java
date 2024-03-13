package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.CUContext;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.visitors.AbstractVisitor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MemberRefsToLambdasVisitor extends AbstractVisitor<Void, Void> {
    public MemberRefsToLambdasVisitor(Trees trees, Messager messager) {
        super(trees, messager);
    }

    private static JCTree.JCExpression getBodyExpr(JCTree.JCMemberReference memberReference, List<JCTree.JCVariableDecl> args) {
        if (memberReference.mode == MemberReferenceTree.ReferenceMode.INVOKE) {
            return ExpressionMaker.makeMethodInvocation(
                    ExpressionMaker.makeFieldAccess(
                            memberReference.expr,
                            memberReference.name.toString()
                    ),
                    memberReference.typeargs,
                    varDeclsToArgs(args)
            );
        } else {
            return ExpressionMaker.makeNewClass(
                    null,
                    memberReference.expr.toString(),
                    memberReference.typeargs,
                    varDeclsToArgs(args)
            );
        }
    }

    private static JCTree.JCExpression[] varDeclsToArgs(List<JCTree.JCVariableDecl> args) {
        return args.stream().map(MemberRefsToLambdasVisitor::toArg).toArray(JCTree.JCExpression[]::new);
    }

    private static JCTree.JCExpression toArg(JCTree.JCVariableDecl varDecl) {
        var arg = ExpressionMaker.makeIdent(varDecl.name.toString());
        arg.type = varDecl.type;
        return arg;
    }

    private static List<Type> getTypeArgs(Symbol sym) {
        var res = new ArrayList<>(sym.type.getTypeArguments());
        res.addAll(sym.owner.type.getTypeArguments());
        return res;
    }

    private static Name getName(JCTree.JCExpression expr) {
        return switch (expr) {
            case JCTree.JCIdent ident -> ident.name;
            case JCTree.JCFieldAccess fieldAccess -> fieldAccess.name;
            default -> throw new IllegalStateException("Unexpected type argument: " + expr);
        };
    }

    @Override
    public Void visitMemberReference(com.sun.source.tree.MemberReferenceTree node, Void ignored) {
        var parent = getCurrentPath().getParentPath().getLeaf();
        List<JCTree.JCExpression> temp;
        var lambda = toLambda((JCTree.JCMemberReference) node, (JCTree) parent);
        if (lambda == null) {
            return super.visitMemberReference(node, ignored);
        }
        switch (parent) {
            case JCTree.JCMethodInvocation meth -> {
                temp = new ArrayList<>(meth.args);
                temp.set(temp.indexOf(node), lambda);
                ((JCTree.JCMethodInvocation) parent).args = com.sun.tools.javac.util.List.from(temp);
            }
            case JCTree.JCNewClass cl -> {
                temp = new ArrayList<>(cl.args);
                temp.set(temp.indexOf(node), lambda);
                ((JCTree.JCNewClass) parent).args = com.sun.tools.javac.util.List.from(temp);
            }
            case JCTree.JCVariableDecl varDecl -> varDecl.init = lambda;
            default -> {/*pass*/}
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "Replaced member reference with lambda in : " + parent);
        return super.visitMemberReference(node, ignored);
    }

    private JCTree.JCLambda toLambda(JCTree.JCMemberReference memberReference, JCTree parent) {
        try {
            var args = getPassedArgs(parent, memberReference);
            return ExpressionMaker.makeLambda(
                    args,
                    getBodyExpr(memberReference, args)
            );
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private List<JCTree.JCVariableDecl> getPassedArgs(JCTree parent, JCTree.JCMemberReference memberReference) {
        if (parent instanceof JCTree.JCVariableDecl varDecl) {
            return getPassedArgsFromVariableDecl(varDecl);
        } else {
            return getPassedArgsFromInvocableParent(parent, memberReference);
        }
    }

    private List<JCTree.JCVariableDecl> getPassedArgsFromVariableDecl(JCTree.JCVariableDecl varDecl) {
        varDecl.init = null;
        var typeArgs = Objects.requireNonNullElseGet(varDecl.vartype.type,
                () -> {
                    TypeUtils.attributeExpression(varDecl, getCurrentPath());
                    return varDecl.vartype.type;
                }).getTypeArguments();
        var funcMethod = getFuncMethod(varDecl.vartype.type.tsym, Collections.emptyMap());
        List<Type> expectedArgs = new ArrayList<>(Objects.requireNonNull(funcMethod).params().map(param -> param.type));
        expectedArgs.replaceAll(type -> type.tsym instanceof Symbol.TypeVariableSymbol
                ? typeArgs.get(getTypeArgs(funcMethod).indexOf(type))
                : type);
        var atomicCounter = new AtomicInteger(0);
        return expectedArgs.stream().map(type -> ExpressionMaker.makeVariableDecl(
                "arg" + Math.abs(type.toString().hashCode()) + "UNIQUEENDING" + atomicCounter.getAndIncrement(),
                type
        )).toList();
    }

    private List<JCTree.JCVariableDecl> getPassedArgsFromInvocableParent(JCTree parent, JCTree.JCMemberReference memberReference) {
        var parentInfo = getParentInfo(parent);
        if (parentInfo.possibleMethods().size() == 1) {
            return getPassedArgsInner(memberReference, parentInfo);
        } else if (parentInfo.possibleMethods().isEmpty()) {
            throw new IllegalStateException("No matching methods for: " + parent);
        } else {
            throw new IllegalStateException("Ambiguous methods: " + parentInfo.possibleMethods());
        }
    }

    private List<JCTree.JCVariableDecl> getPassedArgsInner(JCTree.JCMemberReference memberReference, ParentInfo parentInfo) {
        var funcInterface = replaceAllTypeVars(
                parentInfo.possibleMethods().getFirst().getParameters()
                        .get(parentInfo.passedArgs().indexOf(memberReference)).type,
                parentInfo.genericTypes().get()
        );
        var lambdaMethSym = Objects.requireNonNull(
                getFuncMethod(funcInterface.tsym, parentInfo.genericTypes().get())
        );
        if (getDeclArgSym(memberReference, parentInfo) instanceof Symbol.TypeVariableSymbol) {
            return getArgsForLambdaAsGeneric(lambdaMethSym, funcInterface);
        }
        var typeArgsInParams = lambdaMethSym.params()
                .stream()
                .filter(param -> param.type instanceof Type.TypeVar)
                .map(param -> param.type)
                .toList();
        var lambdaMethArgTypes = getConvertedLambdaMethodArgumentTypes(
                typeArgsInParams, lambdaMethSym, funcInterface
        );
        return IntStream.range(0, lambdaMethArgTypes.size())
                .mapToObj(i -> {
                    var type = lambdaMethArgTypes.get(i);
                    return ExpressionMaker.makeVariableDecl(
                            "arg" + Math.abs(type.toString().hashCode()) + "UNIQUEENDING" + i,
                            type instanceof Type.TypeVar
                                    ? parentInfo.genericTypes().get().get(type.tsym.name)
                                    : type instanceof Type.WildcardType ?
                                    ((Type.WildcardType) type).type :
                                    type);
                }).toList();
    }

    private com.sun.tools.javac.util.List<Type> getConvertedLambdaMethodArgumentTypes(List<Type> typeArgsInParams, Symbol.MethodSymbol lambdaMethSym, Type requiredLambdaMeth) {
        var conversionMap = getGenericsConversionMap(typeArgsInParams, lambdaMethSym, requiredLambdaMeth);
        return lambdaMethSym.params()
                .map(param -> replaceAllTypeVars(param.type, conversionMap));
    }

    private static Map<Name, Type> getGenericsConversionMap(
            List<Type> typeArgsInParams,
            Symbol.MethodSymbol lambdaMethSym,
            Type requiredLambdaMeth) {
        return IntStream.range(0, typeArgsInParams.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> typeArgsInParams.get(i) instanceof Type.WildcardType?
                                ((Type.WildcardType) typeArgsInParams.get(i)).type.tsym.name :
                                typeArgsInParams.get(i).tsym.name,
                        i -> {
                            var typeArgsOfFuncInterface = getTypeArgs(lambdaMethSym);
                            return requiredLambdaMeth.getTypeArguments().get(
                                    typeArgsOfFuncInterface.indexOf(typeArgsInParams.get(i))
                            );
                        }
                ));
    }

    private static List<JCTree.JCVariableDecl> getArgsForLambdaAsGeneric(Symbol.MethodSymbol lambdaMethSym, Type fincInterface) {
        return lambdaMethSym.params().stream().map(param -> ExpressionMaker.makeVariableDecl(
                "arg" + Math.abs(param.type.toString().hashCode())
                        + "UNIQUEENDING" + param.toString().hashCode(),
                param.type instanceof Type.TypeVar
                        ? fincInterface.getTypeArguments().get(
                        getTypeArgs(lambdaMethSym).indexOf(param.type))
                        : param.type
        )).toList();
    }

    private static Symbol.TypeSymbol getDeclArgSym(JCTree.JCMemberReference memberReference, ParentInfo parentInfo) {
        return parentInfo.possibleMethods().getFirst().params().get(
                parentInfo.passedArgs().indexOf(memberReference))
                .type.tsym;
    }

    private ParentInfo getParentInfo(JCTree parent) {
        return switch (parent) {
            case JCTree.JCMethodInvocation meth -> getParentInfoFromMeth(meth);
            case JCTree.JCNewClass cl -> getParentInfoFromNewClass(cl);
            case null, default -> throw new IllegalStateException("Unexpected parent: " + parent);
        };
    }

    private ParentInfo getParentInfoFromNewClass(JCTree.JCNewClass cl) {
        var genericTypes = new AtomicReference<>(cl.typeargs.stream().collect(Collectors.toMap(
                MemberRefsToLambdasVisitor::getName,
                typeArg -> TypeUtils.getTypeByName(getName(typeArg).toString())
        )));
        return new ParentInfo(cl.args,
                getMatchingMethods(ExpressionMaker.makeName("<init>"), cl.args, cl.clazz, genericTypes),
                genericTypes);
    }

    private ParentInfo getParentInfoFromMeth(JCTree.JCMethodInvocation meth) {
        JCTree.JCExpression parentOwner;
        if (meth.meth instanceof JCTree.JCFieldAccess fieldAccess) {
            parentOwner = fieldAccess.selected;
        } else {
            parentOwner = ClassContext.of(PathUtils.getEnclosingClassPath(getCurrentPath()))
                    .getMatchingMethod(meth.meth.toString())
                    .map(m -> ExpressionMaker.makeIdent(m.className()))
                    .orElseThrow();
        }
        var genericTypes = new AtomicReference<>(meth.typeargs.stream().collect(Collectors.toMap(
                MemberRefsToLambdasVisitor::getName,
                typeArg -> TypeUtils.getTypeByName(getName(typeArg).toString())
        )));
        return new ParentInfo(meth.args,
                getMatchingMethods(getName(meth.meth), meth.args, parentOwner, genericTypes),
                genericTypes);
    }

    @SuppressWarnings("unchecked")
    private boolean isRefAssignableTo(JCTree.JCMemberReference ref,
                                      Symbol.MethodSymbol lambdaMethSym,
                                      Map<Name, Type> generics,
                                      Map<Name, ? extends List<Name>> genericsConversionMap) {
        if (lambdaMethSym == null) {
            return false;
        }
        var refSym = ref.sym;
        if (refSym == null) {
            var tree = ExpressionMaker.makeFieldAccess(ref.expr, ref.name.toString());
            TypeUtils.attributeExpression(tree, getCurrentPath());
            var ownerType = tree.selected.type;
            refSym = StreamSupport.stream(
                    ownerType.tsym.members().getSymbolsByName(ref.name, Symbol.MethodSymbol.class::isInstance).spliterator(), false
            ).findAny().orElseThrow();
        }
        if (refSym instanceof Symbol.MethodSymbol refMethSym
                && lambdaMethSym instanceof Symbol.MethodSymbol lambdaMeth) {
            var refMethParams = refMethSym.params();
            var lambdaMethParams = replaceAllTypeVars(
                    lambdaMeth.params(),
                    generics.entrySet().stream()
                            .flatMap(e ->
                                    ((Map<Name, List<Name>>) genericsConversionMap).getOrDefault(
                                            e.getKey(),
                                            List.of(e.getKey())
                                    ).stream().map(name ->
                                            Map.entry(name, e.getValue())
                            )).collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue
                            )));
            return lambdaMethParams.size() <= refMethParams.size() &&
                    IntStream.range(0, lambdaMethParams.size()).allMatch(argIndex ->
                            TypeUtils.isAssignable(
                                    TypeUtils.getBoxedType(lambdaMethParams.get(argIndex).type),
                                    TypeUtils.getBoxedType(refMethParams.get(argIndex).type)
                            ));
        }
        return false;
    }

    private Symbol.MethodSymbol getFuncMethod(Symbol lambdaClassSym, Map<Name, Type> generics) {
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

    private Map<Name, Type> inferGenerics(List<JCTree.JCExpression> args, Symbol.MethodSymbol methodSym) throws IllegalStateException {
        var symGenerics = getTypeArgs(methodSym).stream()
                .map(type -> (Symbol.TypeVariableSymbol) type.tsym).toList();
        return symGenerics.stream()
                .collect(Collectors.toMap(
                        symGeneric -> symGeneric.name,
                        symGeneric -> getActualGenericType(symGeneric, args, methodSym.getParameters())
                ));
    }

    private Type getActualGenericType(Symbol.TypeVariableSymbol symGeneric,
                                      List<JCTree.JCExpression> args,
                                      com.sun.tools.javac.util.List<Symbol.VarSymbol> parameters) {
        var passedArgsTypes = args.stream().map(arg -> {
            var actualType = TypeUtils.getActualType(arg);
            if (actualType == null && !(arg instanceof JCTree.JCMemberReference)) {
                TypeUtils.attributeExpression(arg, getCurrentPath());
                actualType = TypeUtils.getActualType(arg);
            }
            return actualType;
        }).toList();
        var index = IntStream.range(0, parameters.size())
                .filter(i -> Objects.equals(parameters.get(i).type.tsym, symGeneric))
                .findFirst();
        if (index.isEmpty()) {
            return tryInferFromArgTypeArgs(symGeneric, parameters, args).orElse(
                    tryInferFromMemberRefs(symGeneric, args, parameters)
                            .orElseThrow(() -> new IllegalStateException("No passed type for generic: " + symGeneric)));
        }
        if (index.getAsInt() >= passedArgsTypes.size()) {
            throw new IllegalStateException("No passed type for generic: " + symGeneric);
        }
        return passedArgsTypes.get(index.getAsInt());
    }

    private Optional<Type> tryInferFromMemberRefs(
            Symbol.TypeVariableSymbol symGeneric,
            List<JCTree.JCExpression> args,
            com.sun.tools.javac.util.List<Symbol.VarSymbol> parameters
    ) {
        var memberRefsMap = getMemberRefsMap(args);
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
                    var inferredInLambda = lambdaParamTypes.stream()
                            .collect(Collectors.toMap(
                                    type -> type.tsym.name,
                                    type -> entry.getValue().params().get(
                                            IntStream.range(0, lambdaParamTypes.size())
                                                    .filter(i -> Objects.equals(
                                                            funcMethod.params().get(i).type, type
                                                    ))
                                                    .findFirst().orElseThrow(() -> new IllegalStateException(
                                                            "No matching type for: " + type
                                                    ))
                                    ).type
                            ));
                    var inferred = inferredInLambda.get(conversionMap.getOrDefault(symGeneric.name,
                            symGeneric.type).tsym.name);
                    if (inferred == null) {
                        return null;
                    }
                    return TypeUtils.getBoxedType(
                            inferred instanceof Type.TypeVar
                                    ? entry.getKey().getTypeArguments().get(
                                    getTypeArgs(entry.getValue()).indexOf(inferred)
                            ).type
                                    : inferred
                    );
                }).filter(Objects::nonNull).findFirst();
    }

    private Map<JCTree.JCMemberReference, Symbol.MethodSymbol> getMemberRefsMap(
            List<JCTree.JCExpression> args
    ) {
        return args.stream()
                .filter(JCTree.JCMemberReference.class::isInstance)
                .map(JCTree.JCMemberReference.class::cast)
                .collect(Collectors.toMap(
                        ref -> ref,
                        ref -> getAllAccessibleMembers((Symbol.ClassSymbol) Objects.requireNonNullElseGet(
                                ref.expr.type, () -> {
                                    var temp = ExpressionMaker.makeFieldAccess(ref.expr, ref.name.toString());
                                    TypeUtils.attributeExpression(temp, getCurrentPath());
                                    return temp.selected.type;
                                }).tsym, ref.getName(), null)
                                .filter(Symbol.MethodSymbol.class::isInstance)
                                .map(Symbol.MethodSymbol.class::cast)
                                .findFirst().orElseThrow()
                ));
    }

    private Optional<Type> tryInferFromArgTypeArgs(Symbol.TypeVariableSymbol symGeneric,
                                                   List<Symbol.VarSymbol> parameters,
                                                   List<JCTree.JCExpression> args) {
        for (var i = 0; i < parameters.size(); i++) {
            var inferred = tryInferFromArgTypeArgsInner(symGeneric, parameters.get(i).type, args.get(i).type);
            if (inferred.isPresent()) {
                return inferred;
            }
        }
        return Optional.empty();
    }

    private Optional<Type> tryInferFromArgTypeArgsInner(
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

    private List<Symbol.MethodSymbol> getMatchingMethods(Name methName,
                                                         List<JCTree.JCExpression> passedArgs,
                                                         JCTree.JCExpression parentOwner,
                                                         AtomicReference<Map<Name, Type>> genericTypes) {
        var parentSym = getParentSymbol(parentOwner);
        parentSym.complete();
        if (parentSym instanceof Symbol.ClassSymbol parentClass) {
            return getAllAccessibleMembers(parentClass, methName, null)
                    .filter(Symbol.MethodSymbol.class::isInstance)
                    .map(Symbol.MethodSymbol.class::cast)
                    .filter(isMethodSuitable(passedArgs, genericTypes))
                    .toList();
        } else {
            throw new IllegalStateException("Unexpected parent symbol: " + parentSym);
        }
    }

    private Stream<Symbol> filterAccessibleMembers(
            Symbol.ClassSymbol classSymbol,
            Stream<Symbol> stream
    ) {
        return stream.filter(s -> TypeUtils.isAccessible(s, classSymbol))
                .filter(s -> !s.isStatic())
                .filter(Symbol.MethodSymbol.class::isInstance);
    }

    private Symbol.MethodSymbol updateMethSymbol(Symbol.ClassSymbol classSymbol, Symbol.ClassSymbol classToUnifyTypesWith, Symbol.MethodSymbol sym) {
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
                    TypeUtils.getTypeByName("java.lang.Object"),
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

    private Stream<Symbol> getAllAccessibleMembers(
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

    private Symbol getParentSymbol(JCTree.JCExpression parentOwner) {
        var parentSym = switch (parentOwner) {
            case JCTree.JCMethodInvocation meth -> getSymbol(switch (meth.meth) {
                case JCTree.JCFieldAccess fieldAccess -> fieldAccess.selected;
                case JCTree.JCIdent ident -> ident;
                default -> throw new IllegalStateException("Unexpected method invocation: " + meth);
            });
            case JCTree.JCNewClass cl -> getSymbol(cl.clazz);
            case JCTree.JCMemberReference jcMemberReference -> getSymbol(jcMemberReference.expr);
            default -> getSymbol(parentOwner);
        };
        var className = CUContext.from(getCurrentPath().getCompilationUnit())
                .getMatchingImportedOrStaticClass(parentSym.getQualifiedName().toString())
                .orElse(parentSym.getQualifiedName().toString());
        return TypeUtils.getTypeByName(className).tsym;
    }

    private Symbol getSymbol(JCTree.JCExpression parentOwner) {
        return Objects.requireNonNullElseGet(TreeInfo.symbol(parentOwner), () -> {
            TypeUtils.attributeExpression(parentOwner, getCurrentPath());
            return TreeInfo.symbol(parentOwner);
        });
    }

    private Predicate<Symbol.MethodSymbol> isMethodSuitable(List<JCTree.JCExpression> passedArgs,
                                                            AtomicReference<Map<Name, Type>> genericTypes) {
        return method -> {
            if (genericTypes.get() == null || genericTypes.get().isEmpty()) {
                try {
                    genericTypes.set(inferGenerics(passedArgs, method));
                } catch (IllegalStateException e) {
                    return false;
                }
            }
            return method.getParameters().size() == passedArgs.size()
                    && IntStream.range(0, passedArgs.size())
                    .allMatch(i -> isArgSuitable(
                            passedArgs.get(i),
                            i,
                            method.getParameters().get(i),
                            genericTypes.get(),
                            method
                    ));
        };
    }

    private boolean isArgSuitable(
            JCTree.JCExpression passedArg,
            int argIndex,
            Symbol.VarSymbol reqArg,
            Map<Name, Type> generics,
            Symbol.MethodSymbol methodSym) {
        return passedArg instanceof JCTree.JCMemberReference ref
                && isRefAssignableTo(
                ref,
                getFuncMethod(reqArg.type.tsym, generics),
                generics,
                getGenericsConversionMap(methodSym, argIndex)
        )
                || TypeUtils.isAssignable(passedArg.type, reqArg.type)
                || generics.containsKey(ExpressionMaker.makeName(reqArg.type.toString()))
                || !reqArg.type.getTypeArguments().isEmpty()
                && TypeUtils.isAssignable(
                replaceAllTypeVars(
                        reqArg.type,
                        generics
                ),
                passedArg.type
        );
    }

    private Map<Name, ? extends List<Name>> getGenericsConversionMap(Symbol.MethodSymbol methodSym, int argIndex) {
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

    private Type replaceAllTypeVars(Type type, Map<Name, Type> generics) {
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

    private List<Symbol.VarSymbol> replaceAllTypeVars(List<Symbol.VarSymbol> symbols, Map<Name, Type> generics) {
        return symbols.stream().map(sym -> new Symbol.VarSymbol(
                sym.flags_field,
                sym.name,
                replaceAllTypeVars(sym.type, generics),
                sym.owner
        )).toList();
    }

    private record ParentInfo(com.sun.tools.javac.util.List<JCTree.JCExpression> passedArgs,
                              List<Symbol.MethodSymbol> possibleMethods,
                              AtomicReference<Map<Name, Type>> genericTypes) {
    }
}
