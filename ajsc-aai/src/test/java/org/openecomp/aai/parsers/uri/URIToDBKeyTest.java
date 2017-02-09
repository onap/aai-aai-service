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

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.LoaderFactory;
import org.openecomp.aai.introspection.ModelInjestor;
import org.openecomp.aai.introspection.ModelType;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.logging.LogLineBuilder;
import org.openecomp.aai.parsers.uri.URIToDBKey;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.att.aft.dme2.internal.javaxwsrs.core.UriBuilder;

@PrepareForTest(ModelInjestor.class)
public class URIToDBKeyTest {

	private Loader loader = LoaderFactory.createLoaderForVersion(ModelType.MOXY, Version.v8, new LogLineBuilder("TEST", "TEST"));

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
		URIToDBKey parse = new URIToDBKey(loader, uri);
		Object result = parse.getResult();

		String expected = "cloud-region/CR/AAI25/tenant/key1/vserver/key2/l-interface/key3";
		
		assertEquals("blah", expected, result);
		
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
		URIToDBKey parse = new URIToDBKey(loader, uri);
		Object result = parse.getResult();
		
		String expected = "cloud-region/CR/AAI25/tenant/key1/vserver/key2/l-interface/key3";
		
		assertEquals("blah", expected, result);
		
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
		
		URIToDBKey parse = new URIToDBKey(loader, uri);
		
	}
	
	/**
	 * No valid tokens.
	 *
	 * @throws JAXBException the JAXB exception
	 * @throws AAIException the AAI exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Test
    public void noValidTokens() throws JAXBException, AAIException, IllegalArgumentException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("/aai/" + loader.getVersion() + "/cloud/blah/blah").build();
		
		thrown.expect(AAIException.class);
		thrown.expectMessage(startsWith("AAI_3001"));
		
		URIToDBKey parse = new URIToDBKey(loader, uri);
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
		URI uri = UriBuilder.fromPath("/aai/" + loader.getVersion() + "/cloud-infrastructure/tenants/tenant/key1/vservers/vserver/key2/l-interfaces/l-interface/key3").build();
		
		URIToDBKey parse = new URIToDBKey(loader, uri);
		Object result = parse.getResult();

		String expected = "cloud-region/CR/AAI25/tenant/key1/vserver/key2/l-interface/key3";
		
		assertEquals("blah", expected, result);
	}
	
	/**
	 * Naming exceptions.
	 *
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws AAIException the AAI exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Ignore
	@Test
    public void namingExceptions() throws IllegalArgumentException, AAIException, UnsupportedEncodingException {
		URI uri = UriBuilder.fromPath("network/vces/vce/key1/port-groups/port-group/key2/cvlan-tags/cvlan-tag/655").build();
		URIToDBKey parse = new URIToDBKey(loader, uri);
		Object result = parse.getResult();

		String expected = "vce/key1/port-group/key2/cvlan-tag/655";
		
		assertEquals("blah", expected, result);
		
    }
		
}
