/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.hal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.core.AbstractLinkDiscovererUnitTest;

/**
 * Unit tests for {@link HalLinkDiscoverer}.
 * 
 * @author Oliver Gierke
 */
public class HalLinkDiscovererUnitTest extends AbstractLinkDiscovererUnitTest {

	static final LinkDiscoverer discoverer = new HalLinkDiscoverer();
	static final String SAMPLE = "{ _links : { self : { href : 'selfHref' }, " + //
			"relation : [ { href : 'firstHref' }, { href : 'secondHref' }], " + //
			"'http://www.foo.com/bar' : { href : 'fullRelHref' }, " + "}}";

	/**
	 * @see #314
	 */
	@Test
	public void discoversFullyQualifiedRel() {

		Link link = getDiscoverer().findLinkWithRel("http://www.foo.com/bar", SAMPLE);

		assertThat(link, is(notNullValue()));
		assertThat(link.getHref(), is("fullRelHref"));
	}

	/**
	 * @see #470
	 */
	@Test
	public void supportsHalUtf8() {
		assertThat(getDiscoverer().supports(MediaTypes.HAL_JSON_UTF8), is(true));
	}

	@Override
	protected LinkDiscoverer getDiscoverer() {
		return discoverer;
	}

	@Override
	protected String getInputString() {
		return SAMPLE;
	}

	@Override
	protected String getInputStringWithoutLinkContainer() {
		return "{}";
	}
}
