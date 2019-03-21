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
package org.springframework.hateoas.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Wither;

import org.springframework.hateoas.AffordancePattern;
import org.springframework.hateoas.AffordancePrompt;
import org.springframework.hateoas.AffordanceProperty;
import org.springframework.hateoas.AffordanceReadOnly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Greg Turnquist
 */
@Data
@Wither
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Employee {

	@AffordanceProperty(prompt = "Full name", pattern = "s* s*")
	private String name;

	@AffordancePrompt("Main role")
	@AffordancePattern("Chief .*")
	@AffordanceReadOnly
	private String role;

	Employee() {
		this(null, null);
	}
}
