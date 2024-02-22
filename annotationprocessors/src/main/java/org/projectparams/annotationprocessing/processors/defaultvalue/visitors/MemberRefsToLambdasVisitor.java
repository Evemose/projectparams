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
    @SuppressWarnings("unused")
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
                messager.printMessage(Diagnostic.Kind.NOTE, "Replaced method ref arg with lambda: " + meth);
            }
            case JCTree.JCNewClass cl -> {
                temp = new ArrayList<>(cl.args);
                temp.set(temp.indexOf(node), lambda);
                ((JCTree.JCNewClass) parent).args = com.sun.tools.javac.util.List.from(temp);
            }
            case JCTree.JCVariableDecl varDecl -> {/*pass for now*/}
            default -> {/*pass*/}
        }
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
                    args.stream().map(varDecl -> {
                        var arg = ExpressionMaker.makeIdent(varDecl.name.toString());
                        arg.type = varDecl.type;
                        return arg;
                    }).toArray(JCTree.JCExpression[]::new)
            );
        } else {
            return ExpressionMaker.makeNewClass(
                    null,
                    memberReference.expr.toString(),
                    memberReference.typeargs,
                    args.stream().map(varDecl -> {
                        var arg = ExpressionMaker.makeIdent(varDecl.name.toString());
                        arg.type = varDecl.vartype.type;
                        return arg;
                    }).toArray(JCTree.JCExpression[]::new)
            );
        }
    }


    private List<JCTree.JCVariableDecl> getPassedArgs(JCTree parent, JCTree.JCMemberReference memberReference) {
        if (parent instanceof JCTree.JCMethodInvocation meth) {
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
            var possibleMethods = getMatchingMethods(meth, parentOwner, genericTypes);
            if (possibleMethods.size() == 1) {
                var index = meth.args.indexOf(memberReference);
                var requiredLambdaMeth = possibleMethods.getFirst().getParameters().get(index).type;
                if (requiredLambdaMeth.tsym instanceof Symbol.TypeVariableSymbol) {
                    requiredLambdaMeth = genericTypes.get().get(requiredLambdaMeth.tsym.name);
                }
                var lambdaMethodType = (Type.MethodType) getFuncMethod(requiredLambdaMeth.tsym, Collections.emptyMap()).asType();
                return IntStream.range(0, lambdaMethodType.argtypes.size())
                        .mapToObj(i -> {
                            var type = lambdaMethodType.argtypes.get(i);
                            return ExpressionMaker.makeVariableDecl(
                                    "arg" + type.toString().hashCode() + "UNIQUEENDING",
                                    type.tsym instanceof Symbol.TypeVariableSymbol
                                            ? getInferredType(type, genericTypes.get(), i)
                                            : type);
                        }).toList();
            } else if (possibleMethods.isEmpty()) {
                throw new IllegalStateException("No matching methods for: " + meth);
            } else {
                throw new IllegalStateException("Ambiguous methods: " + possibleMethods);
            }
        }
        return new ArrayList<>();
    }

    private static Type getInferredType(Type type, Map<Name, Type> genericTypes, int argIndex) {
        var genericType = genericTypes.get(type.tsym.name);
        if (genericType.tsym instanceof Symbol.ClassSymbol classSymbol && classSymbol.isInterface()) {
            return genericType.allparams().get(argIndex);
        }
        return genericType;
    }

    private boolean isRefAssignableTo(JCTree.JCMemberReference ref, Symbol.MethodSymbol lambdaMethSym, Map<Name, Type> generics) {
        var refSym = TreeInfo.symbol(ref);
        if (refSym instanceof Symbol.MethodSymbol refMethSym
                && lambdaMethSym instanceof Symbol.MethodSymbol lambdaMeth) {
            var refMethParams = refMethSym.params();
            var lambdaMethParams = lambdaMeth.params();
            return lambdaMethParams.size() <= refMethParams.size()
                    && IntStream.range(0, lambdaMethParams.size())
                    .allMatch(i -> TypeUtils.isAssignable(lambdaMethParams.get(i).type, refMethParams.get(i).type)
                            || TypeUtils.isAssignable(
                            generics.get(ExpressionMaker.makeName(refMethParams.get(i).type.toString())),
                            refMethParams.get(i).type)
                    );
        }
        return false;
    }

    private Symbol.MethodSymbol getFuncMethod(Symbol lambdaClassSym, Map<Name, Type> generics) {
        if (lambdaClassSym instanceof Symbol.TypeVariableSymbol) {
            lambdaClassSym = generics.get(lambdaClassSym.name).tsym;
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

    private Map<Name, Type> inferGenerics(JCTree.JCMethodInvocation meth, Symbol.MethodSymbol methodSym) throws IllegalStateException {
        var symGenerics = methodSym.getTypeParameters();
        var passedArgsTypes = meth.args.stream().map(arg -> {
            var actualType = TypeUtils.getActualType(arg);
            if (actualType == null && !(arg instanceof JCTree.JCMemberReference)) {
                TypeUtils.attributeExpression(arg, getCurrentPath());
            }
            return TypeUtils.getActualType(arg);
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

    private List<Symbol.MethodSymbol> getMatchingMethods(JCTree.JCMethodInvocation meth,
                                                         JCTree.JCExpression parentOwner,
                                                         AtomicReference<Map<Name, Type>> genericTypes) {
        var parentSym = TreeInfo.symbol(parentOwner);
        if (parentSym == null) {
            TypeUtils.attributeExpression(parentOwner, getCurrentPath());
            parentSym = TreeInfo.symbol(parentOwner);
        }
        var parentType = TypeUtils.getTypeByName(parentSym.getQualifiedName().toString());
        parentSym = parentType.tsym;
        parentSym.complete();
        if (parentSym instanceof Symbol.ClassSymbol parentClass) {
            var methName = getName(meth.meth);
            return StreamSupport.stream(parentClass.members().getSymbolsByName(methName).spliterator(), false)
                    .filter(Symbol.MethodSymbol.class::isInstance)
                    .map(Symbol.MethodSymbol.class::cast)
                    .filter(isMethodSuitable(meth, genericTypes)
                    ).toList();
        } else {
            throw new IllegalStateException("Unexpected parent symbol: " + parentSym);
        }
    }

    private Predicate<Symbol.MethodSymbol> isMethodSuitable(JCTree.JCMethodInvocation meth, AtomicReference<Map<Name, Type>> genericTypes) {
        return method -> {
            if (genericTypes.get() == null || genericTypes.get().isEmpty()) {
                try {
                    genericTypes.set(inferGenerics(meth, method));
                } catch (IllegalStateException e) {
                    return false;
                }
            }
            return method.getParameters().size() == meth.args.size()
                    && IntStream.range(0, meth.args.size())
                    .allMatch(i -> isArgSuitable(
                            meth.args.get(i),
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
                && isRefAssignableTo(ref, getFuncMethod(reqArg.type.tsym, generics), generics)
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
