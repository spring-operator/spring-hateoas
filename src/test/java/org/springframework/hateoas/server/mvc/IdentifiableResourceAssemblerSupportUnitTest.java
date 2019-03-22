/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.hateoas.server.mvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.TestUtils;
import org.springframework.hateoas.server.LinkBuilder;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Unit tests for {@link IdentifiableRepresentationModelAssemblerSupport}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 */
public class IdentifiableResourceAssemblerSupportUnitTest extends TestUtils {

	PersonResourceAssembler assembler = new PersonResourceAssembler();
	Person person;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		this.person = new Person();
		this.person.id = 10L;
		this.person.alternateId = "id";
	}

	@Test
	public void createsInstanceWithSelfLinkToController() {

		PersonResource resource = assembler.createModel(person);
		Link link = resource.getRequiredLink(IanaLinkRelations.SELF.value());

		assertThat(link).isNotNull();
		assertThat(resource.getLinks()).hasSize(1);
	}

	@Test
	public void usesAlternateIdIfGivenExplicitly() {

		PersonResource resource = assembler.createModelWithId(person.alternateId, person);
		Optional<Link> selfLink = resource.getId();

		assertThat(selfLink.map(Link::getHref)) //
				.hasValueSatisfying(it -> assertThat(it).endsWith("/people/id"));
	}

	@Test
	public void unwrapsIdentifyablesForParameters() {

		PersonResource resource = new PersonResourceAssembler(ParameterizedController.class).createModel(person, person,
				"bar");
		Optional<Link> selfLink = resource.getId();

		assertThat(selfLink.map(Link::getHref)) //
				.hasValueSatisfying(it -> assertThat(it).endsWith("/people/10/bar/addresses/10"));
	}

	/**
	 * @see #416
	 */
	@Test
	public void convertsEntitiesToListOfResources() {

		Person first = new Person();
		first.id = 1L;
		Person second = new Person();
		second.id = 2L;

		List<PersonResource> result = assembler.map(Arrays.asList(first, second)).toListOfResources();

		LinkBuilder builder = linkTo(PersonController.class);

		PersonResource firstResource = new PersonResource();
		firstResource.add(builder.slash(1L).withSelfRel());

		PersonResource secondResource = new PersonResource();
		secondResource.add(builder.slash(1L).withSelfRel());

		assertThat(result).hasSize(2);
		assertThat(result).contains(firstResource, secondResource);
	}

	/**
	 * @see #416
	 */
	@Test
	public void convertsEntitiesToResources() {

		Person first = new Person();
		first.id = 1L;
		Person second = new Person();
		second.id = 2L;

		CollectionModel<PersonResource> result = assembler.map(Arrays.asList(first, second)).toResources();

		LinkBuilder builder = linkTo(PersonController.class);

		PersonResource firstResource = new PersonResource();
		firstResource.add(builder.slash(1L).withSelfRel());

		PersonResource secondResource = new PersonResource();
		secondResource.add(builder.slash(1L).withSelfRel());

		assertThat(result).hasSize(2);
		assertThat(result).contains(firstResource, secondResource);
	}

	@RequestMapping("/people")
	static class PersonController {

	}

	@RequestMapping("/people/{id}/{foo}/addresses")
	static class ParameterizedController {

	}

	static class Person implements Identifiable<Long> {

		Long id;
		String alternateId;

		@Override
		public Optional<Long> getId() {
			return Optional.ofNullable(id);
		}
	}

	static class PersonResource extends RepresentationModel<PersonResource> {

	}

	class PersonResourceAssembler extends IdentifiableRepresentationModelAssemblerSupport<Person, PersonResource> {

		PersonResourceAssembler() {
			this(PersonController.class);
		}

		PersonResourceAssembler(Class<?> controllerType) {
			super(controllerType, PersonResource.class);
		}

		@Override
		public PersonResource toModel(Person entity) {
			return createModel(entity);
		}
	}
}
