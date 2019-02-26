/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.uber;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.support.PropertyUtils;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Core element containing either a {@link Link} or a single property inside an {@link UberDocument}.
 *
 * @author Greg Turnquist
 * @since 1.0
 */
@Value
@Wither(AccessLevel.PACKAGE)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class UberData {

	private @Getter(onMethod = @__({@Nullable})) String id;
	private @Getter(onMethod = @__({@Nullable})) String name;
	private @Getter(onMethod = @__({@Nullable})) String label;
	private @Getter(onMethod = @__({@Nullable})) List<LinkRelation> rel;
	private @Getter(onMethod = @__({@Nullable})) String url;
	private UberAction action;
	private boolean transclude;
	private @Getter(onMethod = @__({@Nullable})) String model;
	private @Getter(onMethod = @__({@Nullable})) List<String> sending;
	private @Getter(onMethod = @__({@Nullable})) List<String> accepting;
	private @Getter(onMethod = @__({@Nullable})) Object value;
	private @Getter(onMethod = @__({@Nullable})) List<UberData> data;

	@JsonCreator
	UberData(@JsonProperty("id") @Nullable String id, @JsonProperty("name") @Nullable String name, @JsonProperty("label") @Nullable String label,
			@JsonProperty("rel") @Nullable List<LinkRelation> rel, @JsonProperty("url") @Nullable String url,
			@JsonProperty("action") @Nullable UberAction action, @JsonProperty("transclude") boolean transclude,
			@JsonProperty("model") @Nullable String model, @JsonProperty("sending") @Nullable List<String> sending,
			@JsonProperty("accepting") @Nullable List<String> accepting, @JsonProperty("value") @Nullable Object value,
			@JsonProperty("data") @Nullable List<UberData> data) {

		this.id = id;
		this.name = name;
		this.label = label;
		this.rel = rel;
		this.url = url;
		this.action = action;
		this.transclude = transclude;
		this.model = model;
		this.sending = sending;
		this.accepting = accepting;
		this.value = value;
		this.data = data;
	}

	UberData() {
		this(null, null, null, null, null, UberAction.READ, false, null, null, null, null, null);
	}

	/**
	 * Don't render if it's {@link UberAction#READ}.
	 */
	@Nullable
	public UberAction getAction() {
		return action == UberAction.READ ? null : action;
	}

	/*
	 * Use a {@link Boolean} to support returning {@literal null}, and if it is {@literal null}, don't render.
	 */
	@Nullable
	public Boolean isTemplated() {

		return Optional.ofNullable(this.url) //
				.map(s -> s.contains("{?") ? true : null) //
				.orElse(null);
	}

	/*
	 * Use a {@link Boolean} to support returning {@literal null}, and if it is {@literal null}, don't render.
	 */
	@Nullable
	public Boolean isTransclude() {
		return this.transclude ? this.transclude : null;
	}

	/**
	 * Fetch all the links found in this {@link UberData}.
	 */
	@JsonIgnore
	public List<Link> getLinks() {

		return Optional.ofNullable(this.rel) //
				.map(rels -> rels.stream() //
						.map(rel -> new Link(this.url, rel)) //
						.collect(Collectors.toList())) //
				.orElse(Collections.emptyList());
	}

	/**
	 * Simple scalar types that can be encoded by value, not type.
	 */
	private final static HashSet<Class<?>> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(String.class));

	/**
	 * Set of all Spring HATEOAS resource types.
	 */
	private static final HashSet<Class<?>> RESOURCE_TYPES = new HashSet<>(
			Arrays.asList(ResourceSupport.class, Resource.class, Resources.class, PagedResources.class));

	/**
	 * Convert a {@link ResourceSupport} into a list of {@link UberData}s, containing links and content.
	 *
	 * @param resource
	 * @return
	 */
	static List<UberData> extractLinksAndContent(ResourceSupport resource) {

		List<UberData> data = extractLinks(resource);

		extractContent(resource).ifPresent(data::add);

		return data;
	}

	/**
	 * Convert a {@link Resource} into a list of {@link UberData}s, containing links and content.
	 *
	 * @param resource
	 * @return
	 */
	static List<UberData> extractLinksAndContent(Resource<?> resource) {

		List<UberData> data = extractLinks(resource);

		extractContent(resource.getContent()).ifPresent(data::add);

		return data;
	}

	/**
	 * Convert {@link Resources} into a list of {@link UberData}, with each item nested in a sub-UberData.
	 *
	 * @param resources
	 * @return
	 */
	static List<UberData> extractLinksAndContent(Resources<?> resources) {

		List<UberData> data = extractLinks(resources);

		data.addAll(resources.getContent().stream().map(UberData::doExtractLinksAndContent)
				.map(uberData -> new UberData().withData(uberData)).collect(Collectors.toList()));

		return data;
	}

	static List<UberData> extractLinksAndContent(PagedResources<?> resources) {

		List<UberData> collectionOfResources = extractLinksAndContent((Resources<?>) resources);

		if (resources.getMetadata() != null) {

			collectionOfResources.add(new UberData().withName("page")
					.withData(Arrays.asList(new UberData().withName("number").withValue(resources.getMetadata().getNumber()),
							new UberData().withName("size").withValue(resources.getMetadata().getSize()),
							new UberData().withName("totalElements").withValue(resources.getMetadata().getTotalElements()),
							new UberData().withName("totalPages").withValue(resources.getMetadata().getTotalPages()))));
		}

		return collectionOfResources;
	}

	/**
	 * Convert a {@link List} of {@link Link}s into a list of {@link UberData}.
	 *
	 * @param links
	 * @return
	 */
	private static List<UberData> extractLinks(Links links) {

		return urlRelMap(links).entrySet().stream() //
				.map(entry -> new UberData() //
						.withUrl(entry.getKey()) //
						.withRel(entry.getValue().getRels())) //
				.collect(Collectors.toList());
	}

	/**
	 * Extract all the direct {@link Link}s and {@link Affordance}-based links from a {@link ResourceSupport}.
	 *
	 * @param resource
	 * @return
	 */
	private static List<UberData> extractLinks(ResourceSupport resource) {

		List<UberData> data = new ArrayList<>();

		List<UberData> links = extractLinks(resource.getLinks());
		List<UberData> affordanceBasedLinks = extractAffordances(resource.getLinks());

		if (affordanceBasedLinks.isEmpty()) {
			data.addAll(links);
		} else {
			data.addAll(mergeDeclaredLinksIntoAffordanceLinks(affordanceBasedLinks, links));
		}

		return data;
	}

	/**
	 * Convert an object's properties into an {@link UberData}.
	 *
	 * @param content
	 * @return
	 */
	private static Optional<UberData> extractContent(@Nullable Object content) {

		return Optional.ofNullable(content) //
				.filter(it -> !RESOURCE_TYPES.contains(content.getClass())) //
				.map(it -> new UberData() //
						.withName(StringUtils.uncapitalize(it.getClass().getSimpleName())) //
						.withData(extractProperties(it)));
	}

	/**
	 * Extract links and content from an object of any type.
	 */
	private static List<UberData> doExtractLinksAndContent(Object item) {

		if (item instanceof Resource) {
			return extractLinksAndContent((Resource<?>) item);
		}

		if (item instanceof ResourceSupport) {
			return extractLinksAndContent((ResourceSupport) item);
		}

		return extractLinksAndContent(new Resource<>(item));
	}

	/**
	 * Turn a {@list List} of {@link Link}s into a {@link Map}, where you can see ALL the rels of a given link.
	 *
	 * @param links
	 * @return a map with links mapping onto a {@link List} of rels
	 */
	private static Map<String, LinkAndRels> urlRelMap(Links links) {

		Map<String, LinkAndRels> urlRelMap = new LinkedHashMap<>();

		links.forEach(link -> {
			LinkAndRels linkAndRels = urlRelMap.computeIfAbsent(link.getHref(), s -> new LinkAndRels());
			linkAndRels.setLink(link);
			linkAndRels.getRels().add(link.getRel());
		});

		return urlRelMap;
	}

	/**
	 * Find all the {@link Affordance}s for a set of {@link Link}s, and convert them into {@link UberData}.
	 *
	 * @param links
	 * @return
	 */
	private static List<UberData> extractAffordances(Links links) {

		return links.stream() //
				.flatMap(it -> it.getAffordances().stream()) //
				.map(it -> it.getAffordanceModel(MediaTypes.UBER_JSON)) //
				.map(UberAffordanceModel.class::cast) //
				.map(it -> {

					if (it.hasHttpMethod(HttpMethod.GET)) {

						String suffix = it.getQueryProperties().stream() //
								.map(UberData::getName) //
								.collect(Collectors.joining(","));

						if (!it.getQueryMethodParameters().isEmpty()) {
							suffix = "{?" + suffix + "}";
						}

						return new UberData() //
								.withName(it.getName()) //
								.withRel(Collections.singletonList(LinkRelation.of(it.getName())))
								.withUrl(it.getLink().expand().getHref() + suffix) //
								.withAction(it.getAction());

					} else {

						return new UberData() //
								.withName(it.getName()) //
								.withRel(Collections.singletonList(LinkRelation.of(it.getName())))
								.withUrl(it.getLink().expand().getHref()).withModel(it.getInputProperties().stream() //
										.map(UberData::getName).map(property -> property + "={" + property + "}") //
										.collect(Collectors.joining("&")))
								.withAction(it.getAction());
					}
				}).collect(Collectors.toList());
	}

	/**
	 * Take a list of {@link Affordance}-based {@link Link}s, and overlay them with intersecting, declared {@link Link}s.
	 *
	 * @param affordanceBasedLinks
	 * @param links
	 * @return
	 */
	private static List<UberData> mergeDeclaredLinksIntoAffordanceLinks(List<UberData> affordanceBasedLinks,
			List<UberData> links) {

		return affordanceBasedLinks.stream() //
				.flatMap(affordance -> links.stream() //
						.filter(link -> link.getUrl().equals(affordance.getUrl())) //
						.map(link -> {

							if (link.getAction() == affordance.getAction()) {

								List<LinkRelation> rels = new ArrayList<>(link.getRel());
								rels.addAll(affordance.getRel());

								return affordance.withName(rels.get(0).value()) //
										.withRel(rels);
							} else {
								return affordance;
							}
						}))
				.collect(Collectors.toList());
	}

	/**
	 * Transform the payload of a {@link Resource} into {@link UberData}.
	 *
	 * @param obj
	 * @return
	 */
	private static List<UberData> extractProperties(Object obj) {

		if (PRIMITIVE_TYPES.contains(obj.getClass())) {
			return Collections.singletonList(new UberData().withValue(obj));
		}

		return PropertyUtils.findProperties(obj).entrySet().stream()
				.map(entry -> new UberData().withName(entry.getKey()).withValue(entry.getValue())).collect(Collectors.toList());
	}

	/**
	 * Holds both a {@link Link} and related {@literal rels}.
	 */
	@Data
	private static class LinkAndRels {

		private Link link;
		private List<LinkRelation> rels = new ArrayList<>();
	}
}
