/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.hateoas;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Utility class to ease tesing.
 * 
 * @author Oliver Gierke
 */
public class TestUtils {

	protected MockHttpServletRequest request;

	@Before
	public void setUp() {

		request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
	}

	protected void assertPointsToMockServer(Link link) {
		assertThat(link.getHref(), startsWith("http://localhost"));
	}

	/**
	 * Provide a mechanism to simulate inserting a {@link ForwardedHeaderFilter} into the servlet filter chain, so
	 * {@literal Forwarded} headers are properly inserted into the test web request.
	 *
	 * @see https://jira.spring.io/browse/SPR-16668
	 */
	protected void adaptRequestFromForwardedHeaders() {

		MockFilterChain chain = new MockFilterChain();

		try {
			new ForwardedHeaderFilter().doFilter(this.request, new MockHttpServletResponse(), chain);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		HttpServletRequest adaptedRequest = (HttpServletRequest) chain.getRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(adaptedRequest));
	}

	public static void assertEqualAndSameHashCode(Object left, Object right) {

		assertThat(left, is(right));
		assertThat(right, is(left));
		assertThat(left, is(left));
		assertThat(left.hashCode(), is(right.hashCode()));
		assertThat(left.toString(), is(right.toString()));
	}

	public static void assertNotEqualAndDifferentHashCode(Object left, Object right) {

		assertThat(left, is(not(right)));
		assertThat(right, is(not(left)));
		assertThat(left.hashCode(), is(not(right.hashCode())));
		assertThat(left.toString(), is(not(right.toString())));
	}
}
