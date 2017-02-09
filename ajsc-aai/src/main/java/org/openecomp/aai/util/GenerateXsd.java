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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openecomp.aai.dbmodel.DbEdgeRules;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.google.common.base.Joiner;


public class GenerateXsd {
	static String apiVersion = null;
	static String apiVersionFmt = null;
	static String responsesUrl = null;
	static String responsesLabel = null;
	static Map<String, String> generatedJavaType = new HashMap<String, String>();
	static Map<String, String> appliedPaths = new HashMap<String, String>();
	static NodeList javaTypeNodes;

	
	public static final int VALUE_NONE = 0;
	public static final int VALUE_DESCRIPTION = 1;
	public static final int VALUE_INDEXED_PROPS = 2;
	
	private static XPath xpath = XPathFactory.newInstance().newXPath();

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		if (args.length > 0) { 
			if (args[0] != null) {
				apiVersion = args[0];
			}
		}
		boolean genDoc = false;
		if ( args.length > 1 ) {
			genDoc = true;
			int index = args[1].indexOf("|");
			if ( index > 0 ) {
				responsesUrl = args[1].substring(0, index);
				responsesLabel = args[1].substring(index+1);
				//System.out.println( "response URL " + responsesUrl);
				//System.out.println( "response label " + responsesLabel);
				responsesUrl = "description: "+ responsesLabel + "(" +
						responsesUrl + ").\n";
				//System.out.println( "operation described with " + responsesUrl);
			}
		}
		String oxmPath = null;
		if ( apiVersion == null || genDoc ) { 
			// to run from eclipse, set these env, e.g. v7, \sources\aai\aaimastergit\bundleconfig-local\etc\oxm\
			String envRev= System.getenv("OXM_REV");
			if ( envRev != null )
				apiVersion = envRev;
			
		}
		oxmPath = System.getenv("OXM_PATH");	
		String outfilePath = System.getenv("OXM_OUTFILEPATH");
		String outfileName = null;
		if ( outfilePath == null ) {
			if ( genDoc ) {
				outfileName = "c:\\temp\\aai.yaml";
			} else
				outfileName = "c:\\temp\\aai_schema.xsd";
		} else {
			if ( genDoc ) {
				outfileName = outfilePath + "aai.yaml";
			} else
				outfileName = outfilePath + "aai_schema.xsd";
		}
		if ( apiVersion != null ) { // generate from oxm
			apiVersionFmt = "." + apiVersion + ".";
			if ( oxmPath == null ) {
				oxmPath = AAIConstants.AAI_HOME_ETC_OXM + AAIConstants.AAI_FILESEP;
				//oxmPath = "\\sources\\aai\\aaimastergit\\bundleconfig-local\\etc\\oxm\\";
			}
			File oxm_file = new File(oxmPath + "aai_oxm_" + apiVersion + ".xml");
			String xsd;
			File outfile;
			if ( genDoc ) {
				xsd = generateSwaggerFromOxmFile( oxm_file);
				outfile =new File(outfileName);				
			} else {
				xsd = processOxmFile( oxm_file);
				outfile =new File(outfileName);
			}
			
			
		    try {
		        outfile.createNewFile();
		    } catch (IOException e) {
	        	System.out.println( "Exception creating output file " + outfileName);
	        	e.printStackTrace();
		    }
	        try {
	        	FileWriter fw = new FileWriter(outfile.getAbsoluteFile());
	        	BufferedWriter bw = new BufferedWriter(fw);
	        	bw.write(xsd);
	        	bw.close();

	        } catch ( IOException e) {
	        	System.out.println( "Exception writing output file " + outfileName);
	        	e.printStackTrace();
	        }
			System.out.println( "GeneratedXSD successful, saved in " + outfileName);
			return;
		}
		
		JAXBContext jaxbContext = null;
		try {
			jaxbContext = JAXBContext.newInstance(org.openecomp.aai.domain.yang.GenericVnf.class);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SchemaOutputResolver sor = new MySchemaOutputResolver();
		jaxbContext.generateSchema(sor);
		
	}

	public static class MySchemaOutputResolver extends SchemaOutputResolver {

		public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
			File file = new File("c:\\temp\\aai_schema.xsd");
			StreamResult result = new StreamResult(file);
			result.setSystemId(file.toURI().toURL().toString());
			return result;
		}

	}
	
	public static String processJavaTypeElement( String javaTypeName, Element javaTypeElement) {
		
		String xmlRootElementName = null;

		Map<String, String> addJavaType = new HashMap<String, String>();

		
		NodeList parentNodes = javaTypeElement.getElementsByTagName("java-attributes");
		StringBuffer sb = new StringBuffer();
		if ( parentNodes.getLength() == 0 ) {
			System.out.println( "no java-attributes for java-type " + javaTypeName);
			return "";

		}
		
		NamedNodeMap attributes;
		
		NodeList valNodes = javaTypeElement.getElementsByTagName("xml-root-element");
		Element valElement = (Element) valNodes.item(0);
		attributes = valElement.getAttributes();
		for ( int i = 0; i < attributes.getLength(); ++i ) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getNodeName();

            String attrValue = attr.getNodeValue();
            //System.out.println("Found xml-root-element attribute: " + attrName + " with value: " + attrValue);
            if ( attrName.equals("name"))
            	xmlRootElementName = attrValue;
		}
		/*
		if ( javaTypeName.equals("RelationshipList")) {
			System.out.println( "Skipping " + javaTypeName);
			generatedJavaType.put(javaTypeName, null);
			return "";
		}
		*/
		
		Element parentElement = (Element)parentNodes.item(0);
		NodeList xmlElementNodes = parentElement.getElementsByTagName("xml-element");
		NodeList childNodes;
		Element childElement;
		String xmlElementWrapper;

		Element xmlElementElement;
		String addType;
		String elementName, elementType, elementIsKey, elementIsRequired, elementContainerType;
		StringBuffer sb1 = new StringBuffer();
		if ( xmlElementNodes.getLength() > 0 ) {
			sb1.append("  <xs:element name=\"" + xmlRootElementName + "\">\n");
			sb1.append("    <xs:complexType>\n");
			NodeList properties = GenerateXsd.locateXmlProperties(javaTypeElement);
			if (properties != null) {
				System.out.println("properties found for: " + xmlRootElementName);
				sb1.append("      <xs:annotation>\r\n");
				insertAnnotation(properties, false, "class", sb1, "      ");
				
				sb1.append("      </xs:annotation>\r\n");
			} else {
				System.out.println("no properties found for: " + xmlRootElementName);
			}
			sb1.append("      <xs:sequence>\n");
			for ( int i = 0; i < xmlElementNodes.getLength(); ++i ) {
				
				xmlElementElement = (Element)xmlElementNodes.item(i);
				childNodes = xmlElementElement.getElementsByTagName("xml-element-wrapper");
				
				xmlElementWrapper = null;
				if ( childNodes.getLength() > 0 ) {
					childElement = (Element)childNodes.item(0);
					// get name
					attributes = childElement.getAttributes();
					for ( int k = 0; k < attributes.getLength(); ++k ) {
						Attr attr = (Attr) attributes.item(k);
						String attrName = attr.getNodeName();
						String attrValue = attr.getNodeValue();
						if ( attrName.equals("name")) {
							xmlElementWrapper = attrValue;
							//System.out.println("found xml-element-wrapper " + xmlElementWrapper);
						}
					}

				}
				attributes = xmlElementElement.getAttributes();
				addType = null;
	
	 
				elementName = elementType = elementIsKey = elementIsRequired = elementContainerType = null;
				for ( int j = 0; j < attributes.getLength(); ++j ) {
		            Attr attr = (Attr) attributes.item(j);
		            String attrName = attr.getNodeName();
	
		            String attrValue = attr.getNodeValue();
		            //System.out.println("For " + xmlRootElementName + " Found xml-element attribute: " + attrName + " with value: " + attrValue);
		            if ( attrName.equals("name")) {
		            	elementName = attrValue;
		            }
		            if ( attrName.equals("type")) {
		            	elementType = attrValue;
		            	if ( attrValue.contains(apiVersionFmt) ) {
		            		addType = attrValue.substring(attrValue.lastIndexOf('.')+1);
		            		if ( !generatedJavaType.containsKey(addType) ) {
		            			generatedJavaType.put(addType, attrValue);
		            			sb.append(processJavaTypeElement( addType, getJavaTypeElement(addType) ));
		            			
		            		}
		            	}
		            		
		            }
		            if ( attrName.equals("xml-key")) {
		            	elementIsKey = attrValue;
		            }
		            if ( attrName.equals("required")) {
		            	elementIsRequired = attrValue;
		            }
		            if ( attrName.equals("container-type")) {
		            	elementContainerType = attrValue;
		            }	
				}
	
				if ( xmlElementWrapper != null ) {
					sb1.append("        <xs:element name=\"" + xmlElementWrapper +"\"");
					if ( elementIsRequired == null || !elementIsRequired.equals("true")||addType != null) {	
						sb1.append(" minOccurs=\"0\"");	
					} 
					sb1.append(">\n");
					sb1.append("          <xs:complexType>\n");
					properties = GenerateXsd.locateXmlProperties(javaTypeElement);
					if (properties != null) {
						sb1.append("            <xs:annotation>\r\n");
						insertAnnotation(properties, false, "class", sb1, "            ");
						sb1.append("            </xs:annotation>\r\n");
					} else {
						System.out.println("no properties found for: " + xmlElementWrapper);
					}
					sb1.append("            <xs:sequence>\n");
					sb1.append("      ");
				}
				if ( addType != null ) {
					//sb1.append("        <xs:element ref=\"tns:" + elementName +"\"");
					sb1.append("        <xs:element ref=\"tns:" + getXmlRootElementName(addType) +"\"");
				} else {
					sb1.append("        <xs:element name=\"" + elementName +"\"");
				}
				if ( elementType.equals("java.lang.String"))
					sb1.append(" type=\"xs:string\"");
				//if ( elementType.equals("java.lang.String"))
					//sb1.append(" type=\"xs:string\"");
				if ( elementType.equals("java.lang.Long"))
					sb1.append(" type=\"xs:unsignedInt\"");
				if ( elementType.equals("java.lang.Integer"))
					sb1.append(" type=\"xs:int\"");
				if ( elementType.equals("java.lang.Boolean"))
					sb1.append(" type=\"xs:boolean\"");
				//if ( elementIsRequired != null && elementIsRequired.equals("true")||addType != null) {
				if ( elementIsRequired == null || !elementIsRequired.equals("true")||addType != null) {	
					sb1.append(" minOccurs=\"0\"");
				} 
				if ( elementContainerType != null && elementContainerType.equals("java.util.ArrayList")) {
					sb1.append(" maxOccurs=\"unbounded\"");
				}
				properties = GenerateXsd.locateXmlProperties(xmlElementElement);
				if (properties != null || elementIsKey != null) {
					sb1.append(">\n");
					sb1.append("          <xs:annotation>\r\n");
					insertAnnotation(properties, elementIsKey != null, "field", sb1, "          ");
					sb1.append("          </xs:annotation>\r\n");

					if (xmlElementWrapper== null) {
						sb1.append("        </xs:element>\n");
					}
				} else {
					sb1.append("/>\n");
				}
				if ( xmlElementWrapper != null ) {
					sb1.append("            </xs:sequence>\n");
					sb1.append("          </xs:complexType>\n");
					sb1.append("        </xs:element>\n");
				}
			}
			/*
		if (  xmlRootElementName.equals("notify") ||
			xmlRootElementName.equals("relationship") ||
			xmlRootElementName.equals("relationship-data") ||
			xmlRootElementName.equals("related-to-property") )
			
			sb1.append("        <xs:any namespace=\"##other\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n");
		*/	
		sb1.append("      </xs:sequence>\n");
		sb1.append("    </xs:complexType>\n");
		sb1.append("  </xs:element>\n");
		}
		/*
		NodeList valNodes = javaTypeElement.getElementsByTagName("xml-root-element");
		Element valElement = (Element) valNodes.item(0);
		attributes = valElement.getAttributes();
		for ( int i = 0; i < attributes.getLength(); ++i ) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getNodeName();

            String attrValue = attr.getNodeValue();
            System.out.println("Found xml-root-element attribute: " + attrName + " with value: " + attrValue);
            if ( attrValue.equals("name"))
            	xmlRootElementName = attrValue;
		}
		*/
		
		if ( xmlElementNodes.getLength() < 1 ) {
			sb.append("  <xs:element name=\"" + xmlRootElementName + "\">\n");
			sb.append("    <xs:complexType>\n");
			sb.append("      <xs:sequence/>\n");
			sb.append("    </xs:complexType>\n");
			sb.append("  </xs:element>\n");
			generatedJavaType.put(javaTypeName, null);
			return sb.toString();			
		}
		
		sb.append( sb1 );

		return sb.toString();
	}
	
	private static void insertAnnotation(NodeList items, boolean isKey, String target, StringBuffer sb1, String indentation) {
		if (items != null || isKey) {
			List<String> metadata = new ArrayList<>();
			
			String name = "";
			String value = "";
			Element item = null;
			if (isKey) {
				metadata.add("isKey=true");
			}
			if (items != null) {
				for (int i = 0; i < items.getLength(); i++) {
					item = (Element)items.item(i);
					name = item.getAttribute("name");
					value = item.getAttribute("value");
					if (name.equals("abstract")) {
						name = "isAbstract";
					} else if (name.equals("extends")) {
						name = "extendsFrom";
					}
					metadata.add(name + "=\"" + value.replaceAll("&",  "&amp;") + "\"");
					System.out.println("property name: " + name);
	
				}
			}
			sb1.append(
					indentation + "  <xs:appinfo>\r\n" + 
							indentation + "    <annox:annotate target=\""+target+"\">@org.openecomp.aai.annotations.Metadata(" + Joiner.on(",").join(metadata) + ")</annox:annotate>\r\n" + 
							indentation + "  </xs:appinfo>\r\n");
		}

	}

	private static Element getJavaTypeElement( String javaTypeName )
	{
		
		String attrName, attrValue;
		Attr attr;
		Element javaTypeElement;
		for ( int i = 0; i < javaTypeNodes.getLength(); ++ i ) {
			javaTypeElement = (Element) javaTypeNodes.item(i);
			NamedNodeMap attributes = javaTypeElement.getAttributes();
			for ( int j = 0; j < attributes.getLength(); ++j ) {
	            attr = (Attr) attributes.item(j);
	            attrName = attr.getNodeName();
	            attrValue = attr.getNodeValue();
	            if ( attrName.equals("name") && attrValue.equals(javaTypeName))
	            	return javaTypeElement;
			}
		}
		System.out.println( "oxm file format error, missing java-type " + javaTypeName);
		return (Element) null;
	}
	
	private static Element getJavaTypeElementSwagger( String javaTypeName )
	{
		
		String attrName, attrValue;
		Attr attr;
		Element javaTypeElement;
		for ( int i = 0; i < javaTypeNodes.getLength(); ++ i ) {
			javaTypeElement = (Element) javaTypeNodes.item(i);
			NamedNodeMap attributes = javaTypeElement.getAttributes();
			for ( int j = 0; j < attributes.getLength(); ++j ) {
	            attr = (Attr) attributes.item(j);
	            attrName = attr.getNodeName();
	            attrValue = attr.getNodeValue();
	            if ( attrName.equals("name") && attrValue.equals(javaTypeName))
	            	return javaTypeElement;
			}
		}
		System.out.println( "oxm file format error, missing java-type " + javaTypeName);
		return (Element) null;
	}
	private static String getXmlRootElementName( String javaTypeName )
	{
		
		String attrName, attrValue;
		Attr attr;
		Element javaTypeElement;
		for ( int i = 0; i < javaTypeNodes.getLength(); ++ i ) {
			javaTypeElement = (Element) javaTypeNodes.item(i);
			NamedNodeMap attributes = javaTypeElement.getAttributes();
			for ( int j = 0; j < attributes.getLength(); ++j ) {
	            attr = (Attr) attributes.item(j);
	            attrName = attr.getNodeName();
	            attrValue = attr.getNodeValue();
	            if ( attrName.equals("name") && attrValue.equals(javaTypeName)) {
	        		NodeList valNodes = javaTypeElement.getElementsByTagName("xml-root-element");
	        		Element valElement = (Element) valNodes.item(0);
	        		attributes = valElement.getAttributes();
	        		for ( int k = 0; k < attributes.getLength(); ++k ) {
	                    attr = (Attr) attributes.item(k);
	                    attrName = attr.getNodeName();

	                    attrValue = attr.getNodeValue();
	                    //System.out.println("Found xml-root-element attribute: " + attrName + " with value: " + attrValue);
	                    if ( attrName.equals("name"))
	                    	return (attrValue);
	        		}
	            }
			}
		}
		System.out.println( "oxm file format error, missing java-type " + javaTypeName);
		return null;
	}	
	
	
	public static String processOxmFile( File oxmFile )
	{
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
		sb.append("<xs:schema elementFormDefault=\"qualified\" version=\"1.0\" targetNamespace=\"http://org.openecomp.aai.inventory/" 
				+ apiVersion + "\" xmlns:tns=\"http://org.openecomp.aai.inventory/" + apiVersion + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
						+ "\n"
						+ "xmlns:jaxb=\"http://java.sun.com/xml/ns/jaxb\"\r\n" + 
						"    jaxb:version=\"2.1\" \r\n" + 
						"    xmlns:annox=\"http://annox.dev.java.net\" \r\n" + 
						"    jaxb:extensionBindingPrefixes=\"annox\">\n\n");

		try {
		    
		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    Document doc = dBuilder.parse(oxmFile);

		    NodeList bindingsNodes = doc.getElementsByTagName("xml-bindings");
			Element bindingElement;
			NodeList javaTypesNodes;
			Element javaTypesElement;
			
			Element javaTypeElement;

			
			if ( bindingsNodes == null || bindingsNodes.getLength() == 0 ) {
				System.out.println( "missing <binding-nodes> in " + oxmFile );
				return null;
			}	    
			
			bindingElement = (Element) bindingsNodes.item(0);
			javaTypesNodes = bindingElement.getElementsByTagName("java-types");
			if ( javaTypesNodes.getLength() < 1 ) {
				System.out.println( "missing <binding-nodes><java-types> in " + oxmFile );
				return null;
			}
			javaTypesElement = (Element) javaTypesNodes.item(0);
			javaTypeNodes = javaTypesElement.getElementsByTagName("java-type");
			if ( javaTypeNodes.getLength() < 1 ) {
				System.out.println( "missing <binding-nodes><java-types><java-type> in " + oxmFile );
				return null;
			}

			String javaTypeName;
			String attrName, attrValue;
			Attr attr;
			for ( int i = 0; i < javaTypeNodes.getLength(); ++ i ) {
				javaTypeElement = (Element) javaTypeNodes.item(i);
				NamedNodeMap attributes = javaTypeElement.getAttributes();
				javaTypeName = null;
				for ( int j = 0; j < attributes.getLength(); ++j ) {
		            attr = (Attr) attributes.item(j);
		            attrName = attr.getNodeName();
		            attrValue = attr.getNodeValue();
		            if ( attrName.equals("name"))
		            	javaTypeName = attrValue;
				}
				if ( javaTypeName == null ) {
					System.out.println( "<java-type> has no name attribute in " + oxmFile );
					return null;
				}
				if ( !generatedJavaType.containsKey(javaTypeName) ) {
					generatedJavaType.put(javaTypeName, null);
					sb.append(processJavaTypeElement( javaTypeName, javaTypeElement ));
				}
			}
				
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		sb.append("</xs:schema>\n");
		return sb.toString();
	}

	private static boolean isStandardType( String elementType )
	{
		switch ( elementType ) {
		case "java.lang.String":
		case "java.lang.Long":
		case "java.lang.Integer":
		case"java.lang.Boolean":
			return true;
		}
		return false;
	}
	
	private static Vector<String> getIndexedProps( String attrValue )
	{
		if ( attrValue == null )
			return null;
		StringTokenizer st = new StringTokenizer( attrValue, ",");
		if ( st.countTokens() ==  0 )
			return null;
		Vector<String> result = new Vector<String>();
		while ( st.hasMoreTokens()) {
			result.add(st.nextToken());
		}
		return result;
	}
	
	static private Vector<String> getEdgeRules( String nodeName ) 
	{
		
		Vector<String> result = new Vector<String>();
		Iterator<String> edgeRulesIterator;
		edgeRulesIterator = org.openecomp.aai.dbmodel.DbEdgeRules.EdgeRules.keySet().iterator();
		
		while( edgeRulesIterator.hasNext() ){
	    	String ruleKey = edgeRulesIterator.next();
	    	if ( ruleKey.startsWith(nodeName + "|" ) ||
	    			ruleKey.endsWith("|" + nodeName)) {
	    		Collection <String> edRuleColl = DbEdgeRules.EdgeRules.get(ruleKey);
	    		Iterator <String> ruleItr = edRuleColl.iterator();
	    		if( ruleItr.hasNext() ){		
	    			//String fullRuleString = ruleItr.next();
	    			//System.out.println( "nodeName " + nodeName + " ruleKey "  + ruleKey + " ruleString " + fullRuleString);
	    			//result.add(ruleKey + "-" + fullRuleString);
	    			result.add(ruleKey );
	    		}
			}
		}
		return result;
	}
	
	public static String processJavaTypeElementSwagger( String javaTypeName, Element javaTypeElement, 
			StringBuffer pathSb, StringBuffer definitionsSb, String path, String tag, String opId,
			String getItemName, StringBuffer pathParams, String queryParams, String validEdges) {
		
		String xmlRootElementName = null;

		//Map<String, String> addJavaType = new HashMap<String, String>();
		String useTag = null;
		String useOpId = null;
		
		if ( tag != null ) {
			switch ( tag ) {
			case "Network":
			case "ServiceDesignAndCreation":
			case "Business":
			case "CloudInfrastructure":
				break;
			default:
				return null;
			}
		}
		/*
		if ( path == null )
			System.out.println( "processJavaTypeElementSwagger called with null path for javaTypeName " + javaTypeName);
		*/
		/*
		if ( path == null || !(path.contains("cloud-infrastructure")))
			switch ( javaTypeName) {
			 case "Inventory":
				useTag = null;
				break;
				
			case "CloudInfrastructure":
			case "Search":
			case "Actions":
			case "ServiceDesignAndCreation":
			case "Network":
				if ( tag == null )
					useTag = javaTypeName;

				break;
			default:
					return null;
					
			}
		*/
		
		if ( !javaTypeName.equals("Inventory") ) {
			if ( javaTypeName.equals("AaiInternal"))
				return null;
			if ( opId == null )
				useOpId = javaTypeName;
			else
				useOpId = opId + javaTypeName;
			if ( tag == null )
				useTag = javaTypeName;
		}
		
		/*
		if ( javaTypeName.equals("GenericVnf"))
			System.out.println( "Processing " + javaTypeName);
		else if ( javaTypeName.equals("Service"))
				System.out.println( "Processing " + javaTypeName);
		else if ( javaTypeName.equals("SitePair"))
			System.out.println( "Processing " + javaTypeName);
		*/
		NodeList parentNodes = javaTypeElement.getElementsByTagName("java-attributes");
		StringBuffer sb = new StringBuffer();
		if ( parentNodes.getLength() == 0 ) {
			System.out.println( "no java-attributes for java-type " + javaTypeName);
			return "";

		}
		
		NamedNodeMap attributes;
		
		NodeList valNodes = javaTypeElement.getElementsByTagName("xml-root-element");
		Element valElement = (Element) valNodes.item(0);
		attributes = valElement.getAttributes();
		for ( int i = 0; i < attributes.getLength(); ++i ) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getNodeName();

            String attrValue = attr.getNodeValue();
            //System.out.println("Found xml-root-element attribute: " + attrName + " with value: " + attrValue);
            if ( attrName.equals("name"))
            	xmlRootElementName = attrValue;
		}
		/*
		if ( xmlRootElementName.equals("oam-networks"))
			System.out.println( "xmlRootElement oam-networks with getItemData [" + getItemName + "]");
		*/
		//already processed
		/*
		if ( generatedJavaType.containsKey(xmlRootElementName) ) {
			return null;
		}
		*/
		NodeList childNodes;
		Element childElement;
		NodeList xmlPropNodes = javaTypeElement.getElementsByTagName("xml-properties");
		Element xmlPropElement;
		String pathDescriptionProperty = null;
		
		
		Vector<String> indexedProps = null;
		
		/*System.out.println( "javaTypeName " + javaTypeName + " has xml-properties length " + xmlPropNodes.getLength());
		if ( path != null && path.equals("/network/generic-vnfs"))
			System.out.println("path is " + "/network/generic-vnfs with getItemName " + getItemName);
		*/
		if ( xmlPropNodes.getLength() > 0 ) {

			for ( int i = 0; i < xmlPropNodes.getLength(); ++i ) {
				xmlPropElement = (Element)xmlPropNodes.item(i);
				if ( !xmlPropElement.getParentNode().isSameNode(javaTypeElement))
					continue;
				childNodes = xmlPropElement.getElementsByTagName("xml-property");
				
				if ( childNodes.getLength() > 0 ) {
					for ( int j = 0; j < childNodes.getLength(); ++j ) {
						childElement = (Element)childNodes.item(j);
						// get name
						int useValue = VALUE_NONE;
						attributes = childElement.getAttributes();
						for ( int k = 0; k < attributes.getLength(); ++k ) {
							Attr attr = (Attr) attributes.item(k);
							String attrName = attr.getNodeName();
							String attrValue = attr.getNodeValue();
							if ( attrName == null || attrValue == null )
								continue;
							if ( attrName.equals("name") && attrValue.equals("description")) {
								useValue = VALUE_DESCRIPTION;
							}
							if ( useValue == VALUE_DESCRIPTION && attrName.equals("value")) {
								pathDescriptionProperty = attrValue;
								//break;
								//System.out.println("found xml-element-wrapper " + xmlElementWrapper);
							}
							if ( attrValue.equals("indexedProps")) {
								useValue = VALUE_INDEXED_PROPS;
							}
							if ( useValue == VALUE_INDEXED_PROPS && attrName.equals("value")) {
								indexedProps = getIndexedProps( attrValue );
							}
						}
					}
				}
			}
		}
		//System.out.println("javaTypeName " + javaTypeName + " description " + pathDescriptionProperty);

		/*
		if ( javaTypeName.equals("RelationshipList")) {
			System.out.println( "Skipping " + javaTypeName);
			generatedJavaType.put(javaTypeName, null);
			return "";
		}
		*/
		
		Element parentElement = (Element)parentNodes.item(0);
		NodeList xmlElementNodes = parentElement.getElementsByTagName("xml-element");

		String xmlElementWrapper;
		String attrDescription = null;

		Element xmlElementElement;
		String addType = null;
		String elementType = null, elementIsKey = null, elementIsRequired, elementContainerType = null;
		String elementName = null;
		StringBuffer sbParameters = new StringBuffer();

		StringBuffer sbRequired = new StringBuffer();
		int requiredCnt = 0;
		int propertyCnt = 0;
		StringBuffer sbProperties = new StringBuffer();
		StringBuffer sbIndexedParams = new StringBuffer();

		xmlElementWrapper = null;
		StringTokenizer st;
		if ( xmlRootElementName.equals("inventory"))
			path = "";
		else if ( path == null )
			//path = "/aai/" + apiVersion;
			path = "/" + xmlRootElementName;
		else
			path += "/" + xmlRootElementName;
		st = new StringTokenizer(path, "/");
		/*
		if ( path.equals("/business/customers/customer/{global-customer-id}/service-subscriptions/service-subscription"))
			System.out.println("processing path /business/customers/customer/{global-customer-id}/service-subscriptions with tag " + tag);
		*/
		boolean genPath = false;
		/*
		if ( path != null && path.equals("/network/generic-vnfs/generic-vnf"))
			System.out.println("path is " + "/network/generic-vnfs/generic-vnf");
		*/
		if ( st.countTokens() > 1 && getItemName == null ) {
			if ( appliedPaths.containsKey(path)) 
				return null;
			appliedPaths.put(path, null);
			genPath = true;
			if ( path.contains("/relationship/") ) { // filter paths with relationship-list
				genPath = false;
			}
			if ( path.endsWith("/relationship-list")) {
				genPath = false;
			}
					
		}
		
		Vector<String> addTypeV = null;
		if (  xmlElementNodes.getLength() > 0 ) {
			boolean hasKey = false;
			
			for ( int i = 0; i < xmlElementNodes.getLength(); ++i ) {
				xmlElementElement = (Element)xmlElementNodes.item(i);
				if ( !xmlElementElement.getParentNode().isSameNode(parentElement))
					continue;
				/*childNodes = xmlElementElement.getElementsByTagName("xml-element-wrapper");
				if ( childNodes.getLength() > 0 ) {
					childElement = (Element)childNodes.item(0);
					// get name
					attributes = childElement.getAttributes();
					for ( int k = 0; k < attributes.getLength(); ++k ) {
						Attr attr = (Attr) attributes.item(k);
						String attrName = attr.getNodeName();
						String attrValue = attr.getNodeValue();
						if ( attrName.equals("name")) {
							xmlElementWrapper = attrValue;
							//System.out.println("found xml-element-wrapper " + xmlElementWrapper);
						}
					}

				}
				*/
				valNodes = xmlElementElement.getElementsByTagName("xml-properties");
				attrDescription = null;
				if ( valNodes.getLength() > 0 ) {
					for ( int j = 0; j < valNodes.getLength(); ++j ) {
						valElement = (Element)valNodes.item(j);
						if ( !valElement.getParentNode().isSameNode(xmlElementElement))
							continue;
						childNodes = valElement.getElementsByTagName("xml-property");
						if ( childNodes.getLength() > 0 ) {
							childElement = (Element)childNodes.item(0);
							// get name
							attributes = childElement.getAttributes();
							attrDescription = null;
							boolean useValue = false;
							for ( int k = 0; k < attributes.getLength(); ++k ) {
								Attr attr = (Attr) attributes.item(k);
								String attrName = attr.getNodeName();
								String attrValue = attr.getNodeValue();
								if ( attrName.equals("name") && attrValue.equals("description")) {
									useValue = true;
								}
								if ( useValue && attrName.equals("value")) {
									attrDescription = attrValue;
									//System.out.println("found xml-element-wrapper " + xmlElementWrapper);
								}
							}

						}
					}
				}
				
				attributes = xmlElementElement.getAttributes();
				addTypeV = null; // vector of 1
				addType = null;
				
				elementName = elementType = elementIsKey = elementIsRequired = elementContainerType = null;
				for ( int j = 0; j < attributes.getLength(); ++j ) {
		            Attr attr = (Attr) attributes.item(j);
		            String attrName = attr.getNodeName();
	
		            String attrValue = attr.getNodeValue();
		            //System.out.println("For " + xmlRootElementName + " Found xml-element attribute: " + attrName + " with value: " + attrValue);
		            if ( attrName.equals("name")) {
		            	elementName = attrValue;

		            }
		            if ( attrName.equals("type") && getItemName == null ) {
		            	elementType = attrValue;
		            	if ( attrValue.contains(apiVersionFmt) ) {
		            		addType = attrValue.substring(attrValue.lastIndexOf('.')+1);
		            		if ( addTypeV == null ) 
		            			addTypeV = new Vector<String>();
		            		addTypeV.add(addType);
		            	}
		            		
		            }
		            if ( attrName.equals("xml-key")) {
		            	elementIsKey = attrValue;
		            	path += "/{" + elementName + "}";
		            }
		            if ( attrName.equals("required")) {
		            	elementIsRequired = attrValue;
		            }
		            if ( attrName.equals("container-type")) {
		            	elementContainerType = attrValue;
		            }	
				}
            	if ( getItemName != null ) {
            		if ( getItemName.equals("array") ) {
            			if ( elementContainerType != null && elementContainerType.equals("java.util.ArrayList")) {
            				//System.out.println( " returning array " + elementName );
            				return elementName;
            			}
            			
            		} else { // not an array check
            			if ( elementContainerType == null || !elementContainerType.equals("java.util.ArrayList")) {
            				//System.out.println( " returning object " + elementName );
            				return elementName;
            			}
            			
            		}
            		//System.out.println( " returning null" );
            		return null;
            	}
				if ( elementIsRequired != null ) {
					if ( requiredCnt == 0 )
						sbRequired.append("    required:\n");
					++requiredCnt;
					if ( addTypeV != null ) {
						for ( int k = 0; k < addTypeV.size(); ++i ) {
							sbRequired.append("    - " + getXmlRootElementName(addTypeV.elementAt(k)) + ":\n");
						}
					} else 
						sbRequired.append("    - " + elementName + "\n");

				}

				if (  elementIsKey != null )  {
					hasKey = true;


					sbParameters.append(("        - name: " + elementName + "\n"));
					sbParameters.append(("          in: path\n"));
					if ( attrDescription != null && attrDescription.length() > 0 )
						sbParameters.append(("          description: " + attrDescription + "\n"));
					sbParameters.append(("          required: true\n"));
					if ( elementType.equals("java.lang.String"))
						sbParameters.append("          type: string\n");
					if ( elementType.equals("java.lang.Long")) {
						sbParameters.append("          type: integer\n");
						sbParameters.append("          format: int64\n");
					}
					if ( elementType.equals("java.lang.Integer")) {
						sbParameters.append("          type: integer\n");
						sbParameters.append("          format: int32\n");
					}
					if ( elementType.equals("java.lang.Boolean"))
						sbParameters.append("          type: boolean\n");


				} else if (  indexedProps != null
						&& indexedProps.contains(elementName ) ) {
					sbIndexedParams.append(("        - name: " + elementName + "\n"));
					sbIndexedParams.append(("          in: query\n"));
					if ( attrDescription != null && attrDescription.length() > 0 )
						sbIndexedParams.append(("          description: " + attrDescription + "\n"));
					sbIndexedParams.append(("          required: false\n"));
					if ( elementType.equals("java.lang.String"))
						sbIndexedParams.append("          type: string\n");
					if ( elementType.equals("java.lang.Long")) {
						sbIndexedParams.append("          type: integer\n");
						sbIndexedParams.append("          format: int64\n");
					}
					if ( elementType.equals("java.lang.Integer")) {
						sbIndexedParams.append("          type: integer\n");
						sbIndexedParams.append("          format: int32\n");
					}
					if ( elementType.equals("java.lang.Boolean"))
						sbIndexedParams.append("          type: boolean\n");
				}

			/*
			if ( elementName != null && elementName.equals("inventory-item"))
				System.out.println( "processing inventory-item elementName");
			*/
		
			if ( isStandardType(elementType)) {
				sbProperties.append("      " + elementName + ":\n");
				++propertyCnt;
				sbProperties.append("        type: ");

				if ( elementType.equals("java.lang.String"))
					sbProperties.append("string\n");
				else if ( elementType.equals("java.lang.Long")) {
					sbProperties.append("integer\n");
					sbProperties.append("        format: int64\n");
				}
				else if ( elementType.equals("java.lang.Integer")){
					sbProperties.append("integer\n");
					sbProperties.append("        format: int32\n");
				}
				else if ( elementType.equals("java.lang.Boolean"))
					sbProperties.append("boolean\n");
				if ( attrDescription != null && attrDescription.length() > 0 )
					sbProperties.append("        description: " + attrDescription + "\n");
			}

			//if ( addType != null && elementContainerType != null && elementContainerType.equals("java.util.ArrayList") ) {
			boolean addingAaiInternal = false;
	        if ( addTypeV !=  null ) {
	    		StringBuffer newPathParams = null;
	    		if ( pathParams != null  ) {
	    			newPathParams = new StringBuffer();
	    			newPathParams.append(pathParams);
	    		}
	            if ( sbParameters.toString().length() > 0 ) {
					if ( newPathParams == null )
						newPathParams = new StringBuffer();
					newPathParams.append(sbParameters);
	            }
	            String newQueryParams = null;
	            if ( sbIndexedParams.toString().length() > 0 ) {
	            	if ( queryParams == null )
	            		newQueryParams = sbIndexedParams.toString();
	            	else
	            		newQueryParams = queryParams + sbIndexedParams.toString();
	            } else {
	            	newQueryParams = queryParams;
	            }
	        	for ( int k = 0; k < addTypeV.size(); ++k ) {
	        		addType = addTypeV.elementAt(k);
	        
	        		if ( opId == null || !opId.contains(addType)) {
	        			/*
	        			if ( validEdges == null ) {
	        			*/
	        				if ( addType.equals("RelationshipList") ) {
	        					// get edge rule
	        					Vector<String> edges = getEdgeRules(xmlRootElementName );
	        					if ( edges.size() > 0 ) {
		        					StringBuffer sbEdge = new StringBuffer();
		        					sbEdge.append("      description: |\n");				
		        					sbEdge.append("        ###### related-to\n");
		        					for ( int xx = 0; xx < edges.size(); ++ xx) {
		        						if ( edges.elementAt(xx).startsWith(xmlRootElementName))
		        						sbEdge.append("        - TO " + edges.elementAt(xx).substring(edges.elementAt(xx).indexOf("|")+1) + "\n");
		        					}
		        					for ( int xx = 0; xx < edges.size(); ++ xx) {
		        						if ( edges.elementAt(xx).endsWith(xmlRootElementName))
		        						sbEdge.append("        - FROM " + edges.elementAt(xx).substring(0, edges.elementAt(xx).indexOf("|")) + "\n");
		        					}
		        					validEdges = sbEdge.toString();
	        					}
	        					
	        				}
	        				/*
	        			}
	        			*/
	        			processJavaTypeElementSwagger( addType, getJavaTypeElementSwagger(addType), 
	    					pathSb, definitionsSb, path,  tag == null ? useTag : tag, useOpId, null,
	    					newPathParams, newQueryParams, validEdges);
	        		}
	        		// need item name of array
					String itemName = processJavaTypeElementSwagger( addType, getJavaTypeElementSwagger(addType), 
	    					pathSb, definitionsSb, path,  tag == null ? useTag : tag, useOpId, 
	    							"array", null, null, null );
					
					if ( itemName != null ) {
						if ( addType.equals("AaiInternal") ) {
							//System.out.println( "addType AaiInternal, skip properties");
							addingAaiInternal = true;
						} else if ( getItemName == null) {
							++propertyCnt;
							sbProperties.append("      " + getXmlRootElementName(addType) + ":\n");
							sbProperties.append("        type: array\n        items:\n");
							sbProperties.append("          $ref: \"#/definitions/" + itemName + "\"\n");
							if ( attrDescription != null && attrDescription.length() > 0 )
								sbProperties.append("        description: " + attrDescription + "\n");
						}
					} else {
						/*itemName = processJavaTypeElementSwagger( addType, getJavaTypeElementSwagger(addType), 
	        					pathSb, definitionsSb, path,  tag == null ? useTag : tag, useOpId, "other" );
						if ( itemName != null ) {
						*/
						if ( elementContainerType != null && elementContainerType.equals("java.util.ArrayList")) {
							// need properties for getXmlRootElementName(addType)
				    		newPathParams = null;
				    		if ( pathParams != null  ) {
				    			newPathParams = new StringBuffer();
				    			newPathParams.append(pathParams);
				    		}
				            if ( sbParameters.toString().length() > 0 ) {
								if ( newPathParams == null )
									newPathParams = new StringBuffer();
								newPathParams.append(sbParameters);
				            }
				            newQueryParams = null;
				            if ( sbIndexedParams.toString().length() > 0 ) {
				            	if ( queryParams == null )
				            		newQueryParams = sbIndexedParams.toString();
				            	else
				            		newQueryParams = queryParams + sbIndexedParams.toString();
				            } else {
				            	newQueryParams = queryParams;
				            }
				            /*
		        			if ( validEdges == null ) {
		        			*/
		        				if ( addType.equals("RelationshipList") ) {
		        					// get edge rule
		        					Vector<String> edges = getEdgeRules(xmlRootElementName );
		        					if ( edges.size() > 0 ) {
			        					StringBuffer sbEdge = new StringBuffer();
			        					sbEdge.append("      description: |\n");				
			        					sbEdge.append("        ###### related-to\n");
			        					for ( int xx = 0; xx < edges.size(); ++ xx) {
			        						if ( edges.elementAt(xx).startsWith(xmlRootElementName))
			        						sbEdge.append("        - TO " + edges.elementAt(xx).substring(edges.elementAt(xx).indexOf("|")+1) + "\n");
			        					}
			        					for ( int xx = 0; xx < edges.size(); ++ xx) {
			        						if ( edges.elementAt(xx).endsWith(xmlRootElementName))
			        						sbEdge.append("        - FROM " + edges.elementAt(xx).substring(0, edges.elementAt(xx).indexOf("|")) + "\n");
			        					}
			        					validEdges = sbEdge.toString();
		        					}
		        				}
		        				/*
		        			}
		        			*/
							processJavaTypeElementSwagger( addType, getJavaTypeElementSwagger(addType), 
		        					pathSb, definitionsSb, path,  tag == null ? useTag : tag, useOpId, 
		        							null, newPathParams, newQueryParams, validEdges );
							sbProperties.append("      " + getXmlRootElementName(addType) + ":\n");
							sbProperties.append("        type: array\n        items:          \n");
							sbProperties.append("          $ref: \"#/definitions/" + getXmlRootElementName(addType) + "\"\n");
						} else {
							sbProperties.append("      " + getXmlRootElementName(addType) + ":\n");
							sbProperties.append("        type: object\n");
							sbProperties.append("        $ref: \"#/definitions/" + getXmlRootElementName(addType) + "\"\n");
						}
						if ( attrDescription != null && attrDescription.length() > 0 )
							sbProperties.append("        description: " + attrDescription + "\n");
						++propertyCnt;
						/*}
						else {
							System.out.println(" unable to define swagger object for " + addType);
						}
						*/
					}
					//if ( getItemName == null) looking for missing properties
						//generatedJavaType.put(addType, null);
	        	}
	        }
		}
	}	
		if ( genPath ) {
			/*
			if ( useOpId.equals("CloudInfrastructureComplexesComplexCtagPools"))
				System.out.println( "adding path CloudInfrastructureComplexesComplexCtagPools");
			*/

			if ( !path.endsWith("/relationship") ) {
				pathSb.append("  " + path + ":\n" );
				pathSb.append("    get:\n");
				pathSb.append("      tags:\n");
				pathSb.append("        - " + tag + "\n");
				pathSb.append("      summary: returns " + xmlRootElementName + "\n");
	
				pathSb.append("      description: returns " + xmlRootElementName + "\n");
				pathSb.append("      operationId: get" + useOpId + "\n");
				pathSb.append("      produces:\n");
				pathSb.append("        - application/json\n");
				pathSb.append("        - application/xml\n");
				
				pathSb.append("      responses:\n");
				pathSb.append("        \"200\":\n");
				pathSb.append("          description: successful operation\n");
				pathSb.append("          schema:\n");
				pathSb.append("              $ref: \"#/definitions/" + xmlRootElementName + "\"\n");
				pathSb.append("        \"default\":\n");
				pathSb.append("          " + responsesUrl);
				/*
				pathSb.append("        \"200\":\n");
				pathSb.append("          description: successful operation\n");
				pathSb.append("          schema:\n");
				pathSb.append("              $ref: \"#/definitions/" + xmlRootElementName + "\"\n");
				pathSb.append("        \"404\":\n");
				pathSb.append("          description: resource was not found\n");
				pathSb.append("        \"400\":\n");
				pathSb.append("          description: bad request\n");
				*/
				if ( path.indexOf('{') > 0 ) {
		    		
		            if ( sbParameters.toString().length() > 0 ) {
						if ( pathParams == null )
							pathParams = new StringBuffer();
						pathParams.append(sbParameters);
		            }
					if ( pathParams != null) {
						pathSb.append("      parameters:\n");
						pathSb.append(pathParams);
					} else
						System.out.println( "null pathParams for " + useOpId);
					if ( sbIndexedParams.toString().length() > 0 ) {
						if ( queryParams == null )
							queryParams = sbIndexedParams.toString();
						else
							queryParams = queryParams + sbIndexedParams.toString();
					}
					if ( queryParams != null ) {
						if ( pathParams == null ) {
							pathSb.append("      parameters:\n");
						}
						pathSb.append(queryParams);
					}
				}
			}
			boolean skipPutDelete = false; // no put or delete for "all" 
			if ( !path.endsWith("/relationship") ) {				
				if ( !path.endsWith("}") ){
						skipPutDelete = true;
				}
					
			}
			if ( path.indexOf('{') > 0 && !opId.startsWith("Search") &&!skipPutDelete) {
				// add PUT
				if ( path.endsWith("/relationship") ) {
					pathSb.append("  " + path + ":\n" );
				} 
				pathSb.append("    put:\n");
				pathSb.append("      tags:\n");
				pathSb.append("        - " + tag + "\n");

				if ( validEdges != null && path.endsWith("/relationship") ) {
					pathSb.append("      summary: see description for " + xmlRootElementName + " valid edges\n");
					pathSb.append( validEdges );
				} else {
					pathSb.append("      summary: create or update an existing " + xmlRootElementName + "\n");
					pathSb.append("      description: create or update an existing " + xmlRootElementName + "\n");
				}
				pathSb.append("      operationId: createOrUpdate" + useOpId + "\n");
				pathSb.append("      consumes:\n");
				pathSb.append("        - application/json\n");
				pathSb.append("        - application/xml\n");					
				pathSb.append("      produces:\n");
				pathSb.append("        - application/json\n");
				pathSb.append("        - application/xml\n");
				pathSb.append("      responses:\n");
				pathSb.append("        \"default\":\n");
				pathSb.append("          " + responsesUrl);
				/*
				pathSb.append("      responses:\n");
				pathSb.append("        \"200\":\n");
				pathSb.append("          description: existing resource has been modified and there is a response buffer\n");					
				pathSb.append("        \"201\":\n");
				pathSb.append("          description: new resource is created\n");	
				pathSb.append("        \"202\":\n");
				pathSb.append("          description: action requested but may have taken other actions as well, which are returned in the response payload\n");				
				pathSb.append("        \"204\":\n");
				pathSb.append("          description: existing resource has been modified and there is no response buffer\n");				
				pathSb.append("        \"400\":\n");
				pathSb.append("          description: Bad Request will be returned if headers are missing\n");
				pathSb.append("        \"404\":\n");
				pathSb.append("          description: Not Found will be returned if an unknown URL is used\n");
				*/						
				pathSb.append("      parameters:\n");
				//pathSb.append("        - in: path\n");
				pathSb.append(pathParams); // for nesting
				pathSb.append("        - name: body\n");
				pathSb.append("          in: body\n");
				pathSb.append("          description: " + xmlRootElementName + " object that needs to be created or updated\n");
				pathSb.append("          required: true\n");
				pathSb.append("          schema:\n");
				pathSb.append("            $ref: \"#/definitions/" + xmlRootElementName + "\"\n");
				/*
				if ( queryParams != null ) {
					pathSb.append(queryParams);
				}
				*/
				// add DELETE
				pathSb.append("    delete:\n");
				pathSb.append("      tags:\n");
				pathSb.append("        - " + tag + "\n");
				pathSb.append("      summary: delete an existing " + xmlRootElementName + "\n");
				
				pathSb.append("      description: delete an existing " + xmlRootElementName + "\n");
				
				pathSb.append("      operationId: delete" + useOpId + "\n");
				pathSb.append("      consumes:\n");
				pathSb.append("        - application/json\n");
				pathSb.append("        - application/xml\n");					
				pathSb.append("      produces:\n");
				pathSb.append("        - application/json\n");
				pathSb.append("        - application/xml\n");
				pathSb.append("      responses:\n");
				pathSb.append("        \"default\":\n");
				pathSb.append("          " + responsesUrl);
				/*
				pathSb.append("      responses:\n");
				pathSb.append("        \"200\":\n");
				pathSb.append("          description: successful, the response includes an entity describing the status\n");
				pathSb.append("        \"204\":\n");
				pathSb.append("          description: successful, action has been enacted but the response does not include an entity\n");
				pathSb.append("        \"400\":\n");
				pathSb.append("          description: Bad Request will be returned if headers are missing\n");
				pathSb.append("        \"404\":\n");
				pathSb.append("          description: Not Found will be returned if an unknown URL is used\n");
				*/				
				pathSb.append("      parameters:\n");
				//pathSb.append("        - in: path\n");
				pathSb.append(pathParams); // for nesting
				if ( !path.endsWith("/relationship") ) {
					pathSb.append("        - name: resource-version\n");
	
					pathSb.append("          in: query\n");
					pathSb.append("          description: resource-version for concurrency\n");
					pathSb.append("          required: true\n");
					pathSb.append("          type: string\n");
				}
				/*
				if ( queryParams != null ) {
					pathSb.append(queryParams);
				}
				*/
			}
			
		}
		if ( generatedJavaType.containsKey(xmlRootElementName) ) {
			return null;
		}
		definitionsSb.append("  " + xmlRootElementName + ":\n");
		if ( pathDescriptionProperty != null ) 
			definitionsSb.append("    description: >\n        " + pathDescriptionProperty
					+ "\n" );
		
		if ( requiredCnt > 0 )
			definitionsSb.append(sbRequired);
		if ( propertyCnt > 0 ) {
			definitionsSb.append("    properties:\n");
			definitionsSb.append(sbProperties);
		}
		generatedJavaType.put(xmlRootElementName, null);
		return null;
	}
	
	public static String generateSwaggerFromOxmFile( File oxmFile )
	{

		StringBuffer sb = new StringBuffer();
		sb.append("swagger: \"2.0\"\ninfo:\n  description:\n    A&AI REST API\n  version: \"" + apiVersion +"\"\n");
		sb.append("  title: Active and Available Inventory REST API\n");
		sb.append("  contact:\n    name: A&AI Delivery Team\n");
		sb.append("host: host.openecomp.com\nbasePath: /aai/" + apiVersion + "\n");
		sb.append("schemes:\n  - https\npaths:\n");
		/*
		sb.append("responses:\n");
		sb.append("  \"200\":\n");
		sb.append("    description: successful operation\n");
		sb.append("  \"404\":\n");
		sb.append("    description: resource was not found\n");
		sb.append("  \"400\":\n");
		sb.append("    description: bad request\n");
		*/
		try {
		    
		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    Document doc = dBuilder.parse(oxmFile);

		    NodeList bindingsNodes = doc.getElementsByTagName("xml-bindings");
			Element bindingElement;
			NodeList javaTypesNodes;
			Element javaTypesElement;
			
			Element javaTypeElement;

			
			if ( bindingsNodes == null || bindingsNodes.getLength() == 0 ) {
				System.out.println( "missing <binding-nodes> in " + oxmFile );
				return null;
			}	    
			
			bindingElement = (Element) bindingsNodes.item(0);
			javaTypesNodes = bindingElement.getElementsByTagName("java-types");
			if ( javaTypesNodes.getLength() < 1 ) {
				System.out.println( "missing <binding-nodes><java-types> in " + oxmFile );
				return null;
			}
			javaTypesElement = (Element) javaTypesNodes.item(0);

			javaTypeNodes = javaTypesElement.getElementsByTagName("java-type");
			if ( javaTypeNodes.getLength() < 1 ) {
				System.out.println( "missing <binding-nodes><java-types><java-type> in " + oxmFile );
				return null;
			}

			String javaTypeName;
			String attrName, attrValue;
			Attr attr;
			StringBuffer pathSb = new StringBuffer();
			
			StringBuffer definitionsSb = new StringBuffer("definitions:\n");
			
			for ( int i = 0; i < javaTypeNodes.getLength(); ++ i ) {
				javaTypeElement = (Element) javaTypeNodes.item(i);
				NamedNodeMap attributes = javaTypeElement.getAttributes();
				javaTypeName = null;
				for ( int j = 0; j < attributes.getLength(); ++j ) {
		            attr = (Attr) attributes.item(j);
		            attrName = attr.getNodeName();
		            attrValue = attr.getNodeValue();
		            if ( attrName.equals("name"))
		            	javaTypeName = attrValue;
				}
				if ( javaTypeName == null ) {
					System.out.println( "<java-type> has no name attribute in " + oxmFile );
					return null;
				}
				if ( !generatedJavaType.containsKey(getXmlRootElementName(javaTypeName)) ) {
					
					//generatedJavaType.put(javaTypeName, null);
					//if ( javaTypeName.equals("search")||javaTypeName.equals("actions"))

					processJavaTypeElementSwagger( javaTypeName, javaTypeElement, pathSb,
							definitionsSb, null, null, null, null, null, null, null);
				}
			}
			sb.append(pathSb);
			//System.out.println( "definitions block\n" + definitionsSb.toString());
			sb.append(definitionsSb.toString());
			//sb.append(definitionsSb);
				
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		//System.out.println("generated " + sb.toString());
		return sb.toString();
	}
	
	private static NodeList locateXmlProperties(Element element) {
		XPathExpression expr;
		try {
			expr = xpath.compile("xml-properties");
			Object result = expr.evaluate(element, XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			for (int i = 0; i < nodes.getLength(); i++) {
				Element xmlProperty = (Element)nodes.item(i);
				return xmlProperty.getElementsByTagName("xml-property");
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
}
