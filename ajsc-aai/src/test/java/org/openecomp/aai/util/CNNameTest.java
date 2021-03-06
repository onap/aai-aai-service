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

package org.openecomp.aai.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openecomp.aai.util.CNName;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.agent.PowerMockAgent;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;

import ch.qos.logback.access.spi.IAccessEvent;

@PowerMockIgnore("javax.security.auth.x500.X500Principal")
@PrepareForTest({IAccessEvent.class, HttpServletRequest.class, X509Certificate.class})
public class CNNameTest {
	
	@Rule
	public PowerMockRule rule = new PowerMockRule();
	
	static {
	     PowerMockAgent.initializeIfNeeded();
	 }
	
	
	IAccessEvent mockAccEvent;
	HttpServletRequest mockHttpServletRequest;
	CNName cnname;
	X509Certificate cert;

	/**
	 * Initialize.
	 */
	@Before
	public void initialize(){
		mockAccEvent = Mockito.mock(IAccessEvent.class);
		mockHttpServletRequest = Mockito.mock(HttpServletRequest.class);
		cert = Mockito.mock(X509Certificate.class);
	}

	
	/**
	 * Test 'convert' when there is no AccessConverter.
	 */
	@Test
	public void testConvert_withoutAccessConverter(){
		cnname = getTestObj(false);
		assertTrue("Conversion failed with no AccessConverter", "INACTIVE_HEADER_CONV".equals(cnname.convert(mockAccEvent)));
	}

	/**
	 * Test 'convert' with no CipherSuite.
	 */
	@Test
	public void testConvert_withNullCipherSuite(){
		setupForCipherSuite(null);
		assertTrue("Conversion failed for a null CipherSuite", "-".equals(cnname.convert(mockAccEvent)));
	}
	
	
	/**
	 * Test 'convert' with a non-null CipherSuite.
	 */
	@Test
	public void testConvert_withNotNullCipherSuite(){
		
		setupForCipherSuite("StrRepOfAValidSuite");

		final X500Principal principal = new X500Principal("CN=AnAI, OU=DOX, O=BWS, C=CA");
		
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		final X509Certificate[] certChain = {cert};
		
		when(mockHttpServletRequest.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(certChain);

		assertTrue("Conversion failed for a valid CipherSuite", principal.toString().equals(cnname.convert(mockAccEvent)));
	}
	
	
	/**
	 * Helper method to mock IAccessEvent and HttpServletRequest.
	 *
	 * @param suite CipherSuite to be used in current test
	 */
	private void setupForCipherSuite(String suite){
		cnname = getTestObj(true);
		when(mockAccEvent.getRequest()).thenReturn(mockHttpServletRequest);
		when(mockHttpServletRequest.getAttribute("javax.servlet.request.cipher_suite")).thenReturn(suite);
	}
	
		
	/**
	 * Helper method to create a CNName object with overridden 'start status' .
	 *
	 * @param instanceStarted Start status to be used
	 * @return CNName object to test
	 */
	private CNName getTestObj(final boolean instanceStarted){
		return new CNName(){
			@Override
			public boolean isStarted(){
				return instanceStarted;
			}
		};
	}
}



