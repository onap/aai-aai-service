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

package org.openecomp.aai.parsers.query;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.parsers.uri.Parsable;
import org.openecomp.aai.parsers.uri.URIParser;
import org.openecomp.aai.parsers.uri.URIToDBKey;
import org.openecomp.aai.query.builder.QueryBuilder;

import com.att.aft.dme2.internal.javaxwsrs.core.UriBuilder;

/**
 * The Class UniqueURIQueryParser.
 */
public class UniqueURIQueryParser extends QueryParser implements Parsable {

	
	private URIToDBKey dbKeyParser = null;
	
	private Introspector previous = null;
	
	private boolean endsInContainer = false;
	
	private Introspector finalContainer = null;
	
	private String parentName = "";
	
	/**
	 * Instantiates a new unique URI query parser.
	 *
	 * @param loader the loader
	 * @param queryBuilder the query builder
	 * @param uri the uri
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws AAIException the AAI exception
	 */
	public UniqueURIQueryParser(Loader loader, QueryBuilder queryBuilder, URI uri) throws UnsupportedEncodingException, IllegalArgumentException, AAIException {
		super(loader, queryBuilder, uri);
		URIParser parser = new URIParser(loader, uri);
		parser.parse(this);
		
		if (!endsInContainer) {
			this.dbKeyParser = new URIToDBKey(loader, uri);
			String dbKey = (String)dbKeyParser.getResult();
			queryBuilder.getVerticesByIndexedProperty("aai-unique-key", dbKey);
			queryBuilder.formBoundary();
			
			if (!(parentName.equals("") || parentName.equals(this.resultResource))) {
				URI parentUri = UriBuilder.fromPath(uri.getRawPath().substring(0, uri.getRawPath().indexOf(containerResource))).build();
				this.dbKeyParser = new URIToDBKey(loader, parentUri);
				this.parentQueryBuilder = queryBuilder.newInstance().getVerticesByIndexedProperty("aai-unique-key", (String)dbKeyParser.getResult());
				this.parentResourceType = parentName;
			} 
			this.containerResource = "";
		} else {
			URI parentUri = UriBuilder.fromPath(uri.getRawPath().substring(0, uri.getRawPath().indexOf(this.finalContainer.getDbName()))).build();
			this.dbKeyParser = new URIToDBKey(loader, parentUri);
			String dbKey = (String)dbKeyParser.getResult();
			this.parentResourceType = parentName;

			if (!dbKey.equals("")) {
				queryBuilder.getVerticesByIndexedProperty("aai-unique-key", dbKey);
				queryBuilder.formBoundary();
				queryBuilder.createEdgeTraversal(previous, finalContainer);

			} 
			
			queryBuilder.createContainerQuery(finalContainer);
				
			
		}
	}
	
	
	/**
	 * Instantiates a new unique URI query parser.
	 *
	 * @param loader the loader
	 * @param queryBuilder the query builder
	 */
	public UniqueURIQueryParser(Loader loader, QueryBuilder queryBuilder) {
		super(loader, queryBuilder);
	}


	/**
	 * @{inheritDoc}
	 */
	@Override
	public void processObject(Introspector obj, Map<String, String> uriKeys) {
		this.resultResource = obj.getDbName();
		if (previous != null) {
			this.parentName = previous.getDbName();
		}
		this.previous  = obj;
		
		
	}


	/**
	 * @{inheritDoc}
	 */
	@Override
	public void processContainer(Introspector obj, Map<String, String> uriKeys, boolean isFinalContainer) {
		this.containerResource = obj.getName();
		if (previous != null) {
			this.parentName = previous.getDbName();
		}
		if (isFinalContainer) {
			this.endsInContainer = true;
			this.resultResource = obj.getChildDBName();
			
			this.finalContainer = obj;
		}
		
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void processNamespace(Introspector obj) {
	
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getCloudRegionTransform() {
		return "add";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public boolean useOriginalLoader() {
		return false;
	}
	
}
