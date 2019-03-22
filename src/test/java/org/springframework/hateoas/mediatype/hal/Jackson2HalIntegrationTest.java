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
package org.springframework.hateoas.mediatype.hal;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.hateoas.AbstractJackson2MarshallingIntegrationTest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.HalConfiguration.RenderSingleLinks;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule.HalHandlerInstantiator;
import org.springframework.hateoas.server.core.AnnotationLinkRelationProvider;
import org.springframework.hateoas.server.core.EmbeddedWrappers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for Jackson 2 HAL integration.
 *
 * @author Alexander Baetz
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Jeffrey Walraven
 */
public class Jackson2HalIntegrationTest extends AbstractJackson2MarshallingIntegrationTest {

	static final String SINGLE_LINK_REFERENCE = "{\"_links\":{\"self\":{\"href\":\"localhost\"}}}";
	static final String LIST_LINK_REFERENCE = "{\"_links\":{\"self\":[{\"href\":\"localhost\"},{\"href\":\"localhost2\"}]}}";

	static final String SIMPLE_EMBEDDED_RESOURCE_REFERENCE = "{\"_embedded\":{\"content\":[\"first\",\"second\"]},\"_links\":{\"self\":{\"href\":\"localhost\"}}}";
	static final String SINGLE_EMBEDDED_RESOURCE_REFERENCE = "{\"_embedded\":{\"content\":[{\"text\":\"test1\",\"number\":1,\"_links\":{\"self\":{\"href\":\"localhost\"}}}]},\"_links\":{\"self\":{\"href\":\"localhost\"}}}";
	static final String LIST_EMBEDDED_RESOURCE_REFERENCE = "{\"_embedded\":{\"content\":[{\"text\":\"test1\",\"number\":1,\"_links\":{\"self\":{\"href\":\"localhost\"}}},{\"text\":\"test2\",\"number\":2,\"_links\":{\"self\":{\"href\":\"localhost\"}}}]},\"_links\":{\"self\":{\"href\":\"localhost\"}}}";

	static final String ANNOTATED_EMBEDDED_RESOURCE_REFERENCE = "{\"_embedded\":{\"pojos\":[{\"text\":\"test1\",\"number\":1,\"_links\":{\"self\":{\"href\":\"localhost\"}}}]},\"_links\":{\"self\":{\"href\":\"localhost\"}}}";
	static final String ANNOTATED_EMBEDDED_RESOURCES_REFERENCE = "{\"_embedded\":{\"pojos\":[{\"text\":\"test1\",\"number\":1,\"_links\":{\"self\":{\"href\":\"localhost\"}}},{\"text\":\"test2\",\"number\":2,\"_links\":{\"self\":{\"href\":\"localhost\"}}}]}}";

	static final String ANNOTATED_PAGED_RESOURCES = "{\"_embedded\":{\"pojos\":[{\"text\":\"test1\",\"number\":1,\"_links\":{\"self\":{\"href\":\"localhost\"}}},{\"text\":\"test2\",\"number\":2,\"_links\":{\"self\":{\"href\":\"localhost\"}}}]},\"_links\":{\"next\":{\"href\":\"foo\"},\"prev\":{\"href\":\"bar\"}},\"page\":{\"size\":2,\"totalElements\":4,\"totalPages\":2,\"number\":0}}";

	static final Links PAGINATION_LINKS = Links.of(new Link("foo", IanaLinkRelations.NEXT.value()),
			new Link("bar", IanaLinkRelations.PREV.value()));

	static final String CURIED_DOCUMENT = "{\"_links\":{\"self\":{\"href\":\"foo\"},\"foo:myrel\":{\"href\":\"bar\"},\"curies\":[{\"href\":\"http://localhost:8080/rels/{rel}\",\"name\":\"foo\",\"templated\":true}]}}";
	static final String MULTIPLE_CURIES_DOCUMENT = "{\"_links\":{\"default:myrel\":{\"href\":\"foo\"},\"curies\":[{\"href\":\"bar\",\"name\":\"foo\"},{\"href\":\"foo\",\"name\":\"bar\"}]}}";
	static final String SINGLE_NON_CURIE_LINK = "{\"_links\":{\"self\":{\"href\":\"foo\"}}}";
	static final String EMPTY_DOCUMENT = "{}";

	static final String LINK_TEMPLATE = "{\"_links\":{\"search\":{\"href\":\"/foo{?bar}\",\"templated\":true}}}";

	static final String LINK_WITH_TITLE = "{\"_links\":{\"ns:foobar\":{\"href\":\"target\",\"title\":\"Foobar's title!\"}}}";

	static final String SINGLE_WITH_ONE_EXTRA_ATTRIBUTES = "{\"_links\":{\"self\":{\"href\":\"localhost\",\"title\":\"the title\"}}}";
	static final String SINGLE_WITH_ALL_EXTRA_ATTRIBUTES = "{\"_links\":{\"self\":{\"href\":\"localhost\",\"hreflang\":\"en\",\"title\":\"the title\",\"type\":\"the type\",\"deprecation\":\"/customers/deprecated\"}}}";

	@Before
	public void setUpModule() {

		mapper.registerModule(new Jackson2HalModule());
		mapper.setHandlerInstantiator(
				new HalHandlerInstantiator(new AnnotationLinkRelationProvider(), null, null, new HalConfiguration()));
	}

	/**
	 * @see #29
	 */
	@Test
	public void rendersSingleLinkAsObject() throws Exception {

		RepresentationModel<?> resourceSupport = new RepresentationModel<>();
		resourceSupport.add(new Link("localhost"));

		assertThat(write(resourceSupport)).isEqualTo(SINGLE_LINK_REFERENCE);
	}

	/**
	 * @see #100
	 */
	@Test
	public void rendersAllExtraRFC5988Attributes() throws Exception {

		RepresentationModel<?> resourceSupport = new RepresentationModel<>();
		resourceSupport.add(new Link("localhost", "self") //
				.withHreflang("en") //
				.withTitle("the title") //
				.withType("the type") //
				.withMedia("the media") //
				.withDeprecation("/customers/deprecated"));

		assertThat(write(resourceSupport)).isEqualTo(SINGLE_WITH_ALL_EXTRA_ATTRIBUTES);
	}

	/**
	 * HAL doesn't support "media" so it's removed from the "expected" link.
	 *
	 * @see #699
	 */
	@Test
	public void deserializeAllExtraRFC5988Attributes() throws Exception {

		RepresentationModel<?> expected = new RepresentationModel<>();
		expected.add(new Link("localhost", "self") //
				.withHreflang("en") //
				.withTitle("the title") //
				.withType("the type") //
				.withDeprecation("/customers/deprecated"));

		assertThat(read(SINGLE_WITH_ALL_EXTRA_ATTRIBUTES, RepresentationModel.class)).isEqualTo(expected);
	}

	@Test
	public void rendersWithOneExtraRFC5988Attribute() throws Exception {

		RepresentationModel<?> resourceSupport = new RepresentationModel<>();
		resourceSupport.add(new Link("localhost", "self").withTitle("the title"));

		assertThat(write(resourceSupport)).isEqualTo(SINGLE_WITH_ONE_EXTRA_ATTRIBUTES);
	}

	/**
	 * @see #699
	 */
	@Test
	public void deserializeOneExtraRFC5988Attribute() throws Exception {

		RepresentationModel<?> expected = new RepresentationModel<>();
		expected.add(new Link("localhost", "self").withTitle("the title"));

		assertThat(read(SINGLE_WITH_ONE_EXTRA_ATTRIBUTES, RepresentationModel.class)).isEqualTo(expected);
	}

	@Test
	public void deserializeSingleLink() throws Exception {
		RepresentationModel<?> expected = new RepresentationModel<>();
		expected.add(new Link("localhost"));
		assertThat(read(SINGLE_LINK_REFERENCE, RepresentationModel.class)).isEqualTo(expected);
	}

	/**
	 * @see #29
	 */
	@Test
	public void rendersMultipleLinkAsArray() throws Exception {

		RepresentationModel<?> resourceSupport = new RepresentationModel<>();
		resourceSupport.add(new Link("localhost"));
		resourceSupport.add(new Link("localhost2"));

		assertThat(write(resourceSupport)).isEqualTo(LIST_LINK_REFERENCE);
	}

	@Test
	public void deserializeMultipleLinks() throws Exception {

		RepresentationModel<?> expected = new RepresentationModel<>();
		expected.add(new Link("localhost"));
		expected.add(new Link("localhost2"));

		assertThat(read(LIST_LINK_REFERENCE, RepresentationModel.class)).isEqualTo(expected);
	}

	@Test
	public void rendersSimpleResourcesAsEmbedded() throws Exception {

		List<String> content = new ArrayList<>();
		content.add("first");
		content.add("second");

		CollectionModel<String> resources = new CollectionModel<>(content);
		resources.add(new Link("localhost"));

		assertThat(write(resources)).isEqualTo(SIMPLE_EMBEDDED_RESOURCE_REFERENCE);
	}

	@Test
	public void deserializesSimpleResourcesAsEmbedded() throws Exception {

		List<String> content = new ArrayList<>();
		content.add("first");
		content.add("second");

		CollectionModel<String> expected = new CollectionModel<>(content);
		expected.add(new Link("localhost"));

		CollectionModel<String> result = mapper.readValue(SIMPLE_EMBEDDED_RESOURCE_REFERENCE,
				mapper.getTypeFactory().constructParametricType(CollectionModel.class, String.class));

		assertThat(result).isEqualTo(expected);

	}

	@Test
	public void rendersSingleResourceResourcesAsEmbedded() throws Exception {

		List<EntityModel<SimplePojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimplePojo("test1", 1), new Link("localhost")));

		CollectionModel<EntityModel<SimplePojo>> resources = new CollectionModel<>(
				content);
		resources.add(new Link("localhost"));

		assertThat(write(resources)).isEqualTo(SINGLE_EMBEDDED_RESOURCE_REFERENCE);
	}

	@Test
	public void deserializesSingleResourceResourcesAsEmbedded() throws Exception {

		List<EntityModel<SimplePojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimplePojo("test1", 1), new Link("localhost")));

		CollectionModel<EntityModel<SimplePojo>> expected = new CollectionModel<>(
				content);
		expected.add(new Link("localhost"));

		CollectionModel<EntityModel<SimplePojo>> result = mapper.readValue(
				SINGLE_EMBEDDED_RESOURCE_REFERENCE,
				mapper.getTypeFactory().constructParametricType(CollectionModel.class,
						mapper.getTypeFactory().constructParametricType(EntityModel.class, SimplePojo.class)));

		assertThat(result).isEqualTo(expected);

	}

	@Test
	public void rendersMultipleResourceResourcesAsEmbedded() throws Exception {

		CollectionModel<EntityModel<SimplePojo>> resources = setupResources();
		resources.add(new Link("localhost"));

		assertThat(write(resources)).isEqualTo(LIST_EMBEDDED_RESOURCE_REFERENCE);
	}

	@Test
	public void deserializesMultipleResourceResourcesAsEmbedded() throws Exception {

		CollectionModel<EntityModel<SimplePojo>> expected = setupResources();
		expected.add(new Link("localhost"));

		CollectionModel<EntityModel<SimplePojo>> result = mapper.readValue(
				LIST_EMBEDDED_RESOURCE_REFERENCE,
				mapper.getTypeFactory().constructParametricType(CollectionModel.class,
						mapper.getTypeFactory().constructParametricType(EntityModel.class, SimplePojo.class)));

		assertThat(result).isEqualTo(expected);
	}

	/**
	 * @see #47, #60
	 */
	@Test
	public void serializesAnnotatedResourceResourcesAsEmbedded() throws Exception {

		List<EntityModel<SimpleAnnotatedPojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimpleAnnotatedPojo("test1", 1), new Link("localhost")));

		CollectionModel<EntityModel<SimpleAnnotatedPojo>> resources = new CollectionModel<>(
				content);
		resources.add(new Link("localhost"));

		assertThat(write(resources)).isEqualTo(ANNOTATED_EMBEDDED_RESOURCE_REFERENCE);
	}

	/**
	 * @see #47, #60
	 */
	@Test
	public void deserializesAnnotatedResourceResourcesAsEmbedded() throws Exception {

		List<EntityModel<SimpleAnnotatedPojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimpleAnnotatedPojo("test1", 1), new Link("localhost")));

		CollectionModel<EntityModel<SimpleAnnotatedPojo>> expected = new CollectionModel<>(
				content);
		expected.add(new Link("localhost"));

		CollectionModel<EntityModel<SimpleAnnotatedPojo>> result = mapper.readValue(
				ANNOTATED_EMBEDDED_RESOURCE_REFERENCE,
				mapper.getTypeFactory().constructParametricType(CollectionModel.class, mapper.getTypeFactory()
						.constructParametricType(EntityModel.class, SimpleAnnotatedPojo.class)));

		assertThat(result).isEqualTo(expected);
	}

	/**
	 * @see #63
	 */
	@Test
	public void serializesMultipleAnnotatedResourceResourcesAsEmbedded() throws Exception {
		assertThat(write(setupAnnotatedResources())).isEqualTo(ANNOTATED_EMBEDDED_RESOURCES_REFERENCE);
	}

	/**
	 * @see #63
	 */
	@Test
	public void deserializesMultipleAnnotatedResourceResourcesAsEmbedded() throws Exception {

		CollectionModel<EntityModel<SimpleAnnotatedPojo>> result = mapper.readValue(
				ANNOTATED_EMBEDDED_RESOURCES_REFERENCE,
				mapper.getTypeFactory().constructParametricType(CollectionModel.class, mapper.getTypeFactory()
						.constructParametricType(EntityModel.class, SimpleAnnotatedPojo.class)));

		assertThat(result).isEqualTo(setupAnnotatedResources());
	}

	/**
	 * @see #63
	 */
	@Test
	public void serializesPagedResource() throws Exception {
		assertThat(write(setupAnnotatedPagedResources())).isEqualTo(ANNOTATED_PAGED_RESOURCES);
	}

	/**
	 * @see #64
	 */
	@Test
	public void deserializesPagedResource() throws Exception {
		PagedModel<EntityModel<SimpleAnnotatedPojo>> result = mapper.readValue(
				ANNOTATED_PAGED_RESOURCES,
				mapper.getTypeFactory().constructParametricType(PagedModel.class, mapper.getTypeFactory()
						.constructParametricType(EntityModel.class, SimpleAnnotatedPojo.class)));

		assertThat(result).isEqualTo(setupAnnotatedPagedResources());
	}

	/**
	 * @see #125
	 */
	@Test
	public void rendersCuriesCorrectly() throws Exception {

		CollectionModel<Object> resources = new CollectionModel<>(Collections.emptySet(),
				new Link("foo"), new Link("bar", "myrel"));

		assertThat(getCuriedObjectMapper().writeValueAsString(resources)).isEqualTo(CURIED_DOCUMENT);
	}

	/**
	 * @see #125
	 */
	@Test
	public void doesNotRenderCuriesIfNoLinkIsPresent() throws Exception {

		CollectionModel<Object> resources = new CollectionModel<>(Collections.emptySet());
		assertThat(getCuriedObjectMapper().writeValueAsString(resources)).isEqualTo(EMPTY_DOCUMENT);
	}

	/**
	 * @see #125
	 */
	@Test
	public void doesNotRenderCuriesIfNoCurieLinkIsPresent() throws Exception {

		CollectionModel<Object> resources = new CollectionModel<>(Collections.emptySet());
		resources.add(new Link("foo"));

		assertThat(getCuriedObjectMapper().writeValueAsString(resources)).isEqualTo(SINGLE_NON_CURIE_LINK);
	}

	/**
	 * @see #137
	 */
	@Test
	public void rendersTemplate() throws Exception {

		RepresentationModel<?> support = new RepresentationModel<>();
		support.add(new Link("/foo{?bar}", "search"));

		assertThat(write(support)).isEqualTo(LINK_TEMPLATE);
	}

	/**
	 * @see #142
	 */
	@Test
	public void rendersMultipleCuries() throws Exception {

		CollectionModel<Object> resources = new CollectionModel<>(Collections.emptySet());
		resources.add(new Link("foo", "myrel"));

		CurieProvider provider = new DefaultCurieProvider("default", new UriTemplate("/doc{?rel}")) {
			@Override
			public Collection<? extends Object> getCurieInformation(Links links) {
				return Arrays.asList(new Curie("foo", "bar"), new Curie("bar", "foo"));
			}
		};

		assertThat(getCuriedObjectMapper(provider, null).writeValueAsString(resources)).isEqualTo(MULTIPLE_CURIES_DOCUMENT);
	}

	/**
	 * @see #286, #236
	 */
	@Test
	public void rendersEmptyEmbeddedCollections() throws Exception {

		EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

		List<Object> values = new ArrayList<>();
		values.add(wrappers.emptyCollectionOf(SimpleAnnotatedPojo.class));

		CollectionModel<Object> resources = new CollectionModel<>(values);

		assertThat(write(resources)).isEqualTo("{\"_embedded\":{\"pojos\":[]}}");
	}

	/**
	 * @see #378
	 */
	@Test
	public void rendersTitleIfMessageSourceResolvesNamespacedKey() throws Exception {
		verifyResolvedTitle("_links.ns:foobar.title");
	}

	/**
	 * @see #378
	 */
	@Test
	public void rendersTitleIfMessageSourceResolvesLocalKey() throws Exception {
		verifyResolvedTitle("_links.foobar.title");
	}

	@Test
	public void rendersSingleLinkAsArrayWhenConfigured() throws Exception {

		mapper.setHandlerInstantiator(new HalHandlerInstantiator(new AnnotationLinkRelationProvider(), null, null,
				new HalConfiguration().withRenderSingleLinks(RenderSingleLinks.AS_ARRAY)));

		RepresentationModel<?> resourceSupport = new RepresentationModel<>();
		resourceSupport.add(new Link("localhost").withSelfRel());

		assertThat(write(resourceSupport)).isEqualTo("{\"_links\":{\"self\":[{\"href\":\"localhost\"}]}}");
	}

	/**
	 * @see #667
	 */
	@Test
	public void handleTemplatedLinksOnDeserialization() throws IOException {

		RepresentationModel<?> original = new RepresentationModel<>();
		original.add(new Link("/orders{?id}", "order"));

		String serialized = mapper.writeValueAsString(original);

		String expected = "{\"_links\":{\"order\":{\"href\":\"/orders{?id}\",\"templated\":true}}}";

		assertThat(serialized).isEqualTo(expected);

		RepresentationModel<?> deserialized = mapper.readValue(serialized, RepresentationModel.class);

		assertThat(deserialized).isEqualTo(original);
	}

	@Test // #811
	public void rendersSpecificRelWithSingleLinkAsArrayIfConfigured() throws Exception {

		mapper.setHandlerInstantiator(new HalHandlerInstantiator(new AnnotationLinkRelationProvider(), null, null,
				new HalConfiguration().withRenderSingleLinksFor("foo", RenderSingleLinks.AS_ARRAY)));

		RepresentationModel<?> resource = new RepresentationModel<>();
		resource.add(new Link("/some-href", "foo"));

		assertThat(mapper.writeValueAsString(resource)) //
				.isEqualTo("{\"_links\":{\"foo\":[{\"href\":\"/some-href\"}]}}");
	}

	private static void verifyResolvedTitle(String resourceBundleKey) throws Exception {

		LocaleContextHolder.setLocale(Locale.US);

		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage(resourceBundleKey, Locale.US, "Foobar's title!");

		ObjectMapper objectMapper = getCuriedObjectMapper(null, messageSource);

		RepresentationModel<?> resource = new RepresentationModel<>();
		resource.add(new Link("target", "ns:foobar"));

		assertThat(objectMapper.writeValueAsString(resource)).isEqualTo(LINK_WITH_TITLE);
	}

	private static CollectionModel<EntityModel<SimpleAnnotatedPojo>> setupAnnotatedPagedResources() {

		List<EntityModel<SimpleAnnotatedPojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimpleAnnotatedPojo("test1", 1), new Link("localhost")));
		content.add(new EntityModel<>(new SimpleAnnotatedPojo("test2", 2), new Link("localhost")));

		return new PagedModel<>(content, new PageMetadata(2, 0, 4), PAGINATION_LINKS);
	}

	private static CollectionModel<EntityModel<SimpleAnnotatedPojo>> setupAnnotatedResources() {

		List<EntityModel<SimpleAnnotatedPojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimpleAnnotatedPojo("test1", 1), new Link("localhost")));
		content.add(new EntityModel<>(new SimpleAnnotatedPojo("test2", 2), new Link("localhost")));

		return new CollectionModel<>(content);
	}

	private static CollectionModel<EntityModel<SimplePojo>> setupResources() {

		List<EntityModel<SimplePojo>> content = new ArrayList<>();
		content.add(new EntityModel<>(new SimplePojo("test1", 1), new Link("localhost")));
		content.add(new EntityModel<>(new SimplePojo("test2", 2), new Link("localhost")));

		return new CollectionModel<>(content);
	}

	private static ObjectMapper getCuriedObjectMapper() {

		return getCuriedObjectMapper(new DefaultCurieProvider("foo", new UriTemplate("http://localhost:8080/rels/{rel}")),
				null);
	}

	private static ObjectMapper getCuriedObjectMapper(CurieProvider provider, MessageSource messageSource) {

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jackson2HalModule());
		mapper.setHandlerInstantiator(new HalHandlerInstantiator(new AnnotationLinkRelationProvider(), provider,
				messageSource == null ? null : new MessageSourceAccessor(messageSource)));

		return mapper;
	}
}
