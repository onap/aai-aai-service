/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.parsers.uri;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.logging.LogLineBuilder;
import org.openecomp.aai.rest.HttpMethod;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;

/**
 * The Class URIToExtensionInformation.
 */
public class URIToExtensionInformation implements Parsable {

	private String namespace = "";
	
	private String methodName = "";
	
	private String topObject = "";
	
	private List<String> pieces = null;
	
	/**
	 * Instantiates a new URI to extension information.
	 *
	 * @param loader the loader
	 * @param uri the uri
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws AAIException the AAI exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	public URIToExtensionInformation(Loader loader, URI uri) throws IllegalArgumentException, AAIException, UnsupportedEncodingException {
		pieces = new ArrayList<>();
		URIParser parser = new URIParser(loader, uri);
		parser.parse(this);
		
		this.methodName = Joiner.on("").join(this.pieces);
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void processObject(Introspector obj, Map<String, String> uriKeys) {
		String upperCamel = toUpperCamel(obj.getDbName());
		if (topObject.equals("")) {
			topObject = upperCamel;
		}
		pieces.add(upperCamel);

	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void processContainer(Introspector obj, Map<String, String> uriKeys, boolean isFinalContainer) {
		pieces.add(toUpperCamel(obj.getName()));
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void processNamespace(Introspector obj) {
		this.namespace = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, obj.getDbName());
		pieces.add(toUpperCamel(obj.getDbName()));

	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getCloudRegionTransform() {
		return "remove";
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public boolean useOriginalLoader() {
		return true;
	}
	
	/**
	 * Gets the namespace.
	 *
	 * @return the namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}
	
	/**
	 * Gets the top object.
	 *
	 * @return the top object
	 */
	public String getTopObject() {
		return this.topObject;
	}
	
	/**
	 * Gets the method name.
	 *
	 * @param httpMethod the http method
	 * @param isPreprocess the is preprocess
	 * @return the method name
	 */
	public String getMethodName(HttpMethod httpMethod, boolean isPreprocess) {
		String result = "Dynamic";
		if (httpMethod.equals(HttpMethod.PUT)) {
			result += "Add";
		} else if (httpMethod.equals(HttpMethod.DELETE)) {
			result += "Del";
		} else {
			throw new IllegalArgumentException("http method not supported: " + httpMethod);
		}
		result += this.methodName;
		
		if (isPreprocess) {
			result += "PreProc";
		} else {
			result += "PostProc";
		}
		return result;
	}

	/**
	 * To upper camel.
	 *
	 * @param name the name
	 * @return the string
	 */
	private String toUpperCamel(String name) {
		String result = "";
		result = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, name);
		return result;
	}
}
