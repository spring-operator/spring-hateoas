/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.hateoas.server.core;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.client.LinkDiscoverer;

/**
 * Base class for unit tests for {@link LinkDiscoverer} implementations.
 *
 * @author Oliver Gierke
 */
public abstract class AbstractLinkDiscovererUnitTest {

	@Test
	public void findsSingleLink() {

		assertThat(getDiscoverer().findLinkWithRel("self", getInputString())) //
				.hasValue(new Link("selfHref"));

		Links links = getDiscoverer().findLinksWithRel("self", getInputString());

		assertThat(links).hasSize(1);
		assertThat(links).contains(new Link("selfHref"));
	}

	@Test
	public void findsFirstLink() {

		assertThat(getDiscoverer().findLinkWithRel("relation", getInputString()))
				.hasValue(new Link("firstHref", "relation"));
	}

	@Test
	public void findsAllLinks() {

		Links links = getDiscoverer().findLinksWithRel("relation", getInputString());

		assertThat(links).hasSize(2);
		assertThat(links).contains(new Link("firstHref", "relation"), new Link("secondHref", "relation"));
	}

	@Test
	public void returnsForInexistingLink() {
		assertThat(getDiscoverer().findLinkWithRel("something", getInputString())).isEmpty();
	}

	@Test
	public void returnsForInexistingLinkFromInputStream() throws Exception {

		InputStream inputStream = new ByteArrayInputStream(getInputString().getBytes("UTF-8"));
		assertThat(getDiscoverer().findLinkWithRel("something", inputStream)).isEmpty();
	}

	@Test
	public void returnsNullForNonExistingLinkContainer() {

		assertThat(getDiscoverer().findLinksWithRel("something", getInputStringWithoutLinkContainer())).isEmpty();
		assertThat(getDiscoverer().findLinkWithRel("something", getInputStringWithoutLinkContainer())).isEmpty();
	}

	/**
	 * Return the {@link LinkDiscoverer} to be tested.
	 *
	 * @return
	 */
	protected abstract LinkDiscoverer getDiscoverer();

	/**
	 * Return the JSON structure we expect to find the links in.
	 *
	 * @return
	 */
	protected abstract String getInputString();

	protected abstract String getInputStringWithoutLinkContainer();
}
