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

package org.openecomp.aai.query.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.openecomp.aai.db.AAIProperties;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.serialization.db.EdgeRule;
import org.openecomp.aai.serialization.db.EdgeRules;

import com.google.common.base.Joiner;

/**
 * The Class GremlinQueryBuilder.
 */
public abstract class GremlinQueryBuilder extends QueryBuilder {
	
	private EdgeRules edgeRules = EdgeRules.getInstance();
	
	private List<String> list = null;
	
	private int parentStepIndex = 0;
	
	private int stepIndex = 0;
	
	/**
	 * Instantiates a new gremlin query builder.
	 *
	 * @param loader the loader
	 */
	public GremlinQueryBuilder(Loader loader) {
		super(loader);
		list = new ArrayList<String>();
	}
	
	/**
	 * Instantiates a new gremlin query builder.
	 *
	 * @param loader the loader
	 * @param start the start
	 */
	public GremlinQueryBuilder(Loader loader, Vertex start) {
		super(loader, start);
		list = new ArrayList<String>();
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder createDBQuery(Introspector obj) {
		this.createKeyQuery(obj);
		this.createContainerQuery(obj);
		return this;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder getVerticesByIndexedProperty(String key, Object value) {
		return this.getVerticesByProperty(key, value);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder getVerticesByProperty(String key, Object value) {

		String term = "";
		if (value != null && !value.getClass().getName().equals("java.lang.String")) {
			term = value.toString();
		} else {
			term = "'" + value + "'";
		}
		list.add(".has('" + key + "', " + term + ")");
		stepIndex++;
		return this;
	}
	
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder getChildVerticesFromParent(String parentKey, String parentValue, String childType) {
		/*
		String query = ".has('aai-node-type', '" + childType + "')";
		
		return this.processGremlinQuery(parentKey, parentValue, query);
		*/
		//TODO
		return this;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder getTypedVerticesByMap(String type, LinkedHashMap<String, String> map) {
		
		for (String key : map.keySet()) {
			list.add(".has('" + key + "', '" + map.get(key) + "')");
			stepIndex++;
		}
		list.add(".has('aai-node-type', '" + type + "')");
		stepIndex++;
		return this;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder createKeyQuery(Introspector obj) {
		List<String> keys = obj.getKeys();

		for (String key : keys) {
			
			this.getVerticesByProperty(key, obj.getValue(key));
			
		}		
		return this;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder createEdgeTraversal(Introspector parent, Introspector child) {
		String parentName = parent.getDbName();
		String childName = child.getDbName();
		if (parent.isContainer()) {
			parentName = parent.getChildDBName();
		}
		if (child.isContainer()) {
			childName = child.getChildDBName();
		}
		this.edgeQuery(parentName, childName);
		return this;
			
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder createEdgeTraversal(Vertex parent, Introspector child) {
		String nodeType = parent.<String>property(AAIProperties.NODE_TYPE).orElse(null);
		this.edgeQuery(nodeType, child.getDbName());
		
		return this;
			
	}
	
	/**
	 * Edge query.
	 *
	 * @param outType the out type
	 * @param inType the in type
	 */
	private void edgeQuery(String outType, String inType) {
		formBoundary();
		EdgeRule rule;
		String label = "";
		try {
			rule = edgeRules.getEdgeRule(outType, inType);
			label = rule.getLabel();
		} catch (AAIException e) {
			// TODO Auto-generated catch block
		}
		list.add(".out('" +
				label + "')");
		stepIndex++;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public QueryBuilder createContainerQuery(Introspector obj) {
		String type = obj.getChildDBName();
		String abstractType = obj.getMetadata("abstract");
		if (abstractType != null) {
			String command = ".or(";
			List<String> ors = new ArrayList<>();
			String[] inheritors = obj.getMetadata("inheritors").split(",");
			for (int i = 0; i < inheritors.length; i++) {
				ors.add("_().has('aai-node-type', '" + inheritors[i] + "')");
			}
			command += Joiner.on(",").join(ors);
			command += ")";
			list.add(command);
		} else {
			list.add(".has('aai-node-type', '" + type + "')");
		}
		stepIndex++;

		return this;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public Object getParentQuery() {
		StringBuilder sb = new StringBuilder();
		if (parentStepIndex == 0) {
			parentStepIndex = stepIndex;
		}
		for (int i = 0; i < parentStepIndex; i++) {
			sb.append(this.list.get(i));
		}
		
		return sb.toString();
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public Object getQuery() {
		StringBuilder sb = new StringBuilder();
		
		for (String piece : this.list) {
			sb.append(piece);
		}
		
		return sb.toString();
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void formBoundary() {
		parentStepIndex = stepIndex;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public Vertex getStart() {
		return this.start;
	}
	
}
