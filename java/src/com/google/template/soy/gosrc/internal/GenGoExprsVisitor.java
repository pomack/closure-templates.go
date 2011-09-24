/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.gosrc.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.exprtree.Operator;
import com.google.template.soy.internal.base.CharEscapers;
import com.google.template.soy.gosrc.internal.TranslateToGoExprVisitor.TranslateToGoExprVisitorFactory;
import static com.google.template.soy.gosrc.restricted.GoCodeUtils.genCoerceBoolean;
import static com.google.template.soy.gosrc.restricted.GoCodeUtils.genMaybeProtect;
import com.google.template.soy.gosrc.restricted.GoExpr;
import com.google.template.soy.gosrc.restricted.GoExprUtils;
import com.google.template.soy.gosrc.restricted.SoyGoSrcPrintDirective;
import com.google.template.soy.soytree.AbstractSoyNodeVisitor;
import com.google.template.soy.soytree.CallNode;
import com.google.template.soy.soytree.CallParamContentNode;
import com.google.template.soy.soytree.IfCondNode;
import com.google.template.soy.soytree.IfElseNode;
import com.google.template.soy.soytree.IfNode;
import com.google.template.soy.soytree.PrintDirectiveNode;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.RawTextNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.TemplateNode;

import java.util.Deque;
import java.util.List;
import java.util.Map;


/**
 * Visitor for generating Go expressions for parse tree nodes.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * <p> Precondition: MsgNode should not exist in the tree.
 *
 * @author Kai Huang
 */
public class GenGoExprsVisitor extends AbstractSoyNodeVisitor<List<GoExpr>> {


  /**
   * Injectable factory for creating an instance of this class.
   */
  public static interface GenGoExprsVisitorFactory {

    /**
     * @param localVarTranslations The current stack of replacement Go expressions for the local
     *     variables (and foreach-loop special functions) current in scope.
     */
    public GenGoExprsVisitor create(Deque<Map<String, GoExpr>> localVarTranslations);
  }


  /** Map of all SoyGoSrcPrintDirectives (name to directive). */
  Map<String, SoyGoSrcPrintDirective> soyGoSrcDirectivesMap;

  /** Instance of GenCallCodeUtils to use. */
  private final GenCallCodeUtils genCallCodeUtils;

  /** The IsComputableAsGoExprsVisitor used by this instance (when needed). */
  private final IsComputableAsGoExprsVisitor isComputableAsGoExprsVisitor;

  /** Factory for creating an instance of GenGoExprsVisitor. */
  private final GenGoExprsVisitorFactory genGoExprsVisitorFactory;

  /** Factory for creating an instance of TranslateToGoExprVisitor. */
  private final TranslateToGoExprVisitorFactory translateToGoExprVisitorFactory;

  /** The current stack of replacement Go expressions for the local variables (and foreach-loop
   *  special functions) current in scope. */
  private final Deque<Map<String, GoExpr>> localVarTranslations;

  /** List to collect the results. */
  private List<GoExpr> goExprs;


  /**
   * @param soyGoSrcDirectivesMap Map of all SoyGoSrcPrintDirectives (name to directive).
   * @param genCallCodeUtils Instance of GenCallCodeUtils to use.
   * @param isComputableAsGoExprsVisitor The IsComputableAsGoExprsVisitor used by this instance
   *     (when needed).
   * @param genGoExprsVisitorFactory Factory for creating an instance of GenGoExprsVisitor.
   * @param translateToGoExprVisitorFactory Factory for creating an instance of
   *     TranslateToGoExprVisitor.
   * @param localVarTranslations The current stack of replacement Go expressions for the local
   *     variables (and foreach-loop special functions) current in scope.
   */
  @AssistedInject
  GenGoExprsVisitor(
      Map<String, SoyGoSrcPrintDirective> soyGoSrcDirectivesMap,
      GenCallCodeUtils genCallCodeUtils,
      IsComputableAsGoExprsVisitor isComputableAsGoExprsVisitor,
      GenGoExprsVisitorFactory genGoExprsVisitorFactory,
      TranslateToGoExprVisitorFactory translateToGoExprVisitorFactory,
      @Assisted Deque<Map<String, GoExpr>> localVarTranslations) {
    this.soyGoSrcDirectivesMap = soyGoSrcDirectivesMap;
    this.genCallCodeUtils = genCallCodeUtils;
    this.isComputableAsGoExprsVisitor = isComputableAsGoExprsVisitor;
    this.genGoExprsVisitorFactory = genGoExprsVisitorFactory;
    this.translateToGoExprVisitorFactory = translateToGoExprVisitorFactory;
    this.localVarTranslations = localVarTranslations;
  }


  @Override public List<GoExpr> exec(SoyNode node) {
    Preconditions.checkArgument(isComputableAsGoExprsVisitor.exec(node));
    return super.exec(node);
  }


  @Override protected void setup() {
    goExprs = Lists.newArrayList();
  }


  @Override protected List<GoExpr> getResult() {
    return goExprs;
  }


  // -----------------------------------------------------------------------------------------------
  // Implementations for concrete classes.


  @Override protected void visitInternal(TemplateNode node) {
    visitChildren(node);
  }


  /**
   * Example:
   * <pre>
   *   I'm feeling lucky!
   * </pre>
   * generates
   * <pre>
   *   'I\'m feeling lucky!'
   * </pre>
   */
  @Override protected void visitInternal(RawTextNode node) {

    goExprs.add(new GoExpr(
        '"' + CharEscapers.goStringEscaper().escape(node.getRawText()) + '"',
        String.class, Integer.MAX_VALUE));
  }


  /**
   * Example:
   * <pre>
   *   {$boo.foo}
   *   {$goo.moo + 5}
   * </pre>
   * might generate
   * <pre>
   *   opt_data.boo.foo
   *   gooData4.moo + 5
   * </pre>
   */
  @Override protected void visitInternal(PrintNode node) {

    TranslateToGoExprVisitor ttjev =
        translateToGoExprVisitorFactory.create(localVarTranslations);

    GoExpr goExpr = ttjev.exec(node.getExpr());

    // Process directives.
    for (PrintDirectiveNode directiveNode : node.getChildren()) {

      // Get directive.
      SoyGoSrcPrintDirective directive = soyGoSrcDirectivesMap.get(directiveNode.getName());
      if (directive == null) {
        throw new SoySyntaxException(
            "Failed to find SoyGoSrcPrintDirective with name '" + directiveNode.getName() + "'" +
            " (tag " + node.toSourceString() +")");
      }

      // Get directive args.
      List<ExprRootNode<ExprNode>> args = directiveNode.getArgs();
      if (! directive.getValidArgsSizes().contains(args.size())) {
        throw new SoySyntaxException(
            "Print directive '" + directiveNode.getName() + "' used with the wrong number of" +
            " arguments (tag " + node.toSourceString() + ").");
      }

      // Translate directive args.
      List<GoExpr> argsGoExprs = Lists.newArrayListWithCapacity(args.size());
      for (ExprRootNode<ExprNode> arg : args) {
        argsGoExprs.add(ttjev.exec(arg));
      }

      // Apply directive.
      goExpr = directive.applyForGoSrc(goExpr, argsGoExprs);
    }

    goExprs.add(goExpr);
  }


  /**
   * Example:
   * <pre>
   *   {if $boo}
   *     AAA
   *   {elseif $foo}
   *     BBB
   *   {else}
   *     CCC
   *   {/if}
   * </pre>
   * might generate
   * <pre>
   *   (opt_data.boo) ? AAA : (opt_data.foo) ? BBB : CCC
   * </pre>
   */
  @Override protected void visitInternal(IfNode node) {

    // Create another instance of this visitor class for generating Go expressions from children.
    GenGoExprsVisitor genGoExprsVisitor =
        genGoExprsVisitorFactory.create(localVarTranslations);

    StringBuilder goExprTextSb = new StringBuilder();

    boolean hasElse = false;
    for (SoyNode child : node.getChildren()) {

      if (child instanceof IfCondNode) {
        IfCondNode icn = (IfCondNode) child;

        GoExpr condGoExpr =
            translateToGoExprVisitorFactory.create(localVarTranslations).exec(icn.getExpr());
        goExprTextSb.append("(").append(genCoerceBoolean(condGoExpr)).append(") ? ");

        List<GoExpr> condBlockGoExprs = genGoExprsVisitor.exec(icn);
        goExprTextSb.append(
            genMaybeProtect(GoExprUtils.concatGoExprs(condBlockGoExprs),
                            Operator.CONDITIONAL.getPrecedence() + 1));

        goExprTextSb.append(" : ");

      } else if (child instanceof IfElseNode) {
        hasElse = true;
        IfElseNode ien = (IfElseNode) child;

        List<GoExpr> elseBlockGoExprs = genGoExprsVisitor.exec(ien);
        goExprTextSb.append(
            genMaybeProtect(GoExprUtils.concatGoExprs(elseBlockGoExprs),
                            Operator.CONDITIONAL.getPrecedence() + 1));

      } else {
        throw new AssertionError();
      }
    }

    if (!hasElse) {
      goExprTextSb.append("\"\"");
    }

    goExprs.add(new GoExpr(
        goExprTextSb.toString(), String.class, Operator.CONDITIONAL.getPrecedence()));
  }


  @Override protected void visitInternal(IfCondNode node) {
    visitChildren(node);
  }


  @Override protected void visitInternal(IfElseNode node) {
    visitChildren(node);
  }


  /**
   * Example:
   * <pre>
   *   {call name="some.func" data="all" /}
   *   {call name="some.func" data="$boo.foo" /}
   *   {call name="some.func"}
   *     {param key="goo" value="$moo" /}
   *   {/call}
   *   {call name="some.func" data="$boo"}
   *     {param key="goo"}Blah{/param}
   *   {/call}
   * </pre>
   * might generate
   * <pre>
   *   some.func(opt_data)
   *   some.func(opt_data.boo.foo)
   *   some.func({goo: opt_data.moo})
   *   some.func(soy.$$augmentData(opt_data.boo, {goo: 'Blah'}))
   * </pre>
   */
  @Override protected void visitInternal(CallNode node) {
    goExprs.add(genCallCodeUtils.genCallExpr(node, localVarTranslations));
  }


  @Override protected void visitInternal(CallParamContentNode node) {
    visitChildren(node);
  }

}
