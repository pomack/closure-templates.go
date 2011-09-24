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

package com.google.template.soy.gosrc.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.template.soy.gosrc.SoyGoSrcOptions;
import com.google.template.soy.gosrc.internal.GenGoExprsVisitor.GenGoExprsVisitorFactory;
import com.google.template.soy.gosrc.internal.TranslateToGoExprVisitor.TranslateToGoExprVisitorFactory;
import com.google.template.soy.gosrc.restricted.SoyGoSrcFunction;
import com.google.template.soy.gosrc.restricted.SoyGoSrcPrintDirective;
import com.google.template.soy.shared.internal.ApiCallScope;
import com.google.template.soy.shared.internal.BackendModuleUtils;
import com.google.template.soy.shared.internal.GuiceSimpleScope;
import com.google.template.soy.shared.internal.SharedModule;
import com.google.template.soy.shared.restricted.SoyFunction;
import com.google.template.soy.shared.restricted.SoyPrintDirective;

import java.util.Map;
import java.util.Set;


/**
 * Guice module for the Go Source backend.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * @author Kai Huang
 */
public class GoSrcModule extends AbstractModule {


  @Override protected void configure() {

    // Install requisite modules.
    install(new SharedModule());

    // Bind providers of factories (created via assisted inject).
    bind(GenGoExprsVisitorFactory.class)
        .toProvider(FactoryProvider.newFactory(
            GenGoExprsVisitorFactory.class, GenGoExprsVisitor.class));
    bind(TranslateToGoExprVisitorFactory.class)
        .toProvider(FactoryProvider.newFactory(
            TranslateToGoExprVisitorFactory.class, TranslateToGoExprVisitor.class));

    // Bind unscoped providers for parameters in ApiCallScope (these throw exceptions).
    bind(SoyGoSrcOptions.class)
        .toProvider(GuiceSimpleScope.<SoyGoSrcOptions>getUnscopedProvider())
        .in(ApiCallScope.class);
  }


  /**
   * Builds and provides the map of SoyGoSrcFunctions (name to function).
   * @param soyFunctionsSet The installed set of SoyFunctions (from Guice Multibinder). Each
   *     SoyFunction may or may not implement SoyGoSrcFunction.
   */
  @Provides
  @Singleton
  Map<String, SoyGoSrcFunction> provideSoyGoSrcFunctionsMap(Set<SoyFunction> soyFunctionsSet) {

    return BackendModuleUtils.buildBackendSpecificSoyFunctionsMap(
        SoyGoSrcFunction.class, soyFunctionsSet);
  }


  /**
   * Builds and provides the map of SoyGoSrcDirectives (name to directive).
   * @param soyDirectivesSet The installed set of SoyDirectives (from Guice Multibinder). Each
   *     SoyDirective may or may not implement SoyGoSrcDirective.
   */
  @Provides
  @Singleton
  Map<String, SoyGoSrcPrintDirective> provideSoyGoSrcDirectivesMap(
      Set<SoyPrintDirective> soyDirectivesSet) {

    return BackendModuleUtils.buildBackendSpecificSoyDirectivesMap(
        SoyGoSrcPrintDirective.class, soyDirectivesSet);
  }

}
