/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.hateoas.server.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.plugin.core.PluginRegistry;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class DelegatingLinkRelationProvider implements LinkRelationProvider {

	private final @NonNull PluginRegistry<LinkRelationProvider, Class<?>> providers;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.LinkRelationProvider#getItemResourceRelFor(java.lang.Class)
	 */
	@Override
	public LinkRelation getItemResourceRelFor(Class<?> type) {
		return providers.getRequiredPluginFor(type).getItemResourceRelFor(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.server.LinkRelationProvider#getCollectionResourceRelFor(java.lang.Class)
	 */
	@Override
	public LinkRelation getCollectionResourceRelFor(java.lang.Class<?> type) {
		return providers.getRequiredPluginFor(type).getCollectionResourceRelFor(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(java.lang.Class<?> delimiter) {
		return providers.hasPluginFor(delimiter);
	}
}
