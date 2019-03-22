/*
 * Copyright 2012-2017 the original author or authors.
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.Arrays;

import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.util.Assert;

/**
 * Base class to implement {@link RepresentationModelAssembler}s. Will automate {@link RepresentationModel} instance
 * creation and make sure a self-link is always added.
 *
 * @author Oliver Gierke
 */
public abstract class IdentifiableRepresentationModelAssemblerSupport<T extends Identifiable<?>, D extends RepresentationModel<D>>
		extends RepresentationModelAssemblerSupport<T, D> {

	private final Class<?> controllerClass;

	/**
	 * Creates a new {@link RepresentationModelAssemblerSupport} using the given controller class and resource type.
	 *
	 * @param controllerClass must not be {@literal null}.
	 * @param resourceType must not be {@literal null}.
	 */
	public IdentifiableRepresentationModelAssemblerSupport(Class<?> controllerClass, Class<D> resourceType) {

		super(controllerClass, resourceType);
		this.controllerClass = controllerClass;
	}

	/**
	 * Creates a new resource and adds a self link to it consisting using the {@link Identifiable}'s id.
	 *
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	protected D createModel(T entity) {
		return createModel(entity, new Object[0]);
	}

	protected D createModel(T entity, Object... parameters) {
		return createModelWithId(entity.getId(), entity, parameters);
	}

	@Override
	protected D createModelWithId(Object id, T entity, Object... parameters) {

		Assert.notNull(entity, "Entity must not be null!");
		Assert.notNull(id, "Id must not be null!");

		D instance = instantiateModel(entity);
		instance.add(linkTo(controllerClass, unwrapIdentifyables(parameters)).slash(id).withSelfRel());
		return instance;
	}

	/**
	 * Extracts the ids of the given values in case they're {@link Identifiable}s. Returns all other objects as they are.
	 *
	 * @param values must not be {@literal null}.
	 * @return
	 */
	private Object[] unwrapIdentifyables(Object[] values) {

		return Arrays.stream(values) //
				.map(element -> element instanceof Identifiable ? ((Identifiable<?>) element).getId().get() : element) //
				.toArray();
	}
}
