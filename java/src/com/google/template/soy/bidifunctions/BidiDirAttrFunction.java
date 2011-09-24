/*
 * Copyright 2009 Google Inc.
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

package com.google.template.soy.bidifunctions;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.internal.i18n.SoyBidiUtils;
import com.google.template.soy.gosrc.restricted.GoCodeUtils;
import com.google.template.soy.gosrc.restricted.GoExpr;
import com.google.template.soy.gosrc.restricted.SoyGoSrcFunction;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.shared.restricted.ApiCallScopeBindingAnnotations.BidiGlobalDir;
import com.google.template.soy.tofu.restricted.SoyTofuFunction;
import static com.google.template.soy.tofu.restricted.SoyTofuFunctionUtils.toSoyData;

import java.util.List;
import java.util.Set;


/**
 * Soy function that maybe inserts an HTML attribute for bidi directionality ('dir=ltr' or
 * 'dir=rtl'). The function requires the text string that will make up the body of the associated
 * HTML tag pair. If the text string is detected to require different directionality than the
 * curret global directionality, then the appropriate HTML attribute is inserted. Otherwise, nothing
 * is inserted.
 *
 * @author Aharon Lanin
 * @author Kai Huang
 */
@Singleton
class BidiDirAttrFunction implements SoyTofuFunction, SoyJsSrcFunction, SoyGoSrcFunction {


  /** Provider for the current bidi global directionality. */
  private final Provider<Integer> bidiGlobalDirProvider;


  /**
   * @param bidiGlobalDirProvider Provider for the current bidi global directionality.
   */
  @Inject
  BidiDirAttrFunction(@BidiGlobalDir Provider<Integer> bidiGlobalDirProvider) {
    this.bidiGlobalDirProvider = bidiGlobalDirProvider;
  }


  @Override public String getName() {
    return "bidiDirAttr";
  }


  @Override public Set<Integer> getValidArgsSizes() {
    return ImmutableSet.of(1, 2);
  }


  @Override public SoyData computeForTofu(List<SoyData> args) {
    String text = args.get(0).stringValue();
    @SuppressWarnings("SimplifiableConditionalExpression")  // make IntelliJ happy
    boolean isHtml = (args.size() == 2) ? args.get(1).booleanValue() : false /* default */;

    int bidiGlobalDir = bidiGlobalDirProvider.get();
    return toSoyData(SoyBidiUtils.getBidiFormatter(bidiGlobalDir).dirAttr(text, isHtml));
  }


  @Override public JsExpr computeForJsSrc(List<JsExpr> args) {
    JsExpr text = args.get(0);
    JsExpr isHtml = (args.size() == 2) ? args.get(1) : null;

    String callText =
        "soy.$$bidiDirAttr(" + bidiGlobalDirProvider.get() + ", " + text.getText() +
        (isHtml != null ? ", " + isHtml.getText() : "") + ")";

    return new JsExpr(callText, Integer.MAX_VALUE);
  }


  @Override public GoExpr computeForGoSrc(List<GoExpr> args) {
    GoExpr text = args.get(0);
    GoExpr isHtml = (args.size() == 2) ? args.get(1) : null;
    String opt_isHtml = (isHtml == null) ? "false" : isHtml.getText();

    String callText = "soyutil.BidiDirAttr(" + bidiGlobalDirProvider.get() + ", " + GoCodeUtils.genCoerceString(text) + ", " + opt_isHtml + ")";

    return new GoExpr(callText, String.class, Integer.MAX_VALUE);
  }

}
