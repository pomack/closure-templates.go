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
import com.google.template.soy.gosrc.restricted.GoCodeUtils;
import com.google.template.soy.gosrc.restricted.GoExpr;
import com.google.template.soy.gosrc.restricted.SoyGoSrcFunction;
import static com.google.template.soy.gosrc.restricted.SoyGoSrcFunctionUtils.toStringGoExpr;
import com.google.template.soy.javasrc.restricted.JavaCodeUtils;
import com.google.template.soy.javasrc.restricted.JavaExpr;
import com.google.template.soy.javasrc.restricted.SoyJavaSrcFunction;
import static com.google.template.soy.javasrc.restricted.SoyJavaSrcFunctionUtils.toStringJavaExpr;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.shared.restricted.ApiCallScopeBindingAnnotations.BidiGlobalDir;
import com.google.template.soy.tofu.restricted.SoyTofuFunction;
import static com.google.template.soy.tofu.restricted.SoyTofuFunctionUtils.toSoyData;

import java.util.List;
import java.util.Set;


/**
 * Soy function that gets the name of the end edge ('left' or 'right') for the current global bidi
 * directionality.
 *
 * @author Aharon Lanin
 * @author Kai Huang
 */
@Singleton
class BidiEndEdgeFunction implements SoyTofuFunction, SoyJsSrcFunction, SoyJavaSrcFunction, SoyGoSrcFunction {


  /** Provider for the current bidi global directionality. */
  private final Provider<Integer> bidiGlobalDirProvider;


  /**
   * @param bidiGlobalDirProvider Provider for the current bidi global directionality.
   */
  @Inject
  BidiEndEdgeFunction(@BidiGlobalDir Provider<Integer> bidiGlobalDirProvider) {
    this.bidiGlobalDirProvider = bidiGlobalDirProvider;
  }


  @Override public String getName() {
    return "bidiEndEdge";
  }


  @Override public Set<Integer> getValidArgsSizes() {
    return ImmutableSet.of(0);
  }


  @Override public SoyData computeForTofu(List<SoyData> args) {

    return toSoyData((bidiGlobalDirProvider.get() < 0) ? "left" : "right");
  }


  @Override public JsExpr computeForJsSrc(List<JsExpr> args) {

    return new JsExpr((bidiGlobalDirProvider.get() < 0) ? "'left'" : "'right'", Integer.MAX_VALUE);
  }


  @Override public JavaExpr computeForJavaSrc(List<JavaExpr> args) {

    return toStringJavaExpr(JavaCodeUtils.genNewStringData(
        (bidiGlobalDirProvider.get() < 0) ? "\"left\"" : "\"right\""));
  }


  @Override public GoExpr computeForGoSrc(List<GoExpr> args) {

    return toStringGoExpr(GoCodeUtils.genNewStringData(
        (bidiGlobalDirProvider.get() < 0) ? "\"left\"" : "\"right\""));
  }

}
