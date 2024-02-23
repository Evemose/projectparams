package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.visitors.AbstractVisitor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class MemberRefsToLambdasVisitor extends AbstractVisitor<Void, Void> {
    public MemberRefsToLambdasVisitor(Trees trees, Messager messager) {
        super(trees, messager);
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
            case JCTree.JCVariableDecl varDecl -> {/*pass for now*/}
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


    private List<JCTree.JCVariableDecl> getPassedArgs(JCTree parent, JCTree.JCMemberReference memberReference) {
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
        var requiredLambdaMeth = parentInfo.possibleMethods().getFirst().getParameters()
                .get(parentInfo.args().indexOf(memberReference)).type;
        if (requiredLambdaMeth.tsym instanceof Symbol.TypeVariableSymbol) {
            requiredLambdaMeth = parentInfo.genericTypes().get().get(requiredLambdaMeth.tsym.name);
        }
        var lambdaMethodSym = Objects.requireNonNull(getFuncMethod(requiredLambdaMeth.tsym, Collections.emptyMap()));
        var lambdaMethodType = (Type.MethodType) lambdaMethodSym.type;
        return IntStream.range(0, lambdaMethodType.argtypes.size())
                .mapToObj(i -> {
                    var type = lambdaMethodType.argtypes.get(i);
                    return ExpressionMaker.makeVariableDecl(
                            "arg" + type.toString().hashCode() + "UNIQUEENDING",
                            type instanceof Type.TypeVar
                                    ? getInferredType(parentInfo, i, lambdaMethodSym)
                                    : type);
                }).toList();
    }

    private ParentInfo getParentInfo(JCTree parent) {
        return switch (parent) {
            case JCTree.JCMethodInvocation meth -> getParentInfoFromMeth(meth);
            case JCTree.JCNewClass cl -> getParentInfoFromNewClass(cl);
            case JCTree.JCVariableDecl varDecl -> {
                throw new IllegalStateException("Not implemented for variable declaration: " + varDecl);
                // pass
            }
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
            // TODO: implement for other cases
            parentOwner = null;
        }
        var genericTypes = new AtomicReference<>(meth.typeargs.stream().collect(Collectors.toMap(
                MemberRefsToLambdasVisitor::getName,
                typeArg -> TypeUtils.getTypeByName(getName(typeArg).toString())
        )));
        return new ParentInfo(meth.args,
                getMatchingMethods(getName(meth.meth), meth.args, parentOwner, genericTypes),
                genericTypes);
    }

    private record ParentInfo(com.sun.tools.javac.util.List<JCTree.JCExpression> args,
                              List<Symbol.MethodSymbol> possibleMethods,
                              AtomicReference<Map<Name, Type>> genericTypes) {
    }

    private static Type getInferredType(ParentInfo parentInfo, int argIndex, Symbol.MethodSymbol lambdaMethSym) {
        var definitionArgs = parentInfo.possibleMethods().getFirst().params();
        var actualType = definitionArgs.get(argIndex).type.tsym instanceof Symbol.TypeVariableSymbol
                ? parentInfo.genericTypes().get().get(definitionArgs.get(argIndex).type.tsym.name)
                : definitionArgs.get(argIndex).type;
        if (actualType.tsym instanceof Symbol.ClassSymbol classSymbol
                && classSymbol.isInterface()) {
            return actualType.allparams().get(
                    getTypeParams(lambdaMethSym).indexOf(
                            lambdaMethSym.params().get(argIndex).type
                    )
            );
        }
        throw new IllegalStateException("Unexpected type: " + actualType);
    }

    private static List<Type> getTypeParams(Symbol lambdaMethSym) {
        var res = new ArrayList<>(lambdaMethSym.type.getTypeArguments());
        res.addAll(lambdaMethSym.owner.type.getTypeArguments());
        return res;
    }

    private boolean isRefAssignableTo(JCTree.JCMemberReference ref,
                                      Symbol.MethodSymbol lambdaMethSym,
                                      Map<Name, Type> generics,
                                      Symbol.VarSymbol reqArgSym) {
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
            var lambdaMethParams = lambdaMeth.params();
            return  lambdaMethParams.size() <= refMethParams.size() &&
                    IntStream.range(0, lambdaMethParams.size()).allMatch(argIndex -> {
                        if (TypeUtils.isAssignable(
                                TypeUtils.getBoxedType(lambdaMethParams.get(argIndex).type),
                                TypeUtils.getBoxedType(refMethParams.get(argIndex).type))) {
                            return true;
                        }
                        var genType = getTypeParams(reqArgSym).get(
                                getTypeParams(lambdaMethSym).indexOf(
                                        lambdaMethParams.get(argIndex).type
                                )
                        );
                        return reqArgSym.type.tsym instanceof Symbol.TypeVariableSymbol ?
                                TypeUtils.isAssignable(
                                        generics.get(reqArgSym.type.tsym.name),
                                        TypeUtils.getBoxedType(refMethParams.get(argIndex).type)
                                ) : TypeUtils.isAssignable(
                                genType,
                                TypeUtils.getBoxedType(refMethParams.get(argIndex).type)
                        );
                    }
            );
        }
        return false;
    }

    private Symbol.MethodSymbol getFuncMethod(Symbol lambdaClassSym, Map<Name, Type> generics) {
        if (lambdaClassSym instanceof Symbol.TypeVariableSymbol) {
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
        var symGenerics = methodSym.getTypeParameters();
        var passedArgsTypes = args.stream().map(arg -> {
            var actualType = TypeUtils.getActualType(arg);
            if (actualType == null && !(arg instanceof JCTree.JCMemberReference)) {
                TypeUtils.attributeExpression(arg, getCurrentPath());
                actualType = TypeUtils.getActualType(arg);
            }
            return actualType;
        }).toList();
        return symGenerics.stream()
                .collect(Collectors.toMap(
                        symGeneric -> symGeneric.name,
                        symGeneric -> getGenericType(symGeneric, passedArgsTypes, methodSym.getParameters())
                ));
    }

    private Type getGenericType(Symbol.TypeVariableSymbol symGeneric, List<Type> passedArgsTypes, com.sun.tools.javac.util.List<Symbol.VarSymbol> parameters) {
        var index = IntStream.range(0, parameters.size())
                .filter(i -> Objects.equals(parameters.get(i).type.tsym, symGeneric))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No parameter for generic: " + symGeneric));
        if (index >= passedArgsTypes.size()) {
            throw new IllegalStateException("No passed type for generic: " + symGeneric);
        }
        return passedArgsTypes.get(index);
    }

    private List<Symbol.MethodSymbol> getMatchingMethods(Name methName,
                                                         List<JCTree.JCExpression> args,
                                                         JCTree.JCExpression parentOwner,
                                                         AtomicReference<Map<Name, Type>> genericTypes) {
        var parentSym = TreeInfo.symbol(parentOwner);
        if (parentSym == null) {
            TypeUtils.attributeExpression(parentOwner, getCurrentPath());
            parentSym = TreeInfo.symbol(parentOwner);
        }
        parentSym = TypeUtils.getTypeByName(parentSym.getQualifiedName().toString()).tsym;
        parentSym.complete();
        if (parentSym instanceof Symbol.ClassSymbol parentClass) {
            return StreamSupport.stream(parentClass.members().getSymbolsByName(methName).spliterator(), false)
                    .filter(Symbol.MethodSymbol.class::isInstance)
                    .map(Symbol.MethodSymbol.class::cast)
                    .filter(isMethodSuitable(args, genericTypes))
                    .toList();
        } else {
            throw new IllegalStateException("Unexpected parent symbol: " + parentSym);
        }
    }

    private Predicate<Symbol.MethodSymbol> isMethodSuitable(List<JCTree.JCExpression> args, AtomicReference<Map<Name, Type>> genericTypes) {
        return method -> {
            if (genericTypes.get() == null || genericTypes.get().isEmpty()) {
                try {
                    genericTypes.set(inferGenerics(args, method));
                } catch (IllegalStateException e) {
                    return false;
                }
            }
            return method.getParameters().size() == args.size()
                    && IntStream.range(0, args.size())
                    .allMatch(i -> isArgSuitable(
                            args.get(i),
                            method.getParameters().get(i),
                            genericTypes.get()
                    ));
        };
    }

    private boolean isArgSuitable(
            JCTree.JCExpression passedArg,
            Symbol.VarSymbol reqArg,
            Map<Name, Type> generics) {
        return passedArg instanceof JCTree.JCMemberReference ref
                && isRefAssignableTo(ref, getFuncMethod(reqArg.type.tsym, generics), generics, reqArg)
                || TypeUtils.isAssignable(passedArg.type, reqArg.type)
                || generics.containsKey(ExpressionMaker.makeName(reqArg.type.toString()));
    }

    private static Name getName(JCTree.JCExpression expr) {
        return switch (expr) {
            case JCTree.JCIdent ident -> ident.name;
            case JCTree.JCFieldAccess fieldAccess -> fieldAccess.name;
            default -> throw new IllegalStateException("Unexpected type argument: " + expr);
        };
    }
}
