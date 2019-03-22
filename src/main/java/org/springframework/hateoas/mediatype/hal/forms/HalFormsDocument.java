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
package org.springframework.hateoas.mediatype.hal.forms;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule.HalLinkListSerializer;
import org.springframework.hateoas.mediatype.hal.forms.Jackson2HalFormsModule.HalFormsLinksDeserializer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Representation of a HAL-FORMS document.
 *
 * @author Dietrich Schulten
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@Value
@Wither
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@JsonPropertyOrder({ "resource", "resources", "embedded", "links", "templates", "metadata" })
public class HalFormsDocument<T> {

	@Nullable //
	@JsonUnwrapped //
	@JsonInclude(Include.NON_NULL) //
	private T resource;

	@Nullable //
	@JsonInclude(Include.NON_EMPTY) //
	@JsonIgnore //
	@Wither(AccessLevel.PRIVATE) //
	private Collection<T> resources;

	@JsonProperty("_embedded") //
	@JsonInclude(Include.NON_EMPTY) //
	private Map<HalLinkRelation, Object> embedded;

	@Nullable //
	@JsonProperty("page") //
	@JsonInclude(Include.NON_NULL) //
	private PagedModel.PageMetadata pageMetadata;

	// @Singular //
	@JsonProperty("_links") //
	@JsonInclude(Include.NON_EMPTY) //
	@JsonSerialize(using = HalLinkListSerializer.class) //
	@JsonDeserialize(using = HalFormsLinksDeserializer.class) //
	private Links links;

	// @Singular //
	@JsonProperty("_templates") //
	@JsonInclude(Include.NON_EMPTY) //
	private Map<String, HalFormsTemplate> templates;

	private HalFormsDocument() {
		this(null, null, Collections.emptyMap(), null, Links.NONE, Collections.emptyMap());
	}

	/**
	 * Creates a new {@link HalFormsDocument} for the given resource.
	 *
	 * @param resource can be {@literal null}.
	 * @return
	 */
	public static <T> HalFormsDocument<T> forResource(@Nullable T resource) {
		return new HalFormsDocument<T>().withResource(resource);
	}

	/**
	 * returns a new {@link HalFormsDocument} for the given resources.
	 *
	 * @param resources must not be {@literal null}.
	 * @return
	 */
	public static <T> HalFormsDocument<T> forResources(Collection<T> resources) {

		Assert.notNull(resources, "Resources must not be null!");

		return new HalFormsDocument<T>().withResources(resources);
	}

	/**
	 * Creates a new empty {@link HalFormsDocument}.
	 *
	 * @return
	 */
	public static HalFormsDocument<?> empty() {
		return new HalFormsDocument<>();
	}

	/**
	 * Returns the default template of the document.
	 *
	 * @return
	 */
	@JsonIgnore
	public HalFormsTemplate getDefaultTemplate() {
		return getTemplate(HalFormsTemplate.DEFAULT_KEY);
	}

	/**
	 * Returns the template with the given name.
	 *
	 * @param key must not be {@literal null}.
	 * @return
	 */
	@JsonIgnore
	public HalFormsTemplate getTemplate(String key) {

		Assert.notNull(key, "Template key must not be null!");

		return this.templates.get(key);
	}

	public HalFormsDocument<T> withPageMetadata(@Nullable PageMetadata metadata) {
		return new HalFormsDocument<T>(resource, resources, embedded, metadata, links, templates);
	}

	private HalFormsDocument<T> withResource(@Nullable T resource) {
		return new HalFormsDocument<T>(resource, resources, embedded, pageMetadata, links, templates);
	}

	/**
	 * Adds the given {@link Link} to the current document.
	 *
	 * @param link must not be {@literal null}.
	 * @return
	 */
	public HalFormsDocument<T> andLink(Link link) {

		Assert.notNull(link, "Link must not be null!");

		return new HalFormsDocument<>(resource, resources, embedded, pageMetadata, links.and(link), templates);
	}

	/**
	 * Adds the given {@link HalFormsTemplate} to the current document.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @param template must not be {@literal null}.
	 * @return
	 */
	public HalFormsDocument<T> andTemplate(String name, HalFormsTemplate template) {

		Assert.hasText(name, "Template name must not be null or empty!");
		Assert.notNull(template, "Template must not be null!");

		Map<String, HalFormsTemplate> templates = new HashMap<>(this.templates);
		templates.put(name, template);

		return new HalFormsDocument<>(resource, resources, embedded, pageMetadata, links, templates);
	}

	/**
	 * Adds the given value as embedded one.
	 *
	 * @param key must not be {@literal null} or empty.
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public HalFormsDocument<T> andEmbedded(HalLinkRelation key, Object value) {

		Assert.notNull(key, "Embedded key must not be null!");
		Assert.notNull(value, "Embedded value must not be null!");

		Map<HalLinkRelation, Object> embedded = new HashMap<>(this.embedded);
		embedded.put(key, value);

		return new HalFormsDocument<>(resource, resources, embedded, pageMetadata, links, templates);
	}

	HalFormsDocument<T> withLinks(Links links) {
		return new HalFormsDocument<>(resource, resources, embedded, pageMetadata, links, templates);
	}
}
