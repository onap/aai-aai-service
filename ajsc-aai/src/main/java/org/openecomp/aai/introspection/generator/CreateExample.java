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

package org.openecomp.aai.introspection.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.IntrospectorWalker;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.Wanderer;

public class CreateExample implements Wanderer {

	private Random rand = new Random();
	private final long range = 100000000L;
	private Loader loader = null;
	private Introspector result = null;
	private String objectName = null;
	private List<String> blacklist = null;
	
	/**
	 * Instantiates a new creates the example.
	 *
	 * @param loader the loader
	 * @param objectName the object name
	 */
	public CreateExample(Loader loader, String objectName) {

		this.loader = loader;
		this.objectName = objectName;
		this.blacklist = new ArrayList<>();
		
	}

	/**
	 * Gets the example object.
	 *
	 * @return the example object
	 */
	public Introspector getExampleObject() {
		result = loader.introspectorFromName(objectName);
		blacklist = new ArrayList<>();
		blacklist.add("any");
		blacklist.add("relationship-list");
		if (!result.isContainer()) {
			blacklist.add("resource-version");
		}
		IntrospectorWalker walker = new IntrospectorWalker(this, loader.getLogLineBuilder());
		
		walker.preventCycles(true);
		walker.setBlacklist(blacklist);
		walker.walk(result);
		//this.getExampleObject(result);
		
		return result;
	}
	
	/**
	 * Gets the value.
	 *
	 * @param property the property
	 * @param type the type
	 * @param suffix the suffix
	 * @return the value
	 */
	private Object getValue(String property, String type, String suffix) {
		long randLong = (long)(rand.nextDouble()*range);
		Integer randInt = rand.nextInt(100000);
		Integer randShrt = rand.nextInt(20000);
		short randShort = randShrt.shortValue();
	
		Object newObj = null;
		if (type.contains("java.lang.String")) {
			newObj = "example-" + property + "-val-" + randInt + suffix;
		} else if ( type.toLowerCase().equals("long") ||type.contains("java.lang.Long")) {
			newObj = randLong;
		} else if(type.toLowerCase().equals("boolean") || type.contains("java.lang.Boolean")){
			newObj = Boolean.TRUE;
		} else if ( type.toLowerCase().equals("int") || type.contains("java.lang.Integer")){
			newObj = randInt;
		}  else if ( type.toLowerCase().equals("short") || type.contains("java.lang.Short")){
			newObj = randShort;
		}
		
		return newObj;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processPrimitive(String propName, Introspector obj) {
		String 	propType = obj.getType(propName);

		Object val = this.getValue(propName, propType, "");
		obj.setValue(propName, val);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processPrimitiveList(String propName, Introspector obj) {
		int listSize = 2;
		String propType = "";
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < listSize; i++) {
			propType = obj.getGenericType(propName);
			Object val = this.getValue(propName, propType, "-" + (i + 1));
			list.add(val);
		}
		obj.setValue(propName, list);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processComplexObj(Introspector obj) {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void modifyComplexList(List<Object> list, Introspector parent, Introspector child) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createComplexObjIfNull() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int createComplexListSize(Introspector parent, Introspector child) {
		return 1;
	}
}
