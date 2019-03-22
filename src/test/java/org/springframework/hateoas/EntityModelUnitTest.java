/*
 * Copyright 2012 the original author or authors.
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

import java.util.Collections;

import org.junit.Test;

/**
 * Unit tests for {@link EntityModel}.
 * 
 * @author Oliver Gierke
 */
public class EntityModelUnitTest {

	@Test
	public void equalsForSelfReference() {

		EntityModel<String> resource = new EntityModel<>("foo");
		assertThat(resource).isEqualTo(resource);
	}

	@Test
	public void equalsWithEqualContent() {

		EntityModel<String> left = new EntityModel<>("foo");
		EntityModel<String> right = new EntityModel<>("foo");

		assertThat(left).isEqualTo(right);
		assertThat(right).isEqualTo(left);
	}

	@Test
	public void notEqualForDifferentContent() {

		EntityModel<String> left = new EntityModel<>("foo");
		EntityModel<String> right = new EntityModel<>("bar");

		assertThat(left).isNotEqualTo(right);
		assertThat(right).isNotEqualTo(left);
	}

	@Test
	public void notEqualForDifferentLinks() {

		EntityModel<String> left = new EntityModel<>("foo");
		EntityModel<String> right = new EntityModel<>("foo");
		right.add(new Link("localhost"));

		assertThat(left).isNotEqualTo(right);
		assertThat(right).isNotEqualTo(left);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsCollectionContent() {
		new EntityModel<Object>(Collections.emptyList());
	}
}
