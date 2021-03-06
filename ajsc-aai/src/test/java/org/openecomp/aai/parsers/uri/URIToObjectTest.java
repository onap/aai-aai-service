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

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.LoaderFactory;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.logging.LogLineBuilder;
import org.openecomp.aai.parsers.uri.URIToObject;

import com.att.aft.dme2.internal.javaxwsrs.core.UriBuilder;


public class URIToObjectTest {

	private Version version = Version.v8;
	private Version currentVersion = Version.v8;
	private Loader loader = LoaderFactory.createLoaderForVersion(ModelType.MOXY, version, new LogLineBuilder("TEST", "TEST"));
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	/**
	 * Configure.
	 */
	@BeforeClass
	public static void configure() {
		System.setProperty("AJSC_HOME", ".");
		System.setProperty("BUNDLECONFIG_DIR", "bundleconfig-local");
	}
	
	
	/**
	 * Uri.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Ignore
	@Test
    public void uri() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("/aai/" + loader.getVersion() + "/cloud-infrastructure/tenants/tenant/key1/vservers/vserver/key2/l-interfaces/l-interface/key3").build();
		URIToObject parse = new URIToObject(loader, uri);
		Introspector result = parse.getTopEntity();
		String expected = "{\"cloud-owner\":\"CR\",\"cloud-region-id\":\"AAI25\",\"tenants\":{\"tenant\":[{\"tenant-id\":\"key1\",\"vservers\":{\"vserver\":[{\"vserver-id\":\"key2\",\"l-interfaces\":{\"l-interface\":[{\"interface-name\":\"key3\"}]}}]}}]}}";
		String topEntity = "cloud-region";
		String entity = "l-interface";
		
		testSet(result.marshal(false), parse, expected, topEntity, entity, currentVersion);

	}
	
	/**
	 * Uri no version.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Ignore
	@Test
    public void uriNoVersion() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("/cloud-infrastructure/tenants/tenant/key1/vservers/vserver/key2/l-interfaces/l-interface/key3").build();
		HashMap<String, Introspector> relatedObjects = new HashMap<>();
		Introspector tenantObj = this.loader.introspectorFromName("tenant");
		tenantObj.setValue("tenant-id", "key1");
		tenantObj.setValue("tenant-name", "name1");
		relatedObjects.put(tenantObj.getObjectId(), tenantObj);
		Introspector vserverObj = this.loader.introspectorFromName("vserver");
		vserverObj.setValue("vserver-id", "key2");
		vserverObj.setValue("vserver-name", "name2");
		relatedObjects.put(vserverObj.getObjectId(), vserverObj);

		URIToObject parse = new URIToObject(loader, uri, relatedObjects);
		Introspector result = parse.getTopEntity();
		String expected = "{\"cloud-owner\":\"CR\",\"cloud-region-id\":\"AAI25\",\"tenants\":{\"tenant\":[{\"tenant-id\":\"key1\",\"tenant-name\":\"name1\",\"vservers\":{\"vserver\":[{\"vserver-id\":\"key2\",\"vserver-name\":\"name2\",\"l-interfaces\":{\"l-interface\":[{\"interface-name\":\"key3\"}]}}]}}]}}";
		String topEntity = "cloud-region";
		String entity = "l-interface";
		
		testSet(result.marshal(false), parse, expected, topEntity, entity, currentVersion);

		
	}
	

	/**
	 * Bad URI.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Test
    public void badURI() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("/aai/" + loader.getVersion() + "/cloud-infrastructure/tenants/tenant/key1/vservers/vserver/key2/l-interadsfaces/l-interface/key3").build();
		
		thrown.expect(AAIException.class);
		thrown.expectMessage(startsWith("AAI_3001"));
		
		URIToObject parse = new URIToObject(loader, uri);
		
	}
	
	/**
	 * Starts with valid namespace.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Ignore
	@Test
    public void startsWithValidNamespace() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("/cloud-infrastructure/tenants/tenant/key1/vservers/vserver/key2/l-interfaces/l-interface/key3").build();
		URIToObject parse = new URIToObject(loader, uri);
		Introspector result = parse.getTopEntity();
		String expected = "{\"cloud-owner\":\"CR\",\"cloud-region-id\":\"AAI25\",\"tenants\":{\"tenant\":[{\"tenant-id\":\"key1\",\"vservers\":{\"vserver\":[{\"vserver-id\":\"key2\",\"l-interfaces\":{\"l-interface\":[{\"interface-name\":\"key3\"}]}}]}}]}}";
		String topEntity = "cloud-region";
		String entity = "l-interface";
		
		testSet(result.marshal(false), parse, expected, topEntity, entity, currentVersion);

	}
	
	/**
	 * Single top level.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Test
    public void singleTopLevel() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("/network/generic-vnfs/generic-vnf/key1").build();
		URIToObject parse = new URIToObject(loader, uri);
		Introspector result = parse.getTopEntity();
		String expected = "{\"vnf-id\":\"key1\"}";
		
		String topEntity = "generic-vnf";
		String entity = "generic-vnf";
		
		testSet(result.marshal(false), parse, expected, topEntity, entity, version);

	}
	
	/**
	 * Naming exceptions.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Ignore
	@Test
    public void namingExceptions() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("network/vces/vce/key1/port-groups/port-group/key2/cvlan-tags/cvlan-tag/655").build();
		URIToObject parse = new URIToObject(loader, uri);
		Introspector result = parse.getTopEntity();
		String expected = "{\"vnf-id\":\"key1\",\"port-groups\":{\"port-group\":[{\"interface-id\":\"key2\",\"cvlan-tags\":{\"cvlan-tag-entry\":[{\"cvlan-tag\":655}]}}]}}";
		String topEntity = "vce";
		String entity = "cvlan-tag";
		
		testSet(result.marshal(false), parse, expected, topEntity, entity, version);

    }
	
	/**
	 * No list object.
	 *
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	@Ignore
	@Test
	public void noListObject() throws IllegalArgumentException, UnsupportedEncodingException, AAIException {
		URI uri = UriBuilder.fromPath("/aai/v8/network/vpls-pes/vpls-pe/0e6189fd-9257-49b9-a3be-d7ba980ccfc9/lag-interfaces/lag-interface/8ae5aa76-d597-4382-b219-04f266fe5e37/l-interfaces/l-interface/9e141d03-467b-437f-b4eb-b3133ec1e205/l3-interface-ipv4-address-list/8f19f0ea-a81f-488e-8d5c-9b7b53696c11").build();
		URIToObject parse = new URIToObject(loader, uri);
		Introspector result = parse.getTopEntity();
		String topEntity = "vpls-pe";
		String entity = "l3-interface-ipv4-address-list";
		String expected = "{\"equipment-name\":\"0e6189fd-9257-49b9-a3be-d7ba980ccfc9\",\"lag-interfaces\":{\"lag-interface\":[{\"interface-name\":\"8ae5aa76-d597-4382-b219-04f266fe5e37\",\"l-interfaces\":{\"l-interface\":[{\"interface-name\":\"9e141d03-467b-437f-b4eb-b3133ec1e205\",\"l3-interface-ipv4-address-list\":[{\"l3-interface-ipv4-address\":\"8f19f0ea-a81f-488e-8d5c-9b7b53696c11\"}]}]}}]}}";
		testSet(result.marshal(false), parse, expected, topEntity, entity, version);
		
	}
	
	/**
	 * Test set.
	 *
	 * @param json the json
	 * @param parse the parse
	 * @param expected the expected
	 * @param topEntity the top entity
	 * @param entity the entity
	 * @param version the version
	 */
	public void testSet(String json, URIToObject parse, String expected, String topEntity, String entity, Version version) {
		assertEquals("blah", expected, json);
		
		assertEquals("top entity", topEntity, parse.getTopEntityName());

		assertEquals("entity", entity, parse.getEntityName());

		assertEquals("entity object", entity, parse.getEntity().getDbName());
		
		assertEquals("parent list object", 1, parse.getParentList().size());
		
		assertEquals("object version", version, parse.getObjectVersion());
	}
}
