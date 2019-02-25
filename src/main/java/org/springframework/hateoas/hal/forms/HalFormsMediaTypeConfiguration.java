/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.hateoas.hal.forms;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.hateoas.core.DelegatingRelProvider;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Spring configuration for HAL Forms support.
 *
 * @author Greg Turnquist
 * @author Oliver Drotbohm
 */
@Configuration
@RequiredArgsConstructor
class HalFormsMediaTypeConfiguration implements HypermediaMappingInformation {

	private final DelegatingRelProvider relProvider;
	private final ObjectProvider<CurieProvider> curieProvider;
	private final ObjectProvider<HalFormsConfiguration> halFormsConfiguration;
	private final MessageSourceAccessor messageSourceAccessor;

	@Bean
	LinkDiscoverer linkDiscoverer() {
		return new HalFormsLinkDiscoverer();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.config.HypermediaMappingInformation#getMediaTypes()
	 */
	@Override
	public List<MediaType> getMediaTypes() {
		return HypermediaType.HAL_FORMS.getMediaTypes();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.config.HypermediaMappingInformation#configureObjectMapper(com.fasterxml.jackson.databind.ObjectMapper)
	 */
	@Override
	public ObjectMapper configureObjectMapper(ObjectMapper mapper) {

		ObjectMapper mapper1 = mapper.copy();

		mapper1.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper1.registerModule(new Jackson2HalFormsModule());
		mapper1.setHandlerInstantiator(
				new Jackson2HalFormsModule.HalFormsHandlerInstantiator(relProvider, curieProvider.getIfAvailable(),
						messageSourceAccessor, true, halFormsConfiguration.getIfAvailable(HalFormsConfiguration::new)));

		return mapper1;
	}

}
