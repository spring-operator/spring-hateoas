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
package org.springframework.hateoas.mediatype.hal;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation.HalLinkRelationBuilder;

/**
 * Unit tests for {@link HalLinkRelation}.
 *
 * @author Oliver Drotbohm
 */
public class HalLinkRelationUnitTest {

	@Test // #841
	public void createsCuriedLinkRelation() {

		HalLinkRelation relation = HalLinkRelation.curied("curie", "relation");

		assertThat(relation.isCuried()).isTrue();
		assertThat(relation.value()).isEqualTo("curie:relation");
	}

	@Test // #841
	public void curieBuilderCreatesLinkRelations() {

		HalLinkRelationBuilder builder = HalLinkRelation.curieBuilder("curie");

		assertThat(builder.relation("relation").value()).isEqualTo("curie:relation");
	}

	@Test // #841
	public void parsesCuriedLinkRelation() {

		HalLinkRelation relation = HalLinkRelation.of(LinkRelation.of("already:curied"));

		assertThat(relation.isCuried()).isTrue();
		assertThat(relation.getLocalPart()).isEqualTo("curied");
	}

	@Test // #841
	public void parsesUncuriedLinkRelation() {

		HalLinkRelation relation = HalLinkRelation.of(LinkRelation.of("uncuried"));

		assertThat(relation.isCuried()).isFalse();
		assertThat(relation.getLocalPart()).isEqualTo("uncuried");
	}

	@Test // 841
	public void returnsHalLinkRelationOnCreation() {

		HalLinkRelation relation = HalLinkRelation.curied("curie", "relation");

		assertThat(HalLinkRelation.of(relation)).isSameAs(relation);
	}

	@Test // 841
	public void curiesLinkRelation() {

		HalLinkRelation relation = HalLinkRelation.of(LinkRelation.of("relation"));

		HalLinkRelation curiedRelation = relation.curie("curie");

		assertThat(curiedRelation.isCuried()).isTrue();
		assertThat(curiedRelation.curie("otherCurie")).isEqualTo(HalLinkRelation.curied("otherCurie", "relation"));
	}

	@Test // 841
	public void skipsCurieIfAlreadyCuried() {

		HalLinkRelation relation = HalLinkRelation.curied("curie", "relation");

		assertThat(relation.curieIfUncuried("otherCurie")).isEqualTo(relation);
	}

	@Test // #841
	public void skipsCurieForIanaLinkRelation() {

		HalLinkRelation relation = HalLinkRelation.of(IanaLinkRelations.SELF).curieIfUncuried("curie");

		assertThat(relation.isCuried()).isFalse();
	}

	@Test // #841
	public void exposesValueAndLocalPartAsMessageCode() {

		HalLinkRelation relation = HalLinkRelation.curied("curie", "relation");

		assertThat(relation.getCodes()).containsExactly("_links.curie:relation.title", "_links.relation.title");
	}
}
