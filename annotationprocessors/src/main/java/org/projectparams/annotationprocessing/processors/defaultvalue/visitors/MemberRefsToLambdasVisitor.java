package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.visitors.AbstractVisitor;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MemberRefsToLambdasVisitor extends AbstractVisitor<Void, Void> {
    public MemberRefsToLambdasVisitor(Trees trees, Messager messager) {
        super(trees, messager);
    }

    private static Map<Name, Type> getGenericsConversionMap(
            List<Type> typeArgsInParams,
            Symbol.MethodSymbol lambdaMethSym,
            Type requiredLambdaMeth) {
        return IntStream.range(0, typeArgsInParams.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> typeArgsInParams.get(i) instanceof Type.WildcardType ?
                                ((Type.WildcardType) typeArgsInParams.get(i)).type.tsym.name :
                                typeArgsInParams.get(i).tsym.name,
                        i -> {
                            var typeArgsOfFuncInterface = TypeUtils.getTypeArgs(lambdaMethSym);
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
                        TypeUtils.getTypeArgs(lambdaMethSym).indexOf(param.type))
                        : param.type
        )).toList();
    }

    private static Symbol.TypeSymbol getDeclArgSym(JCTree.JCMemberReference memberReference, ParentInfo parentInfo) {
        return parentInfo.possibleMethods().getFirst().params().get(
                parentInfo.passedArgs().indexOf(memberReference))
                .type.tsym;
    }

    private JCTree.JCExpression getBodyExpr(JCTree.JCMemberReference memberReference, List<JCTree.JCVariableDecl> args) {
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

    private JCTree.JCExpression[] varDeclsToArgs(List<JCTree.JCVariableDecl> args) {
        return args.stream().map(this::toArg).toArray(JCTree.JCExpression[]::new);
    }

    private JCTree.JCExpression toArg(JCTree.JCVariableDecl varDecl) {
        var arg = ExpressionMaker.makeIdent(varDecl.name.toString());
        TypeUtils.attributeExpression(varDecl.vartype, getCurrentPath());
        arg.type = varDecl.vartype.type;
        varDecl.type = varDecl.vartype.type;
        return arg;
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
            var lambdaInfo = getLambdaInfo(parent, memberReference);
            var lambda = ExpressionMaker.makeLambda(
                    lambdaInfo.passedArgs,
                    getBodyExpr(memberReference, lambdaInfo.passedArgs)
            );
            lambda.type = lambdaInfo.lambdaType;
            return lambda;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private LambdaInfo getLambdaInfo(JCTree parent, JCTree.JCMemberReference memberReference) {
        if (parent instanceof JCTree.JCVariableDecl varDecl) {
            return getLambdaInfoFromVariableDecl(varDecl);
        } else {
            return getLambdaInfoFromInvocableParent(parent, memberReference);
        }
    }

    private LambdaInfo getLambdaInfoFromVariableDecl(JCTree.JCVariableDecl varDecl) {
        varDecl.init = null;
        var typeArgs = Objects.requireNonNullElseGet(varDecl.vartype.type,
                () -> {
                    TypeUtils.attributeExpression(varDecl, getCurrentPath());
                    return varDecl.vartype.type;
                }).getTypeArguments();
        var funcMethod = TypeUtils.getFuncMethod(varDecl.vartype.type.tsym, Collections.emptyMap());
        List<Type> expectedArgs = new ArrayList<>(Objects.requireNonNull(funcMethod).params().map(param -> param.type));
        expectedArgs.replaceAll(type -> type.tsym instanceof Symbol.TypeVariableSymbol
                ? typeArgs.get(TypeUtils.getTypeArgs(funcMethod).indexOf(type))
                : type);
        var atomicCounter = new AtomicInteger(0);
        return new LambdaInfo(expectedArgs.stream().map(type ->
                ExpressionMaker.makeVariableDecl(
                        "arg" + Math.abs(type.toString().hashCode()) + "UNIQUEENDING" + atomicCounter.getAndIncrement(),
                        type
                )).toList(),
                varDecl.vartype.type
        );
    }

    private LambdaInfo getLambdaInfoFromInvocableParent(JCTree parent, JCTree.JCMemberReference memberReference) {
        var parentInfo = getParentInfo(parent);
        if (parentInfo.possibleMethods().size() == 1) {
            return new LambdaInfo(
                    getPassedArgsInner(memberReference, parentInfo),
                    TypeUtils.replaceAllTypeVars(parentInfo.possibleMethods().getFirst().params().get(
                            parentInfo.passedArgs().indexOf(memberReference)
                    ).type, parentInfo.genericTypes().get())
            );
        } else if (parentInfo.possibleMethods().isEmpty()) {
            throw new IllegalStateException("No matching methods for: " + parent);
        } else {
            throw new IllegalStateException("Ambiguous methods: " + parentInfo.possibleMethods());
        }
    }

    private List<JCTree.JCVariableDecl> getPassedArgsInner(JCTree.JCMemberReference memberReference, ParentInfo parentInfo) {
        var funcInterface = TypeUtils.replaceAllTypeVars(
                parentInfo.possibleMethods().getFirst().getParameters()
                        .get(parentInfo.passedArgs().indexOf(memberReference)).type,
                parentInfo.genericTypes().get()
        );
        var lambdaMethSym = Objects.requireNonNull(
                TypeUtils.getFuncMethod(funcInterface.tsym, parentInfo.genericTypes().get())
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
                                    TypeUtils.getBoxedType(type));
                }).toList();
    }

    private com.sun.tools.javac.util.List<Type> getConvertedLambdaMethodArgumentTypes(List<Type> typeArgsInParams, Symbol.MethodSymbol lambdaMethSym, Type requiredLambdaMeth) {
        var conversionMap = getGenericsConversionMap(typeArgsInParams, lambdaMethSym, requiredLambdaMeth);
        return lambdaMethSym.params()
                .map(param -> TypeUtils.replaceAllTypeVars(param.type, conversionMap));
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
                ExpressionUtils::getName,
                typeArg -> TypeUtils.getTypeByName(ExpressionUtils.getName(typeArg).toString())
        )));
        return new ParentInfo(cl.args,
                TypeUtils.getMatchingMethods(ExpressionMaker.makeName("<init>"), cl.args, cl.clazz, genericTypes, getCurrentPath()),
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
                ExpressionUtils::getName,
                typeArg -> TypeUtils.getTypeByName(ExpressionUtils.getName(typeArg).toString())
        )));
        return new ParentInfo(meth.args,
                TypeUtils.getMatchingMethods(ExpressionUtils.getName(meth.meth), meth.args, parentOwner, genericTypes, getCurrentPath()),
                genericTypes);
    }

    private record LambdaInfo(
            List<JCTree.JCVariableDecl> passedArgs,
            Type lambdaType
    ) {
    }

    private record ParentInfo(com.sun.tools.javac.util.List<JCTree.JCExpression> passedArgs,
                              List<Symbol.MethodSymbol> possibleMethods,
                              AtomicReference<Map<Name, Type>> genericTypes) {
    }
}
