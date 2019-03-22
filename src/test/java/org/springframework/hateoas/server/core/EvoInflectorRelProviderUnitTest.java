/*
 * Copyright 2013-2014 the original author or authors.
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

import org.junit.Test;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;

/**
 * Unit tests for {@link EvoInflectorLinkRelationProvider}.
 *
 * @author Oliver Gierke
 */
public class EvoInflectorRelProviderUnitTest {

	LinkRelationProvider provider = new EvoInflectorLinkRelationProvider();

	@Test
	public void buildsCollectionRelCorrectly() {
		assertRels(City.class, "city", "cities");
		assertRels(Person.class, "person", "persons");
	}

	private void assertRels(Class<?> type, String singleRel, String collectionRel) {

		assertThat(provider.getItemResourceRelFor(type)).isEqualTo(LinkRelation.of(singleRel));
		assertThat(provider.getCollectionResourceRelFor(type)).isEqualTo(LinkRelation.of(collectionRel));
	}

	static class Person {

	}

	static class City {

	}
}
