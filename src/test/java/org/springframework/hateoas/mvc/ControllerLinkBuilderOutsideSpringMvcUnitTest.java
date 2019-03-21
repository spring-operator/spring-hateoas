/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.hateoas.mvc;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Test cases for {@link ControllerLinkBuilder} that are NOT inside an existing Spring MVC request
 *
 * @author Greg Turnquist
 */
public class ControllerLinkBuilderOutsideSpringMvcUnitTest {

	/**
	 * Clear out any existing request attributes left behind by other tests
	 */
	@Before
	public void setUp() {
		RequestContextHolder.setRequestAttributes(null);
	}

	/**
	 * @see #408
	 */
	@Test
	public void requestingLinkOutsideWebRequest() {

		Link link = linkTo(
				methodOn(ControllerLinkBuilderUnitTest.PersonsAddressesController.class, 15).getAddressesForCountry("DE"))
						.withSelfRel();

		assertThat(link, is(new Link("/people/15/addresses/DE").withSelfRel()));
	}
}
