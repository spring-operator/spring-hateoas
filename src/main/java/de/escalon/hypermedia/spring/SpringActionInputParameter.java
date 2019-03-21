/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.Options;
import de.escalon.hypermedia.action.Select;
import de.escalon.hypermedia.action.StringOptions;
import de.escalon.hypermedia.action.Type;
import de.escalon.hypermedia.affordance.ActionDescriptor;
import de.escalon.hypermedia.affordance.ActionInputParameter;
import de.escalon.hypermedia.affordance.DataType;
import de.escalon.hypermedia.affordance.ParameterType;
import de.escalon.hypermedia.affordance.SimpleSuggest;
import de.escalon.hypermedia.affordance.Suggest;
import de.escalon.hypermedia.affordance.SuggestType;

/**
 * Describes a Spring MVC rest services method parameter value with recorded sample call value and input constraints.
 *
 * @author Dietrich Schulten
 */
public class SpringActionInputParameter implements ActionInputParameter {

	private static final String[] EMPTY = new String[0];

	private static final List<Suggest<?>> EMPTY_SUGGEST = Collections.emptyList();

	private final TypeDescriptor typeDescriptor;

	private RequestBody requestBody;

	private RequestParam requestParam;

	private PathVariable pathVariable;

	private RequestHeader requestHeader;

	private final MethodParameter methodParameter;

	private final Object value;

	private Boolean arrayOrCollection = null;

	private final Map<String, Object> inputConstraints = new HashMap<String, Object>();

	Suggest<?>[] possibleValues;

	String[] excluded = EMPTY;

	String[] readOnly = EMPTY;

	String[] hidden = EMPTY;

	String[] include = EMPTY;

	boolean editable = true;

	ParameterType type = ParameterType.UNKNOWN;

	@SuppressWarnings({ "unchecked", "rawtypes" }) PossibleValuesResolver<?> resolver = new FixedPossibleValuesResolver(
			EMPTY_SUGGEST, SuggestType.INTERNAL);

	private static final ConversionService DEFAULT_CONVERSION_SERVICE = new DefaultFormattingConversionService();

	private final ConversionService conversionService;

	private Type fieldType;

	private final String name;

	/**
	 * Creates action input parameter.
	 *
	 * @param methodParameter to describe
	 * @param value used during sample invocation
	 * @param conversionService to apply to value
	 */
	public SpringActionInputParameter(final MethodParameter methodParameter, final Object value,
			final ConversionService conversionService, final String name) {
		this.methodParameter = methodParameter;
		this.value = value;
		this.name = name;
		Annotation[] annotations = methodParameter.getParameterAnnotations();
		Input inputAnnotation = null;
		Select select = null;
		for (Annotation annotation : annotations) {
			if (RequestBody.class.isInstance(annotation)) {
				requestBody = (RequestBody) annotation;
			} else if (RequestParam.class.isInstance(annotation)) {
				requestParam = (RequestParam) annotation;
			} else if (PathVariable.class.isInstance(annotation)) {
				pathVariable = (PathVariable) annotation;
			} else if (RequestHeader.class.isInstance(annotation)) {
				requestHeader = (RequestHeader) annotation;
			} else if (Input.class.isInstance(annotation)) {
				inputAnnotation = (Input) annotation;
			} else if (Select.class.isInstance(annotation)) {
				select = (Select) annotation;
			}
		}

		/**
		 * Check if annotations indicate that is required, for now only for request params & headers
		 */
		boolean requiredByAnnotations = (requestParam != null && requestParam.required())
				|| (requestHeader != null && requestHeader.required());

		if (inputAnnotation != null) {
			putInputConstraint(ActionInputParameter.MIN, Integer.MIN_VALUE, inputAnnotation.min());
			putInputConstraint(ActionInputParameter.MAX, Integer.MAX_VALUE, inputAnnotation.max());
			putInputConstraint(ActionInputParameter.MIN_LENGTH, Integer.MIN_VALUE, inputAnnotation.minLength());
			putInputConstraint(ActionInputParameter.MAX_LENGTH, Integer.MAX_VALUE, inputAnnotation.maxLength());
			putInputConstraint(ActionInputParameter.STEP, 0, inputAnnotation.step());
			putInputConstraint(ActionInputParameter.PATTERN, "", inputAnnotation.pattern());
			setReadOnly(!inputAnnotation.editable());

			/**
			 * Check if annotations indicate that is required
			 */
			setRequired(inputAnnotation.required() || requiredByAnnotations);

			excluded = inputAnnotation.exclude();
			readOnly = inputAnnotation.readOnly();
			hidden = inputAnnotation.hidden();
			include = inputAnnotation.include();
			type = ParameterType.INPUT;
		} else {
			setReadOnly(select != null ? !select.editable() : !editable);
			putInputConstraint(ActionInputParameter.REQUIRED, "", requiredByAnnotations);
		}
		if (inputAnnotation == null || inputAnnotation.value() == Type.FROM_JAVA) {
			if (isArrayOrCollection() || isRequestBody()) {
				fieldType = null;
			} else if (DataType.isNumber(getParameterType())) {
				fieldType = Type.NUMBER;
			} else {
				fieldType = Type.TEXT;
			}
		} else {
			fieldType = inputAnnotation.value();
		}
		createResolver(methodParameter, select);
		this.conversionService = conversionService;
		typeDescriptor = TypeDescriptor.nested(methodParameter, 0);
	}

	public SpringActionInputParameter(final MethodParameter methodParameter, final Object value, final String name) {
		this(methodParameter, value, DEFAULT_CONVERSION_SERVICE, name);
	}

	/**
	 * Creates new ActionInputParameter with default formatting conversion service.
	 *
	 * @param methodParameter holding metadata about the parameter
	 * @param value during sample method invocation
	 */

	public SpringActionInputParameter(final MethodParameter methodParameter, final Object value) {
		this(methodParameter, value, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void createResolver(final MethodParameter methodParameter, final Select select) {
		Class<?> parameterType = methodParameter.getNestedParameterType();
		Class<?> nested;
		SuggestType type = SuggestType.INTERNAL;
		if (select != null && select.required()) {
			type = select.type();
			putInputConstraint(ActionInputParameter.REQUIRED, "", true);
		}

		if (select != null && (select.options() != StringOptions.class || !isEnumType(parameterType))) {
			resolver = new OptionsPossibleValuesResolver<Object>(select);
			this.type = ParameterType.SELECT;
		} else if (Enum[].class.isAssignableFrom(parameterType)) {
			resolver = new FixedPossibleValuesResolver(
					SimpleSuggest.wrap(parameterType.getComponentType().getEnumConstants()), type);
			this.type = ParameterType.SELECT;
		} else if (Enum.class.isAssignableFrom(parameterType)) {
			resolver = new FixedPossibleValuesResolver(SimpleSuggest.wrap(parameterType.getEnumConstants()), type);
			this.type = ParameterType.SELECT;
		} else if (Collection.class.isAssignableFrom(parameterType)) {
			TypeDescriptor descriptor = TypeDescriptor.nested(methodParameter, 1);
			if (descriptor != null) {
				nested = descriptor.getType();
				if (Enum.class.isAssignableFrom(nested)) {
					resolver = new FixedPossibleValuesResolver(SimpleSuggest.wrap(nested.getEnumConstants()), type);
					this.type = ParameterType.SELECT;
				}
			}
		}

	}

	private boolean isEnumType(final Class<?> parameterType) {
		return Enum[].class.isAssignableFrom(parameterType) || Enum.class.isAssignableFrom(parameterType)
				|| Collection.class.isAssignableFrom(parameterType)
						&& Enum.class.isAssignableFrom(TypeDescriptor.nested(methodParameter, 1).getType());
	}

	private void putInputConstraint(final String key, final Object defaultValue, final Object value) {
		if (!value.equals(defaultValue)) {
			inputConstraints.put(key, value);
		}
	}

	/**
	 * The value of the parameter at sample invocation time.
	 *
	 * @return value, may be null
	 */
	@Override
	public Object getValue() {
		return value;
	}

	/**
	 * The value of the parameter at sample invocation time, formatted according to conversion configuration.
	 *
	 * @return value, may be null
	 */
	@Override
	public String getValueFormatted() {
		String ret;
		if (value == null) {
			ret = null;
		} else {
			ret = (String) conversionService.convert(value, typeDescriptor, TypeDescriptor.valueOf(String.class));
		}
		return ret;
	}

	/**
	 * Gets HTML5 parameter type for input field according to {@link Type} annotation.
	 *
	 * @return the type
	 */
	@Override
	public Type getHtmlInputFieldType() {
		return fieldType;
	}

	@Override
	public void setHtmlInputFieldType(final Type type) {
		fieldType = type;
	}

	@Override
	public boolean isRequestBody() {
		return requestBody != null;
	}

	@Override
	public boolean isRequestParam() {
		return requestParam != null;
	}

	@Override
	public boolean isPathVariable() {
		return pathVariable != null;
	}

	@Override
	public boolean isRequestHeader() {
		return requestHeader != null;
	}

	public boolean isInputParameter() {
		return type == ParameterType.INPUT && requestBody == null && pathVariable == null && requestHeader == null
				&& requestParam == null;
	}

	@Override
	public String getRequestHeaderName() {
		return isRequestHeader() ? requestHeader.value() : null;
	}

	/**
	 * Has constraints defined via <code>@Input</code> annotation. Note that there might also be other kinds of
	 * constraints, e.g. <code>@Select</code> may define values for {@link #getPossibleValues}.
	 *
	 * @return true if parameter is constrained
	 */
	@Override
	public boolean hasInputConstraints() {
		return !inputConstraints.isEmpty();
	}

	@Override
	public <T extends Annotation> T getAnnotation(final Class<T> annotation) {
		return methodParameter.getParameterAnnotation(annotation);
	}

	/**
	 * Determines if request body input parameter has a hidden input property.
	 *
	 * @param property name or property path
	 * @return true if hidden
	 */
	boolean isHidden(final String property) {
		return arrayContains(hidden, property);
	}

	boolean isReadOnly(final String property) {
		return (!editable || arrayContains(readOnly, property));
	}

	@Override
	public void setReadOnly(final boolean readOnly) {
		editable = !readOnly;
		putInputConstraint(ActionInputParameter.EDITABLE, "", editable);
	}

	@Override
	public void setRequired(final boolean required) {
		putInputConstraint(ActionInputParameter.REQUIRED, "", required);
	}

	boolean isIncluded(final String property) {
		if (isExcluded(property)) {
			return false;
		}
		if (include == null || include.length == 0) {
			return true;
		}
		return containsPropertyIncludeValue(property);
	}

	/**
	 * Find out if property is included by searching through all annotations.
	 *
	 * @param property
	 * @return
	 */
	private boolean containsPropertyIncludeValue(final String property) {
		return arrayContains(readOnly, property) || arrayContains(hidden, property) || arrayContains(include, property);
	}

	/**
	 * Determines if request body input parameter should be excluded, considering {@link Input#exclude}.
	 *
	 * @param property name or property path
	 * @return true if excluded, false if no include statement found or not excluded
	 */
	private boolean isExcluded(final String property) {
		return excluded != null && arrayContains(excluded, property);
	}

	private boolean arrayContains(final String[] array, final String toFind) {
		if (array == null || array.length == 0) {
			return false;
		}
		for (String item : array) {
			if (toFind.equals(item)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> List<Suggest<T>> getPossibleValues(final ActionDescriptor actionDescriptor) {
		List<Object> from = new ArrayList<Object>();
		for (String paramName : resolver.getParams()) {
			ActionInputParameter parameterValue = actionDescriptor.getActionInputParameter(paramName);
			if (parameterValue != null) {
				from.add(parameterValue.getValue());
			}
		}

		return (List) resolver.getValues(from);

	}

	@Override
	public <T> void setPossibleValues(final List<Suggest<T>> possibleValues) {
		resolver = new FixedPossibleValuesResolver<T>(possibleValues, resolver.getType());
	}

	@Override
	public SuggestType getSuggestType() {
		return resolver.getType();
	}

	@Override
	public void setSuggestType(SuggestType type) {
		resolver.setType(type);
	}

	/**
	 * Determines if action input parameter is an array or collection.
	 *
	 * @return true if array or collection
	 */
	@Override
	public boolean isArrayOrCollection() {
		if (arrayOrCollection == null) {
			arrayOrCollection = DataType.isArrayOrCollection(getParameterType());
		}
		return arrayOrCollection;
	}

	/**
	 * Is this action input parameter required, based on the presence of a default value, the parameter annotations and
	 * the kind of input parameter.
	 *
	 * @return true if required
	 */
	@Override
	public boolean isRequired() {
		if (isRequestBody()) {
			return requestBody.required();
		} else if (isRequestParam()) {
			return !(isDefined(requestParam.defaultValue()) || !requestParam.required());
		} else if (isRequestHeader()) {
			return !(isDefined(requestHeader.defaultValue()) || !requestHeader.required());
		} else {
			return true;
		}
	}

	private boolean isDefined(final String defaultValue) {
		return !ValueConstants.DEFAULT_NONE.equals(defaultValue);
	}

	/**
	 * Determines default value of request param or request header, if available.
	 *
	 * @return value or null
	 */
	public String getDefaultValue() {
		String ret;
		if (isRequestParam()) {
			ret = isDefined(requestParam.defaultValue()) ? requestParam.defaultValue() : null;
		} else if (isRequestHeader()) {
			ret = !(ValueConstants.DEFAULT_NONE.equals(requestHeader.defaultValue())) ? requestHeader.defaultValue() : null;
		} else {
			ret = null;
		}
		return ret;
	}

	/**
	 * Allows convenient access to multiple call values in case that this input parameter is an array or collection. Make
	 * sure to check {@link #isArrayOrCollection()} before calling this method.
	 *
	 * @return call values or empty array
	 * @throws UnsupportedOperationException if this input parameter is not an array or collection
	 */
	@Override
	public Object[] getValues() {
		Object[] callValues;
		if (!isArrayOrCollection()) {
			throw new UnsupportedOperationException("parameter is not an array or collection");
		}
		Object callValue = getValue();
		if (callValue == null) {
			callValues = new Object[0];
		} else {
			Class<?> parameterType = getParameterType();
			if (parameterType.isArray()) {
				callValues = (Object[]) callValue;
			} else {
				callValues = ((Collection<?>) callValue).toArray();
			}
		}
		return callValues;
	}

	/**
	 * Was a sample call value recorded for this parameter?
	 *
	 * @return if call value is present
	 */
	@Override
	public boolean hasValue() {
		return value != null;
	}

	/**
	 * Gets parameter name of this action input parameter.
	 *
	 * @return name
	 */
	@Override
	public String getParameterName() {
		String ret;
		String parameterName = methodParameter.getParameterName();
		if (parameterName == null) {
			methodParameter.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
			ret = methodParameter.getParameterName();
		} else {
			ret = parameterName;
		}
		return ret;
	}

	/**
	 * Class which declares the method to which this input parameter belongs.
	 *
	 * @return class
	 */
	public Class<?> getDeclaringClass() {
		return methodParameter.getDeclaringClass();
	}

	/**
	 * Type of parameter.
	 *
	 * @return type
	 */
	@Override
	public Class<?> getParameterType() {
		return methodParameter.getParameterType();
	}

	/**
	 * Generic type of parameter.
	 *
	 * @return generic type
	 */
	@Override
	public java.lang.reflect.Type getGenericParameterType() {
		return methodParameter.getGenericParameterType();
	}

	/**
	 * Gets the input constraints defined for this action input parameter.
	 *
	 * @return constraints
	 */
	@Override
	public Map<String, Object> getInputConstraints() {
		return inputConstraints;
	}

	@Override
	public String toString() {
		String kind;
		if (isRequestBody()) {
			kind = "RequestBody";
		} else if (isPathVariable()) {
			kind = "PathVariable";
		} else if (isRequestParam()) {
			kind = "RequestParam";
		} else if (isRequestHeader()) {
			kind = "RequestHeader";
		} else {
			kind = "nested bean property";
		}
		return kind + (getParameterName() != null ? " " + getParameterName() : "") + ": "
				+ (value != null ? value.toString() : "no value");
	}

	private static <T extends Options<V>, V> Options<V> getOptions(final Class<? extends Options<V>> beanType) {
		Options<V> options = getBean(beanType);
		if (options == null) {
			try {
				options = beanType.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return options;
	}

	private static <T> T getBean(final Class<T> beanType) {
		try {
			RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
			HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();

			WebApplicationContext context = WebApplicationContextUtils
					.getWebApplicationContext(servletRequest.getSession().getServletContext());
			Map<String, T> beans = context.getBeansOfType(beanType);
			if (!beans.isEmpty()) {
				return beans.values().iterator().next();
			}
		} catch (Exception e) {}
		return null;
	}

	public void setExcluded(final String[] excluded) {
		this.excluded = excluded;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ParameterType getType() {
		return type;
	}

	public void setType(final ParameterType type) {
		this.type = type;
	}

	interface PossibleValuesResolver<T> {
		String[] getParams();

		List<Suggest<T>> getValues(List<?> value);

		SuggestType getType();

		void setType(SuggestType type);
	}

	class FixedPossibleValuesResolver<T> implements PossibleValuesResolver<T> {

		private final List<Suggest<T>> values;
		private SuggestType type;

		public FixedPossibleValuesResolver(final List<Suggest<T>> values, SuggestType type) {
			this.values = values;
			setType(type);
		}

		@Override
		public String[] getParams() {
			return EMPTY;
		}

		@Override
		public List<Suggest<T>> getValues(final List<?> value) {
			return values;
		}

		@Override
		public SuggestType getType() {
			return type;
		}

		@Override
		public void setType(SuggestType type) {
			this.type = type;
		}

	}

	class OptionsPossibleValuesResolver<T> implements PossibleValuesResolver<T> {
		private final Options<T> options;

		private final Select select;

		private SuggestType type;

		@SuppressWarnings("unchecked")
		public OptionsPossibleValuesResolver(final Select select) {
			this.select = select;
			setType(select.type());
			options = getOptions((Class<Options<T>>) select.options());
		}

		@Override
		public String[] getParams() {
			return select.args();
		}

		@Override
		public List<Suggest<T>> getValues(final List<?> args) {
			return options.get(select.value(), args.toArray());
		}

		@Override
		public SuggestType getType() {
			return type;
		}

		@Override
		public void setType(SuggestType type) {
			this.type = type;
		}
	}

}
