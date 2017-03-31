/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.sling.commons.caservice.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.collect.ImmutableList;

import io.wcm.sling.commons.caservice.ContextAwareService;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.testing.mock.aem.junit.AemContext;

public class ContextAwareServiceResolverImplTest {

  @Rule
  public AemContext context = new AemContext();

  private DummySpi contentImpl;
  private DummySpi contentDamImpl;
  private DummySpi contentSampleImpl;

  private ContextAwareServiceResolver underTest;

  @Before
  public void setUp() {
    contentImpl = context.registerService(DummySpi.class, new DummySpiImpl(),
        ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN, "^/content(/.*)?$",
        Constants.SERVICE_RANKING, 100);
    contentDamImpl = context.registerService(DummySpi.class, new DummySpiImpl(),
        ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN, "^/content/dam(/.*)?$",
        Constants.SERVICE_RANKING, 200);
    contentSampleImpl = context.registerService(DummySpi.class, new DummySpiImpl(),
        ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN, "^/content/sample(/.*)?$",
        ContextAwareService.PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN, "^/content/sample/exclude(/.*)?$",
        Constants.SERVICE_RANKING, 300);

    // add some more services with high ranking but invalid properties - they should never be returned
    context.registerService(DummySpi.class, new DummySpiImpl(),
        ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN, "(",
        Constants.SERVICE_RANKING, 10000);
    context.registerService(DummySpi.class, new DummySpiImpl(),
        ContextAwareService.PROPERTY_CONTEXT_PATH_PATTERN, "^/content(/.*)?$",
        ContextAwareService.PROPERTY_CONTEXT_PATH_BLACKLIST_PATTERN, ")",
        Constants.SERVICE_RANKING, 20000);

    underTest = context.registerInjectActivateService(new ContextAwareServiceResolverImpl());
  }

  @Test
  public void testWithDefaultImpl() {
    DummySpi defaultImpl = context.registerService(DummySpi.class, new DummySpiImpl(),
        Constants.SERVICE_RANKING, Integer.MIN_VALUE,
        ContextAwareService.PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);

    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/dam/test1")));
    assertSame(defaultImpl, underTest.resolve(DummySpi.class, context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl, defaultImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2")).collect(Collectors.toList()));
  }

  @Test
  public void testWithoutDefaultImpl() {
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/test1")));
    assertSame(contentSampleImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/test1")));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/sample/exclude/test1")));
    assertSame(contentDamImpl, underTest.resolve(DummySpi.class, context.create().resource("/content/dam/test1")));
    assertNull(underTest.resolve(DummySpi.class, context.create().resource("/etc/test1")));

    assertEquals(ImmutableList.of(contentDamImpl, contentImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/dam/test2")).collect(Collectors.toList()));
  }

  @Test
  public void testWithSlingHttpServletRequest() {
    DummySpi defaultImpl = context.registerService(DummySpi.class, new DummySpiImpl(),
        Constants.SERVICE_RANKING, Integer.MIN_VALUE,
        ContextAwareService.PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);

    context.currentResource(context.create().resource("/content/test1"));
    assertSame(contentImpl, underTest.resolve(DummySpi.class, context.request()));

    assertEquals(ImmutableList.of(contentImpl, defaultImpl),
        underTest.resolveAll(DummySpi.class, context.create().resource("/content/test2")).collect(Collectors.toList()));
  }

  @Test
  public void testWithNull() {
    DummySpi defaultImpl = context.registerService(DummySpi.class, new DummySpiImpl(),
        Constants.SERVICE_RANKING, Integer.MIN_VALUE,
        ContextAwareService.PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY, true);

    assertSame(defaultImpl, underTest.resolve(DummySpi.class, null));

    assertEquals(ImmutableList.of(defaultImpl), underTest.resolveAll(DummySpi.class, null).collect(Collectors.toList()));
  }

}
