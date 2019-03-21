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
package org.springframework.hateoas.hal.forms;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.HalLinkRelation;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Collection of components needed to serialize a HAL-FORMS document.
 *
 * @author Greg Turnquist
 */
class HalFormsSerializers {

	/**
	 * Serializer for {@link Resources}.
	 */
	static class HalFormsResourceSerializer extends ContainerSerializer<Resource<?>> implements ContextualSerializer {

		private static final long serialVersionUID = -7912243216469101379L;

		private final BeanProperty property;

		HalFormsResourceSerializer(@Nullable BeanProperty property) {

			super(Resource.class, false);
			this.property = property;
		}

		HalFormsResourceSerializer() {
			this(null);
		}

		@Override
		public void serialize(Resource<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			HalFormsDocument<?> doc = HalFormsDocument.forResource(value.getContent()) //
					.withLinks(value.getLinks()) //
					.withTemplates(findTemplates(value));

			provider.findValueSerializer(HalFormsDocument.class, property).serialize(doc, gen, provider);
		}

		@Override
		@Nullable
		public JavaType getContentType() {
			return null;
		}

		@Override
		@Nullable
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean hasSingleElement(Resource<?> resource) {
			return false;
		}

		@Override
		@Nullable
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer typeSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
				throws JsonMappingException {
			return new HalFormsResourceSerializer(property);
		}
	}

	/**
	 * Serializer for {@link Resources}
	 */
	static class HalFormsResourcesSerializer extends ContainerSerializer<Resources<?>> implements ContextualSerializer {

		private static final long serialVersionUID = -3601146866067500734L;

		private final BeanProperty property;
		private final Jackson2HalModule.EmbeddedMapper embeddedMapper;

		HalFormsResourcesSerializer(@Nullable BeanProperty property, Jackson2HalModule.EmbeddedMapper embeddedMapper) {

			super(Resources.class, false);

			this.property = property;
			this.embeddedMapper = embeddedMapper;
		}

		HalFormsResourcesSerializer(Jackson2HalModule.EmbeddedMapper embeddedMapper) {
			this(null, embeddedMapper);
		}

		@Override
		public void serialize(Resources<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {

			Map<HalLinkRelation, Object> embeddeds = embeddedMapper.map(value);

			HalFormsDocument<?> doc;

			if (value instanceof PagedResources) {

				doc = HalFormsDocument.empty() //
						.withEmbedded(embeddeds) //
						.withPageMetadata(((PagedResources<?>) value).getMetadata()) //
						.withLinks(value.getLinks()) //
						.withTemplates(findTemplates(value));

			} else {

				doc = HalFormsDocument.empty() //
						.withEmbedded(embeddeds) //
						.withLinks(value.getLinks()) //
						.withTemplates(findTemplates(value));
			}

			provider.findValueSerializer(HalFormsDocument.class, property).serialize(doc, gen, provider);
		}

		@Override
		@Nullable
		public JavaType getContentType() {
			return null;
		}

		@Override
		@Nullable
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean hasSingleElement(Resources<?> resources) {
			return resources.getContent().size() == 1;
		}

		@Override
		@Nullable
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer typeSerializer) {
			return null;
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
				throws JsonMappingException {
			return new HalFormsResourcesSerializer(property, embeddedMapper);
		}
	}

	/**
	 * Extract template details from a {@link ResourceSupport}'s {@link Affordance}s.
	 *
	 * @param resource
	 * @return
	 */
	private static Map<String, HalFormsTemplate> findTemplates(ResourceSupport resource) {

		if (!resource.hasLink(IanaLinkRelations.SELF)) {
			return Collections.emptyMap();
		}

		Map<String, HalFormsTemplate> templates = new HashMap<>();
		List<Affordance> affordances = resource.getLink(IanaLinkRelations.SELF).map(Link::getAffordances)
				.orElse(Collections.emptyList());

		affordances.stream() //
				.map(it -> it.getAffordanceModel(MediaTypes.HAL_FORMS_JSON)) //
				.map(HalFormsAffordanceModel.class::cast) //
				.filter(it -> !it.hasHttpMethod(HttpMethod.GET)) //
				.peek(it -> validate(resource, it)) //
				.forEach(it -> {

					HalFormsTemplate template = HalFormsTemplate.forMethod(it.getHttpMethod()) //
							.withProperties(it.getInputProperties());

					/*
					 * First template in HAL-FORMS is "default".
					 */
					templates.put(templates.isEmpty() ? "default" : it.getName(), template);
				});

		return templates;
	}

	/**
	 * Verify that the resource's self link and the affordance's URI have the same relative path.
	 *
	 * @param resource
	 * @param model
	 */
	private static void validate(ResourceSupport resource, HalFormsAffordanceModel model) {

		String affordanceUri = model.getURI();
		String selfLinkUri = resource.getRequiredLink(IanaLinkRelations.SELF.value()).expand().getHref();

		if (!affordanceUri.equals(selfLinkUri)) {
			throw new IllegalStateException("Affordance's URI " + affordanceUri + " doesn't match self link " + selfLinkUri
					+ " as expected in HAL-FORMS");
		}
	}
}
