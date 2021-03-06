/*
 * Copyright 2012 the original author or authors.
 *
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
 */
package org.springframework.web.servlet.view.mustache;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;

/**
 * Verify both positive and negative cases around
 * loading templates.  The most important is the 
 * ability to handle the loading of partials.
 * 
 * @author Eric D. White <eric@ericwhite.ca>
 */
@RunWith(JMock.class)
public class MustacheTemplateLoaderTestCase {

	private Mockery context = new Mockery();

	private static final String TEST_TEMPLATES_PATH = "WEB-INF/views/";
	private static final String TEST_TEMPLATE = "test-template.html";
	private static final String PARENT_TEMPLATE = "test-parent.html";
	private static final String PARTIAL_TEMPLATE = "test-partial.html";

	private static final ClassPathResource test = new ClassPathResource(
			TEST_TEMPLATES_PATH.concat(TEST_TEMPLATE));
	private static final ClassPathResource parent = new ClassPathResource(
			TEST_TEMPLATES_PATH.concat(PARENT_TEMPLATE));
	private static final ClassPathResource partial = new ClassPathResource(
			TEST_TEMPLATES_PATH.concat(PARTIAL_TEMPLATE));

	private ResourceLoader resourceLoader;
	private MustacheTemplateLoader templateLoader;

	/**
	 * Stub out the resource loader with one that
	 * can load the ClassPathResource's defined.  This
	 * allows the actual MustacheFactory instance to
	 * load the template from the classpath including
	 * resolving partials.
	 */
	@Before
	public void setUp() {
		resourceLoader = context.mock(ResourceLoader.class);

		templateLoader = new MustacheTemplateLoader();
		templateLoader.setPrefix(TEST_TEMPLATES_PATH);
		templateLoader.setResourceLoader(resourceLoader);
	}

	@Test
	public void loadsATemplateContainingNoPartials() throws Exception {

		context.checking(new Expectations() {
			{
				oneOf(resourceLoader).getResource(pathFor(TEST_TEMPLATE));
				will(returnValue(test));
			}
		});

		Mustache template = templateLoader.compile(TEST_TEMPLATE);
		assertThat(template, notNullValue());
	}

	@Test
	public void loadsATemplateContainingAPartial() {

		context.checking(new Expectations() {
			{
				oneOf(resourceLoader).getResource(pathFor(PARENT_TEMPLATE));
				will(returnValue(parent));

				oneOf(resourceLoader).getResource(pathFor(PARTIAL_TEMPLATE));
				will(returnValue(partial));
			}
		});

		Mustache template = templateLoader.compile(PARENT_TEMPLATE);
		assertThat(template, notNullValue());
	}

	@Test(expected = MustacheException.class)
	public void throwsExceptionForTemplatesThatAreNotFound() {
		
		final String noSuchTemplate = "test-no-such-template.html";
		final Resource missing = context.mock(Resource.class);

		context.checking(new Expectations() {
			{
				oneOf(resourceLoader).getResource(pathFor(noSuchTemplate));
				will(returnValue(missing));

				oneOf(missing).exists();
				will(returnValue(Boolean.FALSE));
			}
		});

		templateLoader.compile(noSuchTemplate);
	}

	@Test(expected = MustacheException.class)
	public void throwsExceptionForTemplatesThatFailToLoad() throws IOException {
		
		final String corruptTemplate = "test-corrupt.html";
		final Resource corruptResource = context.mock(Resource.class);

		context.checking(new Expectations() {
			{
				oneOf(resourceLoader).getResource(pathFor(corruptTemplate));
				will(returnValue(corruptResource));

				oneOf(corruptResource).exists();
				will(returnValue(Boolean.TRUE));

				oneOf(corruptResource).getInputStream();
				will(throwException(new IOException("Corrupted stream")));
			}
		});

		templateLoader.compile(corruptTemplate);
	}

	private String pathFor(String templateName) {
		return TEST_TEMPLATES_PATH.concat(templateName);
	}
}
