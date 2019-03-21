/*
 * Copyright 2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link StringLinkRelation}.
 *
 * @author Oliver Gierke
 */
public class StringLinkRelationUnitTests {

	@Test
	public void serializesAsPlainString() throws Exception {

		Sample sample = new Sample();
		sample.relation = StringLinkRelation.of("foo");

		ObjectMapper mapper = new ObjectMapper();

		assertThat(mapper.writeValueAsString(sample)).isEqualTo("{\"relation\":\"foo\"}");
	}

	@Test
	public void deserializesUsingFactoryMethod() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		Sample result = mapper.readValue("{\"relation\":\"foo\"}", Sample.class);

		assertThat(result.relation).isEqualTo(StringLinkRelation.of("foo"));
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	static class Sample {
		StringLinkRelation relation;
	}
}
