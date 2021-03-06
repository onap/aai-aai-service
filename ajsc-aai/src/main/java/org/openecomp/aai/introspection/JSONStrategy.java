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

package org.openecomp.aai.introspection;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.openecomp.aai.logging.LogLineBuilder;

public class JSONStrategy extends Introspector {

	private JSONObject json = null;
	private String namedType = "";
	protected JSONStrategy(Object o, LogLineBuilder llBuilder) {
		super(o, llBuilder);
		json = (JSONObject)o;
		//Assumes you provide a wrapper
		Set<String> keySet = json.keySet();
		if (keySet.size() == 1) {
			namedType = keySet.iterator().next();
			json = (JSONObject)json.get(namedType);
		} else {
			throw new IllegalArgumentException("This object has no named type.");
		}
		
		
	}
	
	protected JSONStrategy(Object o, String namedType, LogLineBuilder llBuilder) {
		super(o, llBuilder);
		json = (JSONObject)o;
		this.namedType = namedType;
		
	}
	
	@Override
	public boolean hasProperty(String name) {
		//TODO 
		return true;
	}
	@Override
	public Object getValue(String name) {
		Object result = "";
		result = json.get(name);
		
		return result;
	}

	@Override
	public void setValue(String name, Object obj) {
		json.put(name, obj);
		
	}
	@Override
	public Object getUnderlyingObject() {
		return this.json;
	}
	
	@Override
	public List<String> getProperties() {
		Set<String> set = json.keySet();
		List<String> result = new ArrayList<>();
		result.addAll(set);
		return result;
	}

	@Override
	public List<String> getRequiredProperties() {
		//unknowable
		
		return this.getProperties();
	}

	@Override
	public List<String> getKeys() {
		//unknowable
		return this.getProperties();
	}

	@Override
	public List<String> getAllKeys() {
		//unknowable
		return this.getProperties();
	}

	@Override
	public String getType(String name) {
		String result = "";
		Class<?> resultClass = this.getClass(name);
		if (resultClass != null) {
			result = resultClass.getName();
		}
		
		if (result.equals("org.json.simple.JSONArray")) {
			result = "java.util.List";
		}
		
		return result;
	}

	@Override
	public String getGenericType(String name) {
		String result = "";
		Class<?> resultClass = this.getGenericTypeClass(name);
		if (resultClass != null) {
			result = resultClass.getName();
		}
		return result;
	}

	@Override
	public String getJavaClassName() {
		return json.getClass().getName();
	}

	@Override
	public Class<?> getClass(String name) {
		Class<?> result = null;
		result = json.get(name).getClass();
		
		return result;
	}

	@Override
	public Class<?> getGenericTypeClass(String name) {
		Object resultObject = null;
		Class<?> resultClass = null;
		resultObject = this.getValue(name);
		if (resultObject.getClass().getName().equals("org.json.simple.JSONArray")) {
			resultClass = ((List)resultObject).get(0).getClass();
		}
		
		return resultClass;
	}

	@Override
	public Object newInstanceOfProperty(String name) {
		try {
			return this.getClass(name).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}

	@Override
	public Object newInstanceOfNestedProperty(String name) {
		try {
			return this.getGenericTypeClass(name).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
	
	@Override
	public boolean isComplexType(String name) {
		String result = this.getType(name);
		
		if (result.contains("JSONObject")) {
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public boolean isComplexGenericType(String name) {
		String result = this.getGenericType(name);
		
		if (result.contains("JSONObject")) {
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public boolean isListType(String name) {
		String result = this.getType(name);
		
		if (result.contains("java.util.List")) {
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public boolean isContainer() {
		List<String> props = this.getProperties();
		boolean result = false;
		if (props.size() == 1 && this.isListType(props.get(0))) {
			result = true;
		}
		
		return result;
	}
	@Override
	protected String findKey() {
		return "";
	}
	
	@Override
	public String getName() {
		return this.namedType;
	}
	
	@Override
	public String getDbName() {
		return this.getName();
	}
	
	@Override
	public String getURI() {
		
		// use a UUID for now 
		return UUID.randomUUID().toString();
	}
	
	@Override
	public String getGenericURI() {
		
		//there is none defined for this
		return "";
	}
	
	@Override
	public String preProcessKey (String key) {
		
		// don't do anything with it
		return key;
		
	}
	
	@Override
	public String marshal(MarshallerProperties properties) {
		//TODO
		return null;
	}
	
	@Override
	public Object clone() {
		//TODO
		return null;
	}
	
	/*@Override
	public String findEdgeName(String parent, String child) {
		
		// Always has for now
		return "has";
		
	}*/
	
	@Override
	public ModelType getModelType() {
		return ModelType.JSON;
	}

	@Override
	public List<String> getIndexedProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getChildName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChild(Introspector child) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSimpleType(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSimpleGenericType(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, String> getPropertyMetadata(String prop) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMetadata(String metadataName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getChildDBName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFullGenericURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object get(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void set(String name, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getObjectId() throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Version getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

}
