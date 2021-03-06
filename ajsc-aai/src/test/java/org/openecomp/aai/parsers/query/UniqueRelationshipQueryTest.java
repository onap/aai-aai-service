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

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.IntrospectorFactory;
import org.openecomp.aai.introspection.LoaderFactory;
import org.openecomp.aai.introspection.ModelInjestor;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.logging.LogLineBuilder;
import org.openecomp.aai.parsers.query.QueryParser;
import org.openecomp.aai.serialization.engines.QueryStyle;
import org.openecomp.aai.serialization.engines.TitanDBEngine;
import org.openecomp.aai.serialization.engines.TransactionalGraphEngine;

public class UniqueRelationshipQueryTest {
	private ModelInjestor injestor = ModelInjestor.getInstance();
	private LogLineBuilder llBuilder = new LogLineBuilder("TEST", "TEST");
	private TransactionalGraphEngine dbEngine = 
			new TitanDBEngine(QueryStyle.GREMLIN_UNIQUE, 
				LoaderFactory.createLoaderForVersion(ModelType.MOXY, Version.v8, llBuilder),
				false);
	private final Version version = Version.v8;
	private DynamicJAXBContext context = injestor.getContextForVersion(version);
	private Unmarshaller unmarshaller = null;
	
	/**
	 * Configure.
	 */
	@BeforeClass
	public static void configure() {
		System.setProperty("AJSC_HOME", ".");
		System.setProperty("BUNDLECONFIG_DIR", "bundleconfig-local");
	}
	
	
	/**
	 * Setup.
	 *
	 * @throws JAXBException the JAXB exception
	 */
	@Before
	public void setup() throws JAXBException {
		unmarshaller = context.createUnmarshaller();
	    unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
	    unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, false);
		unmarshaller.setProperty(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
	}
	
	/**
	 * Parent query.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	@Ignore
	@Test
    public void parentQuery() throws JAXBException, UnsupportedEncodingException, AAIException {
		
		String content =
				"{"
				+ "\"related-to\" : \"pserver\","
				+ "\"relationship-data\" : [{"
				+ "\"relationship-key\" : \"pserver.hostname\","
				+ "\"relationship-value\" : \"key1\""
				+ "}]"
				+ "}";
						
		Object obj = context.newDynamicEntity("Relationship");

		DynamicEntity entity = (DynamicEntity)unmarshaller.unmarshal(new StreamSource(new StringReader(content)), obj.getClass()).getValue();
			
		Introspector wrappedObj = IntrospectorFactory.newInstance(ModelType.MOXY, entity, llBuilder);
		QueryParser query = dbEngine.getQueryBuilder().createQueryFromRelationship(wrappedObj);
		String key = "pserver/key1";
		GraphTraversal<Vertex, Vertex> expected = 
				__.<Vertex>start().has("aai-unique-key", key);
		String resultType = "pserver";
		String containerType = "";
		
		testSet(query, expected, expected, resultType, containerType);
		
    }

	/**
	 * Child query.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	@Ignore
	@Test
    public void childQuery() throws JAXBException, UnsupportedEncodingException, AAIException {
		String content =
				"{"
				+ "\"related-to\" : \"lag-interface\","
				+ "\"relationship-data\" : [{"
				+ "\"relationship-key\" : \"pserver.hostname\","
				+ "\"relationship-value\" : \"key1\""
				+ "}, {"
				+ "\"relationship-key\" : \"lag-interface.interface-name\","
				+ "\"relationship-value\" : \"key2\""
				+ "}]"
				+ "}";
						
		Object obj = context.newDynamicEntity("Relationship");

		DynamicEntity entity = (DynamicEntity)unmarshaller.unmarshal(new StreamSource(new StringReader(content)), obj.getClass()).getValue();
			
		Introspector wrappedObj = IntrospectorFactory.newInstance(ModelType.MOXY, entity, llBuilder);
		QueryParser query = dbEngine.getQueryBuilder().createQueryFromRelationship(wrappedObj);

		String key = "pserver/key1/lag-interface/key2";
		GraphTraversal<Vertex, Vertex> expected = 
				__.<Vertex>start().has("aai-unique-key", key);
		GraphTraversal<Vertex, Vertex> parentExpected = 
				__.<Vertex>start().has("aai-unique-key", "pserver/key1");
		String resultType = "lag-interface";
		String containerType = "";
		
		testSet(query, expected, parentExpected, resultType, containerType);
    }
	
	/**
	 * Naming exceptions.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	@Ignore
	@Test
    public void namingExceptions() throws JAXBException, UnsupportedEncodingException, AAIException {
		String uri = "/vces/vce/key1/port-groups/port-group/key2/cvlan-tags/cvlan-tag/655";
		String content =
				"{"
				+ "\"related-to\" : \"cvlan-tag\","
				+ "\"relationship-data\" : [{"
				+ "\"relationship-key\" : \"vce.vnf-id\","
				+ "\"relationship-value\" : \"key1\""
				+ "}, {"
				+ "\"relationship-key\" : \"port-group.interface-id\","
				+ "\"relationship-value\" : \"key2\""
				+ "},{"
				+ "\"relationship-key\" : \"cvlan-tag.cvlan-tag\","
				+ "\"relationship-value\" : \"655\""
				+ "}]"
				+ "}";
						
		Object obj = context.newDynamicEntity("Relationship");

		DynamicEntity entity = (DynamicEntity)unmarshaller.unmarshal(new StreamSource(new StringReader(content)), obj.getClass()).getValue();
			
		Introspector wrappedObj = IntrospectorFactory.newInstance(ModelType.MOXY, entity, llBuilder);
		QueryParser query = dbEngine.getQueryBuilder().createQueryFromRelationship(wrappedObj);
		String key = "vce/key1/port-group/key2/cvlan-tag/655";
		GraphTraversal<Vertex, Vertex> expected = 
				__.<Vertex>start().has("aai-unique-key", key);
		GraphTraversal<Vertex, Vertex> parentExpected = 
				__.<Vertex>start().has("aai-unique-key", "vce/key1/port-group/key2");
		String resultType = "cvlan-tag";
		String containerType = "";
		
		testSet(query, expected, parentExpected, resultType, containerType);
		
    }
	
	/**
	 * Double key.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	@Ignore
	@Test
    public void doubleKey() throws JAXBException, UnsupportedEncodingException, AAIException {
		String uri = "/service-capabilities/service-capability/key1/key2";
		String content =
				"{"
				+ "\"related-to\" : \"service-capability\","
				+ "\"relationship-data\" : [{"
				+ "\"relationship-key\" : \"service-capability.service-type\","
				+ "\"relationship-value\" : \"key1\""
				+ " }, { "
				+ "\"relationship-key\" : \"service-capability.vnf-type\","
				+ " \"relationship-value\" : \"key2\""
				+ " }]"
				+ "}";
						
		Object obj = context.newDynamicEntity("Relationship");

		DynamicEntity entity = (DynamicEntity)unmarshaller.unmarshal(new StreamSource(new StringReader(content)), obj.getClass()).getValue();
			
		Introspector wrappedObj = IntrospectorFactory.newInstance(ModelType.MOXY, entity, llBuilder);
		QueryParser query = dbEngine.getQueryBuilder().createQueryFromRelationship(wrappedObj);

		String key = "service-capability/key1/key2";
		GraphTraversal<Vertex, Vertex> expected = 
				__.<Vertex>start().has("aai-unique-key", key);
		GraphTraversal<Vertex, Vertex> parentExpected = 
				__.<Vertex>start().has("aai-unique-key", "service-capability/key1/key2");
		String resultType = "service-capability";
		String containerType = "";
		
		testSet(query, expected, parentExpected, resultType, containerType);
		
    }
	
	/**
	 * Short circuit.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	@Ignore
	@Test
    public void shortCircuit() throws JAXBException, UnsupportedEncodingException, AAIException {
		String content =
				"{"
				+ "\"related-to\" : \"cvlan-tag\","
				+ "\"related-link\" : \"http://mock-system-name.com:8443/aai/v6/network/vces/vce/key1/port-groups/port-group/key2/cvlan-tags/cvlan-tag/655\","
				+ "\"relationship-data\" : [{"
				+ "\"relationship-key\" : \"vce.hostname\","
				+ "\"relationship-value\" : \"key1\""
				+ "}, {"
				+ "\"relationship-key\" : \"port-group.interface-name\","
				+ "\"relationship-value\" : \"key2\""
				+ "},{"
				+ "\"relationship-key\" : \"cvlan-tag.-name\","
				+ "\"relationship-value\" : \"655\""
				+ "}]"
				+ "}";
						
		Object obj = context.newDynamicEntity("Relationship");

		DynamicEntity entity = (DynamicEntity)unmarshaller.unmarshal(new StreamSource(new StringReader(content)), obj.getClass()).getValue();
			
		Introspector wrappedObj = IntrospectorFactory.newInstance(ModelType.MOXY, entity, llBuilder);
		QueryParser query = dbEngine.getQueryBuilder().createQueryFromRelationship(wrappedObj);

		String uri = "network/vces/vce/key1/port-groups/port-group/key2/cvlan-tags/cvlan-tag/655";
		String key = "vce/key1/port-group/key2/cvlan-tag/655";
		GraphTraversal<Vertex, Vertex> expected = __.<Vertex>start().has("aai-unique-key", key);
		GraphTraversal<Vertex, Vertex> parentExpected = __.<Vertex>start().has("aai-unique-key", "vce/key1/port-group/key2");
		String resultType = "cvlan-tag";
		String containerType = "";
		
		testSet(query, expected, parentExpected, resultType, containerType);
		
    }
	
	/**
	 * Test set.
	 *
	 * @param query the query
	 * @param expected the expected
	 * @param parentExpected the parent expected
	 * @param resultType the result type
	 * @param containerType the container type
	 */
	public void testSet(QueryParser query, GraphTraversal<Vertex, Vertex> expected, GraphTraversal<Vertex, Vertex> parentExpected, String resultType, String containerType) {
		assertEquals(
				"gremlin query should be " + expected,
				expected,
				query.getQueryBuilder().getQuery());
		assertEquals(
				"parent gremlin query should be " + parentExpected,
				parentExpected,
				query.getParentQueryBuilder().getParentQuery());
		assertEquals(
				"result type should be " + resultType,
				resultType,
				query.getResultType());
		assertEquals(
				"container type should be " + containerType,
				containerType,
				query.getContainerType());
	}
}
