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

package org.openecomp.aai.dbgen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.openecomp.aai.dbmap.AAIGraph;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.ingestModel.DbMaps;
import org.openecomp.aai.ingestModel.IngestModelMoxyOxm;
import org.openecomp.aai.logging.AAILogger;
import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.logging.LogLine;
import org.openecomp.aai.util.AAIConfig;
import org.openecomp.aai.util.AAIConstants;
import org.openecomp.aai.util.AAIPrimaryHost;

import com.att.eelf.configuration.Configuration;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanVertex;


public class DataGrooming {

	private static final String FROMAPPID = "AAI-DB";
	private static final String TRANSID = UUID.randomUUID().toString();
	private static int dupeGrpsDeleted = 0;

	private static AAILogger aaiLogger;

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		
		// Set the logging file properties to be used by EELFManager
		Properties props = System.getProperties();
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_NAME, AAIConstants.AAI_DATA_GROOMING_LOGBACK_PROPS);
		props.setProperty(Configuration.PROPERTY_LOGGING_FILE_PATH, AAIConstants.AAI_HOME_ETC_APP_PROPERTIES);

		aaiLogger = new AAILogger(DataGrooming.class.getName());
		
		String ver = "version"; // Placeholder
		Boolean doAutoFix = false;
		Boolean edgesOnlyFlag = false;
		Boolean dontFixOrphansFlag = false;
		Boolean skipHostCheck = false;
		Boolean singleCommits = false;
		Boolean dupeCheckOff = false;
		Boolean dupeFixOn = false;
		Boolean ghost2CheckOff = false;
		Boolean ghost2FixOn = false;
		
		int maxRecordsToFix = AAIConstants.AAI_GROOMING_DEFAULT_MAX_FIX;
		int sleepMinutes = AAIConstants.AAI_GROOMING_DEFAULT_SLEEP_MINUTES;
		try {
			String maxFixStr = AAIConfig.get("aai.grooming.default.max.fix");
			if( maxFixStr != null &&  !maxFixStr.equals("") ){
				maxRecordsToFix = Integer.parseInt(maxFixStr);
			}
			String sleepStr = AAIConfig.get("aai.grooming.default.sleep.minutes");
			if( sleepStr != null &&  !sleepStr.equals("") ){
				sleepMinutes = Integer.parseInt(sleepStr);
			}
		}
		catch ( Exception e ){
			// Don't worry, we'll just use the defaults that we got from AAIConstants
			System.out.println("WARNING - could not pick up aai.grooming values from aaiconfig.properties file. ");
		}
		
		String prevFileName = "";
		dupeGrpsDeleted = 0;
		SimpleDateFormat d = new SimpleDateFormat("yyyyMMddHHmm");
		d.setTimeZone(TimeZone.getTimeZone("GMT"));
		String dteStr = d.format(new Date()).toString();
		String groomOutFileName = "dataGrooming." + dteStr + ".out";

		if (args.length > 0) {
			// They passed some arguments in that will affect processing
			for (int i = 0; i < args.length; i++) {
				String thisArg = args[i];
				if (thisArg.equals("-edgesOnly")) {
					edgesOnlyFlag = true;
				} else if (thisArg.equals("-autoFix")) {
					doAutoFix = true;
				} else if (thisArg.equals("-skipHostCheck")) {
					skipHostCheck = true;
				} else if (thisArg.equals("-dontFixOrphans")) {
					dontFixOrphansFlag = true;
				} else if (thisArg.equals("-singleCommits")) {
					singleCommits = true;
				} else if (thisArg.equals("-dupeCheckOff")) {
					dupeCheckOff = true;
				} else if (thisArg.equals("-dupeFixOn")) {
					dupeFixOn = true;
				} else if (thisArg.equals("-ghost2CheckOff")) {
					ghost2CheckOff = true;
				} else if (thisArg.equals("-ghost2FixOn")) {
					ghost2FixOn = true;
				} else if (thisArg.equals("-maxFix")) {
					i++;
					if (i >= args.length) {
						System.out
								.println(" No value passed with -maxFix option.  ");
						System.exit(0);
					}
					String nextArg = args[i];
					try {
						maxRecordsToFix = Integer.parseInt(nextArg);
					} catch (Exception e) {
						System.out
								.println("Bad value passed with -maxFix option: ["
										+ nextArg + "]");
						System.exit(0);
					}
				} else if (thisArg.equals("-sleepMinutes")) {
					i++;
					if (i >= args.length) {
						System.out
								.println("No value passed with -sleepMinutes option.");
						System.exit(0);
					}
					String nextArg = args[i];
					try {
						sleepMinutes = Integer.parseInt(nextArg);
					} catch (Exception e) {
						System.out
								.println("Bad value passed with -sleepMinutes option: ["
										+ nextArg + "]");
						System.exit(0);
					}
				} else if (thisArg.equals("-f")) {
					i++;
					if (i >= args.length) {
						System.out.println(" No value passed with -f option. ");
						System.exit(0);
					}
					prevFileName = args[i];
				} else {
					System.out
							.println(" Unrecognized argument passed to DataGrooming: ["
									+ thisArg + "]. ");
					System.out
							.println(" Valid values are: -f -autoFix -maxFix -edgesOnly -dupeFixOn -donFixOrphans -sleepMinutes");
					System.exit(0);
				}
			}
		}
		

		IngestModelMoxyOxm moxyMod = new IngestModelMoxyOxm();
		try {
			ArrayList <String> defaultVerLst = new ArrayList <String> ();
			defaultVerLst.add( AAIConfig.get(AAIConstants.AAI_DEFAULT_API_VERSION_PROP) );
			moxyMod.init( defaultVerLst, false);
		}
		catch (Exception ex){
			String emsg = " ERROR - Could not do the moxyMod.init(). ";
			System.out.print(emsg);
			System.out.println("Exception.getMessage() = [" + ex.getMessage() + "]");
			System.exit(1);
		}

		if (skipHostCheck) {
			System.out.println(" We will skip the HostCheck as requested. ");
		} else {
			// Make sure we are on the "primary" host -- so the cron won't have
			// us running on all the servers...
			try {
				AAIPrimaryHost primaryHost = new AAIPrimaryHost(TRANSID, FROMAPPID);
				if( ! primaryHost.amIPrimary()) { 
					System.out.println(" This is not the Primary Host, so DataGrooming will not be run.  " 
							+ " (Note: -skipHostCheck option can override this check)");
					System.exit(0);
				} 	
			}
			catch ( Exception e) { 
				System.out.println(" Error trying to determine if this is the Primary Host. " 
						+ " (Note: -skipHostCheck option can override this host-check)" );
				System.exit(0);
			}
		}		

		try {
			if (!prevFileName.equals("")) {
				// They are trying to fix some data based on a data in a
				// previous file.
				System.out
						.println(" Call doTheGrooming() with a previous fileName ["
								+ prevFileName + "] for cleanup. ");
				Boolean finalShutdownFlag = true;
				doTheGrooming(prevFileName, edgesOnlyFlag, dontFixOrphansFlag,
						maxRecordsToFix, groomOutFileName, ver, singleCommits,
						dupeCheckOff, dupeFixOn, ghost2CheckOff, ghost2FixOn, finalShutdownFlag);
			} else if (doAutoFix) {
				// They want us to run the processing twice -- first to look for
				// delete candidates, then after
				// napping for a while, run it again and delete any candidates
				// that were found by the first run.
				// Note: we will produce a seperate output file for each of the
				// two runs.
				System.out.println(" Doing an auto-fix call to Grooming. ");
				System.out
						.println(" First, Call doTheGrooming() to look at what's out there. ");
				Boolean finalShutdownFlag = false;
				int fixCandCount = doTheGrooming("", edgesOnlyFlag,
						dontFixOrphansFlag, maxRecordsToFix, groomOutFileName,
						ver, singleCommits, dupeCheckOff, dupeFixOn, ghost2CheckOff, ghost2FixOn, finalShutdownFlag);
				if (fixCandCount == 0) {
					System.out
							.println(" No fix-Candidates were found by the first pass, so no second/fix-pass is needed. ");
				} else {
					// We'll sleep a little and then run a fix-pass based on the
					// first-run's output file.
					try {
						System.out.println("About to sleep for " + sleepMinutes
								+ " minutes.");
						int sleepMsec = sleepMinutes * 60 * 1000;
						Thread.sleep(sleepMsec);
					} catch (InterruptedException ie) {
						System.out
								.println("\n >>> Sleep Thread has been Interrupted <<< ");
						System.exit(0);
					}

					d = new SimpleDateFormat("yyyyMMddHHmm");
					d.setTimeZone(TimeZone.getTimeZone("GMT"));
					dteStr = d.format(new Date()).toString();
					String secondGroomOutFileName = "dataGrooming." + dteStr
							+ ".out";
					System.out
							.println(" Now, call doTheGrooming() a second time and pass in the name of the file "
									+ "generated by the first pass for fixing: ["
									+ groomOutFileName + "]");
					finalShutdownFlag = true;
					doTheGrooming(groomOutFileName, edgesOnlyFlag,
							dontFixOrphansFlag, maxRecordsToFix,
							secondGroomOutFileName, ver, singleCommits,
							dupeCheckOff, dupeFixOn, ghost2CheckOff, ghost2FixOn, finalShutdownFlag);
				}
			} else {
				// Do the grooming - plain vanilla (no fix-it-file, no
				// auto-fixing)
				Boolean finalShutdownFlag = true;
				System.out.println(" Call doTheGrooming() ");
				doTheGrooming("", edgesOnlyFlag, dontFixOrphansFlag,
						maxRecordsToFix, groomOutFileName, ver, singleCommits,
						dupeCheckOff, dupeFixOn, ghost2CheckOff, ghost2FixOn, finalShutdownFlag );
			}
		} catch (Exception ex) {
			System.out.print("Threw a regular Exception:\n");
			System.out.println(ex.getMessage());
		}

		System.out.println(" Done! ");
		System.exit(0);

	}// End of main()

	/**
	 * Do the grooming.
	 *
	 * @param fileNameForFixing the file name for fixing
	 * @param edgesOnlyFlag the edges only flag
	 * @param dontFixOrphansFlag the dont fix orphans flag
	 * @param maxRecordsToFix the max records to fix
	 * @param groomOutFileName the groom out file name
	 * @param version the version
	 * @param singleCommits the single commits
	 * @param dupeCheckOff the dupe check off
	 * @param dupeFixOn the dupe fix on
	 * @param ghost2CheckOff the ghost 2 check off
	 * @param ghost2FixOn the ghost 2 fix on
	 * @param finalShutdownFlag the final shutdown flag
	 * @return the int
	 */
	private static int doTheGrooming(String fileNameForFixing,
			Boolean edgesOnlyFlag, Boolean dontFixOrphansFlag,
			int maxRecordsToFix, String groomOutFileName, String version,
			Boolean singleCommits, 
			Boolean dupeCheckOff, Boolean dupeFixOn,
			Boolean ghost2CheckOff, Boolean ghost2FixOn, Boolean finalShutdownFlag) {

		System.out.println(" Entering doTheGrooming \n");

		int cleanupCandidateCount = 0;
		BufferedWriter bw = null;
		TitanGraph graph = null;
		TitanGraph graph2 = null;
		int deleteCount = 0;

		LogLine logline = new LogLine();
		logline.init("aaidbgen", TRANSID, FROMAPPID, "doTheGrooming");

		ArrayList<String> deleteCandidateList = new ArrayList<String>();
		TitanTransaction g = null;
		TitanTransaction g2 = null;
		try {
			AAIConfig.init(TRANSID, FROMAPPID);
			String targetDir = AAIConstants.AAI_HOME + AAIConstants.AAI_FILESEP
					+ "logs" + AAIConstants.AAI_FILESEP + "data"
					+ AAIConstants.AAI_FILESEP + "dataGrooming";

			// Make sure the target directory exists
			new File(targetDir).mkdirs();

			if (!fileNameForFixing.equals("")) {
				deleteCandidateList = getDeleteList(targetDir,
						fileNameForFixing, edgesOnlyFlag, dontFixOrphansFlag,
						dupeFixOn);
			}

			if (deleteCandidateList.size() > maxRecordsToFix) {
				String infoMsg = " >> WARNING >>  Delete candidate list size ("
						+ deleteCandidateList.size()
						+ ") is too big.  The maxFix we are using is: "
						+ maxRecordsToFix
						+ ".  No candidates will be deleted. ";
				aaiLogger.debug(logline, infoMsg);
				System.out.println(infoMsg);
				// Clear out the list so it won't be processed below.
				deleteCandidateList = new ArrayList<String>();
			}

			SimpleDateFormat d = new SimpleDateFormat("yyyyMMddHHmm");
			d.setTimeZone(TimeZone.getTimeZone("GMT"));
			String dteStr = d.format(new Date()).toString();

			String fullOutputFileName = targetDir + AAIConstants.AAI_FILESEP
					+ groomOutFileName;
			File groomOutFile = new File(fullOutputFileName);
			try {
				groomOutFile.createNewFile();
			} catch (IOException e) {
				String emsg = " Problem creating output file ["
						+ fullOutputFileName + "], exception=" + e.getMessage();
				throw new AAIException("AAI_6124", emsg);
			}

			logline.add("OutputFileName", fullOutputFileName);
			System.out.println(" Will write to " + fullOutputFileName );
			FileWriter fw = new FileWriter(groomOutFile.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			ErrorLogHelper.loadProperties();
			System.out.println("    ---- NOTE --- about to open graph (takes a little while)--------\n");

			
			graph = AAIGraph.getInstance().getGraph();
			if (graph == null) {
				String emsg = "null graph object in DataGrooming\n";
				throw new AAIException("AAI_6101", emsg);
			}
			System.out.println(" Got the graph object. ");
			
			g = graph.newTransaction();
			if (g == null) {
				String emsg = "null graphTransaction object in DataGrooming\n";
				throw new AAIException("AAI_6101", emsg);
			}

			
			ArrayList<String> errArr = new ArrayList<String>();
			int totalNodeCount = 0;
			HashMap<String, String> misMatchedHash = new HashMap<String, String>();
			HashMap<String, TitanVertex> orphanNodeHash = new HashMap<String, TitanVertex>();
			HashMap<String, TitanVertex> missingDepNodeHash = new HashMap<String, TitanVertex>();
			HashMap<String, Edge> oneArmedEdgeHash = new HashMap<String, Edge>();
			HashMap<String, String> emptyVertexHash = new HashMap<String, String>();
			HashMap<String, TitanVertex> ghostNodeHash = new HashMap<String, TitanVertex>();
			ArrayList<String> dupeGroups = new ArrayList<String>();


			DbMaps dbMaps = IngestModelMoxyOxm.dbMapsContainer.get(AAIConfig.get(AAIConstants.AAI_DEFAULT_API_VERSION_PROP));
			
			Iterator<String> nodeMapKPropsIterator = dbMaps.NodeKeyProps.keySet().iterator();
			String ntList = "";

			System.out.println("  Starting DataGrooming Processing ");

			if (edgesOnlyFlag) {
				System.out.println(" NOTE >> Skipping Node processing as requested.  Will only process Edges. << ");
			} 
			else {
				while (nodeMapKPropsIterator.hasNext()) {
					String nType = nodeMapKPropsIterator.next();
					int thisNtCount = 0;
					int thisNtDeleteCount = 0;
					String infoMsg = " >  Look at : [" + nType + "] ...";
					aaiLogger.debug(logline, infoMsg);
					System.out.println(infoMsg);
					ntList = ntList + "," + nType;

					// Get a collection of the names of the key properties for this nodeType to use later
					Collection<String> keyProps = DbMeth.getNodeKeyPropNames(TRANSID, FROMAPPID, nType, version);
					// Get the types of nodes that this nodetype depends on for uniqueness (if any)
					ArrayList <String> depNodeTypes = DbMeth.getDepNodeTypes(TRANSID, FROMAPPID, nType, version);
					
					// Loop through all the nodes of this Node type
					int lastShownForNt = 0;
					ArrayList <TitanVertex> tmpList = new ArrayList <TitanVertex> ();
					Iterable <?> verts =  g.query().has("aai-node-type",nType).vertices(); 
					Iterator<?> iterv = verts.iterator();
					while (iterv.hasNext()) {
						// We put the nodes into an ArrayList because the graph.query iterator can time out
						tmpList.add((TitanVertex)iterv.next());
					}
					
					Iterator <?> iter = tmpList.iterator();
					while (iter.hasNext()) {
						try {
							thisNtCount++;
							if( thisNtCount == lastShownForNt + 250 ){
								lastShownForNt = thisNtCount;
								System.out.println("count for " + nType + " so far = " + thisNtCount );
							}
							totalNodeCount++;
							TitanVertex thisVtx = (TitanVertex) iter.next();
							String thisVid = thisVtx.id().toString();
							ArrayList <TitanVertex> secondGetList = new ArrayList <TitanVertex> ();
							// -----------------------------------------------------------------------
							// For each vertex of this nodeType, we want to:
							//		a) make sure that it can be retrieved using it's AAI defined key
							//   	b) make sure that it is not a duplicate
							// -----------------------------------------------------------------------
							
							// For this instance of this nodeType, get the key properties 
							HashMap<String, Object> propHashWithKeys = new HashMap<String, Object>();
							Iterator<String> keyPropI = keyProps.iterator();
							while (keyPropI.hasNext()) {
								String propName = keyPropI.next();
								String propVal = "";
								Object obj = thisVtx.<Object>property(propName).orElse(null);
								if (obj != null) {
									propVal = obj.toString();
								}
								propHashWithKeys.put(propName, propVal);
							}
							try {
								// If this node is dependent on another for uniqueness, then do the query from that parent node
								// Note - all of our nodes that are dependent on others for uniqueness are 
								// 		"children" of that node.
								boolean depNodeOk = true;
								if( depNodeTypes.isEmpty() ){
									// This kind of node is not dependent on any other.
									// Make sure we can get it back using it's key properties and that we only get one.
									secondGetList = getNodeJustUsingKeyParams( TRANSID, FROMAPPID, g, nType, 
											propHashWithKeys, version );
								} 
								else {
									// This kind of node is dependent on another for uniqueness.  
									// Start at it's parent (the dependent vertex) and make sure we can get it
									// back using it's key properties and that we only get one.
									Iterable <?> verts2 = thisVtx.query().direction(Direction.IN).has("isParent",true).vertices();
									Iterator <?> vertI2 = verts2.iterator();
									TitanVertex parentVtx = null;
									int pCount = 0;
									while( vertI2 != null && vertI2.hasNext() ){
										parentVtx = (TitanVertex) vertI2.next();
										pCount++;
									}
									if( pCount <= 0 ){
										// It's Missing it's dependent/parent node 
										depNodeOk = false;
										boolean zeroEdges = false;
										try {
											Iterator<Edge> tmpEdgeIter = thisVtx.edges(Direction.BOTH);
											int edgeCount = 0;
											while( tmpEdgeIter.hasNext() ){
												edgeCount++;
												tmpEdgeIter.next();
											}
											if( edgeCount == 0 ){  
												zeroEdges = true;
											}
										} catch (Exception ex) {
											String msg = "WARNING from inside the for-each-vid-loop orphan-edges-check ";
											logline.add(msg, ex.getMessage());
											System.out.println(msg + ex.getMessage() + ex.toString());
										}
										
										if (deleteCandidateList.contains(thisVid)) {
											boolean okFlag = true;
											try {
												thisVtx.remove();
												deleteCount++;
												thisNtDeleteCount++;
											} catch (Exception e) {
												okFlag = false;
												String msg = "ERROR trying to delete missing-dep-node VID = " + thisVid;
												logline.add(msg, e.getMessage());
												System.out.println(msg);
											}
											if (okFlag) {
												logline.add( "DELETED missing-dep-node VID:", thisVid);
												System.out.println(" DELETED missing-dep-node VID = " + thisVid);
											}
										} else {
											// We count nodes missing their depNodes two ways - the first if it has
											//    at least some edges, and the second if it has zero edges.  Either
											//    way, they are effectively orphaned.
											// NOTE - Only nodes that have dependent nodes are ever considered "orphaned".
											if( zeroEdges ){
												missingDepNodeHash.put(thisVid,	thisVtx);
											}
											else {
												orphanNodeHash.put(thisVid, thisVtx);
											}
										}
									}
									else if ( pCount > 1 ){
										// Not sure how this could happen?  Should we do something here?
										depNodeOk = false;
									}
									else {
										// We found the parent - so use it to do the second-look.
										// NOTE --- We're just going to do the same check from the other direction - because
										//  there could be duplicates or the pointer going the other way could be broken
										ArrayList <TitanVertex> tmpListSec = new ArrayList <TitanVertex> ();
										tmpListSec = DbMeth.getConnectedChildren(TRANSID, FROMAPPID, g, parentVtx, nType ) ;
										Iterator<TitanVertex> vIter = tmpListSec.iterator();
										while (vIter.hasNext()) {
											TitanVertex tmpV = vIter.next();
											if( vertexHasTheseKeys(tmpV, propHashWithKeys) ){
												secondGetList.add(tmpV);
											}
										}
									}
								}
								
								if( depNodeOk && (secondGetList == null || secondGetList.size() == 0) ){
									// We could not get the node back using it's own key info. 
									// So, it's a PHANTOM
									if (deleteCandidateList.contains(thisVid)) {
										boolean okFlag = true;
										try {
											thisVtx.remove();
											deleteCount++;
											thisNtDeleteCount++;
										} catch (Exception e) {
											okFlag = false;
											String msg = "ERROR trying to delete phantom VID = " + thisVid;
											logline.add(msg, e.getMessage());
											System.out.println(msg);
										}
										if (okFlag) {
											logline.add("DELETED VID:", thisVid);
											System.out.println(" DELETED VID = " + thisVid);
										}
									} else {
										ghostNodeHash.put(thisVid, thisVtx);
									}
								}
								else if( (secondGetList.size() > 1) && depNodeOk && !dupeCheckOff ){
									// Found some DUPLICATES - need to process them
									System.out.print(" - now check Dupes for this guy - ");
									ArrayList<String> tmpDupeGroups = checkAndProcessDupes(
												TRANSID, FROMAPPID, g, version,
												nType, secondGetList, dupeFixOn,
												deleteCandidateList, singleCommits,	dupeGroups, dbMaps);
									Iterator<String> dIter = tmpDupeGroups.iterator();
									while (dIter.hasNext()) {
										// Add in any newly found dupes to our running list
										String tmpGrp = dIter.next();
										System.out.println("Found set of dupes: [" + tmpGrp + "]");
										dupeGroups.add(tmpGrp);
									}
								}
							} 
							catch (AAIException e1) {
								String msg = " For nodeType = " + nType + " Caught this exception: "
										+ e1.getErrorObject().toString();
								System.out.println(msg);
								errArr.add(msg);
							}
							catch (Exception e2) {
								String msg = " For nodeType = " + nType
										+ " Caught this exception: "
										+ e2.toString();
								System.out.println(msg);
								errArr.add(msg);
							}
						}// try block to enclose looping of a single vertex
						catch (Exception exx) {
							String msg = "WARNING from inside the while-verts-loop ";
							logline.add(msg, exx.getMessage());
							System.out.println(msg + exx.getMessage()
									+ exx.toString());
						}
						
					} // while loop for each record of a nodeType
					
					if ( (thisNtDeleteCount > 0) && singleCommits ) {
						g.commit();
						g = AAIGraph.getInstance().getGraph().newTransaction();
						
					}
					thisNtDeleteCount = 0;
					System.out.println( " Processed " + thisNtCount + " records for [" + nType + "], " + totalNodeCount + " total overall. " );
					
				}// While-loop for each node type
			}// end of check to make sure we weren't only supposed to do edges

		
			// --------------------------------------------------------------------------------------
			// Now, we're going to look for one-armed-edges. Ie. an edge that
			// should have
			// been deleted (because a vertex on one side was deleted) but
			// somehow was not deleted.
			// So the one end of it points to a vertexId -- but that vertex is
			// empty.
			// --------------------------------------------------------------------------------------

			// To do some strange checking - we need a second graph object
			System.out.println("    ---- DEBUG --- about to open a SECOND graph (takes a little while)--------\n");
			graph2 = TitanFactory.open(AAIConstants.AAI_CONFIG_FILENAME);
					
			if (graph2 == null) {
				String emsg = "null graph2 object in DataGrooming\n";
				throw new AAIException("AAI_6101", emsg);
			} else {
				System.out.println("Got the graph2 object... \n");
			}
			g2 = graph2.newTransaction();
			if (g2 == null) {
				String emsg = "null graphTransaction2 object in DataGrooming\n";
				throw new AAIException("AAI_6101", emsg);
			}
			
			ArrayList<Vertex> vertList = new ArrayList<Vertex>();
			Iterable vIt3 = g.query().vertices();
			Iterator<Vertex> vItor3 = vIt3.iterator();
			// Gotta hold these in a List - or else HBase times out as you cycle
			// through these
			while (vItor3.hasNext()) {
				Vertex v = vItor3.next();
				vertList.add(v);
			}
			int counter = 0;
			int lastShown = 0;
			Iterator<Vertex> vItor2 = vertList.iterator();
			System.out.println(" Checking for bad edges  --- ");

			while (vItor2.hasNext()) {
				Vertex v = null;
				try {
					try {
						v = vItor2.next();
					} catch (Exception vex) {
						String msg = ">>> WARNING trying to get next vertex on the vItor2 ";
						logline.add(msg, vex.getMessage());
						System.out.println(msg + vex.getMessage());
						continue;
					}

					counter++;
					String thisVertId = "";
					try {
						thisVertId = v.id().toString();
					} catch (Exception ev) {
						String msg = "WARNING when doing getId() on a vertex from our vertex list.  ";
						logline.add(msg, ev.getMessage());
						System.out.println(msg);
						continue;
					}
					if (ghostNodeHash.containsKey(thisVertId)) {
						// This is a phantom node, so don't try to use it
						System.out
								.println(" >> Skipping edge check for edges from vertexId = "
										+ thisVertId
										+ ", since that guy is a Phantom Node");
						continue;
					}
					if (counter == lastShown + 250) {
						lastShown = counter;
						System.out.println("... Checking edges for vertex # "
								+ counter);
					}
					Iterator<Edge> eItor = v.edges(Direction.BOTH);
					while (eItor.hasNext()) {
						Edge e = null;
						Vertex vIn = null;
						Vertex vOut = null;
						try {
							e = eItor.next();
						} catch (Exception iex) {
							String msg = ">>> WARNING trying to get next edge on the eItor ";
							logline.add(msg, iex.getMessage());
							System.out.println(msg + iex.getMessage());
							continue;
						}

						try {
							vIn = e.inVertex();
						} catch (Exception err) {
							String msg = ">>> WARNING trying to get edge's In-vertex ";
							logline.add(msg, err.getMessage());
							System.out.println(msg + err.getMessage());
						}
						String vNtI = "";
						String vIdI = "";
						TitanVertex ghost2 = null;
						
						Boolean keysMissing = true;
						Boolean cantGetUsingVid = false;
						if (vIn != null) {
							try {
								Object ob = vIn.<Object>property("aai-node-type").orElse(null);
								if (ob != null) {
									vNtI = ob.toString();
									keysMissing = anyKeyFieldsMissing(vNtI, vIn);
								}
								ob = vIn.id();
								long vIdLong = 0L;
								if (ob != null) {
									vIdI = ob.toString();
									vIdLong = Long.parseLong(vIdI);
								}
								
								if( ! ghost2CheckOff ){
									TitanVertex connectedVert = g2.getVertex(vIdLong);
									if( connectedVert == null ) {
										System.out.println( "GHOST2 -- got NULL when doing getVertex for vid = " + vIdLong);
										cantGetUsingVid = true;
										
										// If we can NOT get this ghost with the SECOND graph-object, 
										// it is still a ghost since even though we can get data about it using the FIRST graph 
										// object.  
										try {
											 ghost2 = g.getVertex(vIdLong);
										}
										catch( Exception ex){
											System.out.println( "GHOST2 --  Could not get the ghost info for a bad edge for vtxId = " + vIdLong);
										}
										if( ghost2 != null ){
											ghostNodeHash.put(vIdI, ghost2);
										}
									}
								}// end of the ghost2 checking
							} 
							catch (Exception err) {
								String msg = ">>> WARNING trying to get edge's In-vertex props ";
								logline.add(msg, err.getMessage());
								System.out.println(msg + err.getMessage());
							}
						}
						if (keysMissing || vIn == null || vNtI.equals("")
								|| cantGetUsingVid) {
							// this is a bad edge because it points to a vertex
							// that isn't there anymore or is corrupted
							String thisEid = e.id().toString();
							if (deleteCandidateList.contains(thisEid) || deleteCandidateList.contains(vIdI)) {
								boolean okFlag = true;
								if (!vIdI.equals("")) {
									// try to get rid of the corrupted vertex
									try {
										if( (ghost2 != null) && ghost2FixOn ){
											ghost2.remove();
										}
										else {
											vIn.remove();
										}
										if (singleCommits) {
											g.commit();
											g = AAIGraph.getInstance().getGraph().newTransaction();
										}
										deleteCount++;
									} catch (Exception e1) {
										okFlag = false;
										String msg = "WARNING when trying to delete bad-edge-connected VERTEX VID = "
												+ vIdI;
										logline.add(msg, e1.getMessage());
										System.out.println(msg);
									}
									if (okFlag) {
										logline.add(
												"DELETED vertex on a bad edge = ",
												vIdI);
										System.out
												.println(" DELETED vertex from bad edge = "
														+ vIdI);
									}
								} else {
									// remove the edge if we couldn't get the
									// vertex
									try {
										e.remove();
										if (singleCommits) {
											g.commit();
											g = AAIGraph.getInstance().getGraph().newTransaction();
										}
										deleteCount++;
									} catch (Exception ex) {
										// NOTE - often, the exception is just
										// that this edge has already been
										// removed
										okFlag = false;
										String msg = "WARNING when trying to delete edge = "
												+ thisEid;
										logline.add(msg, ex.getMessage());
										System.out.println(msg);
									}
									if (okFlag) {
										logline.add("DELETED edge = ", thisEid);
										System.out.println(" DELETED edge = "
												+ thisEid);
									}
								}
							} else {
								oneArmedEdgeHash.put(thisEid, e);
								if ((vIn != null) && (vIn.id() != null)) {
									emptyVertexHash.put(thisEid, vIn.id()
											.toString());
								}
							}
						}

						try {
							vOut = e.outVertex();
						} catch (Exception err) {
							String msg = ">>> WARNING trying to get edge's Out-vertex ";
							logline.add(msg, err.getMessage());
							System.out.println(msg + err.getMessage());
						}
						String vNtO = "";
						String vIdO = "";
						ghost2 = null;
						keysMissing = true;
						cantGetUsingVid = false;
						if (vOut != null) {
							try {
								Object ob = vOut.<Object>property("aai-node-type").orElse(null);
								if (ob != null) {
									vNtO = ob.toString();
									keysMissing = anyKeyFieldsMissing(vNtO,
											vOut);
								}
								ob = vOut.id();
								long vIdLong = 0L;
								if (ob != null) {
									vIdO = ob.toString();
									vIdLong = Long.parseLong(vIdO);
								}
								
								if( ! ghost2CheckOff ){
									TitanVertex connectedVert = g2.getVertex(vIdLong);
									if( connectedVert == null ) {
										cantGetUsingVid = true;
										System.out.println( "GHOST2 -- got NULL when doing getVertex for vid = " + vIdLong);
										// If we can get this ghost with the other graph-object, then get it -- it's still a ghost
										try {
											 ghost2 = g.getVertex(vIdLong);
										}
										catch( Exception ex){
											System.out.println( "GHOST2 -- Could not get the ghost info for a bad edge for vtxId = " + vIdLong);
										}
										if( ghost2 != null ){
											ghostNodeHash.put(vIdO, ghost2);
										}
									}
								}
							} catch (Exception err) {
								String msg = ">>> WARNING trying to get edge's Out-vertex props ";
								logline.add(msg, err.getMessage());
								System.out.println(msg + err.getMessage());
							}
						}
						if (keysMissing || vOut == null || vNtO.equals("")
								|| cantGetUsingVid) {
							// this is a bad edge because it points to a vertex
							// that isn't there anymore
							String thisEid = e.id().toString();
							if (deleteCandidateList.contains(thisEid) || deleteCandidateList.contains(vIdO)) {
								boolean okFlag = true;
								if (!vIdO.equals("")) {
									// try to get rid of the corrupted vertex
									try {
										if( (ghost2 != null) && ghost2FixOn ){
											ghost2.remove();
										}
										else {
											vOut.remove();
										}
										if (singleCommits) {
											g.commit();
											g = AAIGraph.getInstance().getGraph().newTransaction();
										}
										deleteCount++;
									} catch (Exception e1) {
										okFlag = false;
										String msg = "WARNING when trying to delete bad-edge-connected VID = "
												+ vIdO;
										logline.add(msg, e1.getMessage());
										System.out.println(msg);
									}
									if (okFlag) {
										logline.add(
												"DELETED vertex on a bad edge = ",
												vIdO);
										System.out
												.println(" DELETED vertex from bad edge = "
														+ vIdO);
									}
								} else {
									// remove the edge if we couldn't get the
									// vertex
									try {
										e.remove();
										if (singleCommits) {
											g.commit();
											g = AAIGraph.getInstance().getGraph().newTransaction();

										}
										deleteCount++;
									} catch (Exception ex) {
										// NOTE - often, the exception is just
										// that this edge has already been
										// removed
										okFlag = false;
										String msg = "WARNING when trying to delete edge = "
												+ thisEid;
										logline.add(msg, ex.getMessage());
										System.out.println(msg);
									}
									if (okFlag) {
										logline.add("DELETED edge = ", thisEid);
										System.out.println(" DELETED edge = "
												+ thisEid);
									}
								}
							} else {
								oneArmedEdgeHash.put(thisEid, e);
								if ((vOut != null) && (vOut.id() != null)) {
									emptyVertexHash.put(thisEid, vOut.id()
											.toString());
								}
							}
						}
					}// End of while-edges-loop
				} catch (Exception exx) {
					String msg = "WARNING from in the while-verts-loop ";
					logline.add(msg, exx.getMessage());
					System.out.println(msg);
				}
			}// End of while-vertices-loop

			deleteCount = deleteCount + dupeGrpsDeleted;
			if (!singleCommits && deleteCount > 0) {
				try {
					System.out.println("About to do the commit for "
							+ deleteCount + " removes. ");
					g.commit();
					System.out.println("Commit was successful ");
				} catch (Exception excom) {
					String msg = " >>>> ERROR <<<<   Could not commit changes. ";
					logline.add(msg, excom.getMessage());
					System.out.println(msg);
					deleteCount = 0;
				}
			}

			int ghostNodeCount = ghostNodeHash.size();
			int orphanNodeCount = orphanNodeHash.size();
			int missingDepNodeCount = missingDepNodeHash.size();
			int oneArmedEdgeCount = oneArmedEdgeHash.size();
			int dupeCount = dupeGroups.size();

			deleteCount = deleteCount + dupeGrpsDeleted;

			bw.write("\n\n ============ Summary ==============\n");
			bw.write("Ran these nodeTypes: " + ntList + "\n\n");
			bw.write("There were this many delete candidates from previous run =  "
					+ deleteCandidateList.size() + "\n");
			if (dontFixOrphansFlag) {
				bw.write(" Note - we are not counting orphan nodes since the -dontFixOrphans parameter was used. \n");
			}
			bw.write("Deleted this many delete candidates =  " + deleteCount
					+ "\n");
			bw.write("Total number of nodes looked at =  " + totalNodeCount
					+ "\n");
			bw.write("Ghost Nodes identified = " + ghostNodeCount + "\n");
			bw.write("Orphan Nodes identified =  " + orphanNodeCount + "\n");
			bw.write("Bad Edges identified =  " + oneArmedEdgeCount + "\n");
			bw.write("Missing Dependent Edge (but not orphaned) node count = "
					+ missingDepNodeCount + "\n");
			bw.write("Duplicate Groups count =  " + dupeCount + "\n");
			bw.write("MisMatching Label/aai-node-type count =  "
					+ misMatchedHash.size() + "\n");

			bw.write("\n ------------- Delete Candidates ---------\n");
			for (Map.Entry<String, TitanVertex> entry : ghostNodeHash
					.entrySet()) {
				String vid = entry.getKey();
				bw.write("DeleteCandidate: Phantom Vid = [" + vid + "]\n");
				cleanupCandidateCount++;
			}
			for (Map.Entry<String, TitanVertex> entry : orphanNodeHash
					.entrySet()) {
				String vid = entry.getKey();
				bw.write("DeleteCandidate: OrphanDepNode Vid = [" + vid + "]\n");
				if (!dontFixOrphansFlag) {
					cleanupCandidateCount++;
				}
			}
			for (Map.Entry<String, Edge> entry : oneArmedEdgeHash.entrySet()) {
				String eid = entry.getKey();
				bw.write("DeleteCandidate: Bad EDGE Edge-id = [" + eid + "]\n");
				cleanupCandidateCount++;
			}
			for (Map.Entry<String, TitanVertex> entry : missingDepNodeHash
					.entrySet()) {
				String vid = entry.getKey();
				bw.write("DeleteCandidate: (maybe) missingDepNode Vid = ["
						+ vid + "]\n");
				cleanupCandidateCount++;
			}
			bw.write("\n-- NOTE - To see DeleteCandidates for Duplicates, you need to look in the Duplicates Detail section below.\n");

			bw.write("\n ------------- GHOST NODES - detail ");
			for (Map.Entry<String, TitanVertex> entry : ghostNodeHash
					.entrySet()) {
				try {
					String vid = entry.getKey();
					bw.write("\n ==> Phantom Vid = " + vid + "\n");
					ArrayList<String> retArr = DbMeth.showPropertiesForNode(
							TRANSID, FROMAPPID, entry.getValue());
					for (String info : retArr) {
						bw.write(info + "\n");
					}
	
					retArr = DbMeth.showAllEdgesForNode(TRANSID, FROMAPPID,
							entry.getValue());
					for (String info : retArr) {
						bw.write(info + "\n");
					}
				} catch (Exception dex) {
					String msg = "error trying to print detail info for a ghost-node:  ";
					logline.add(msg, dex.getMessage());
					System.out.println(msg);
				}
			}

			bw.write("\n ------------- Missing Dependent Edge ORPHAN NODES - detail: ");
			for (Map.Entry<String, TitanVertex> entry : orphanNodeHash
					.entrySet()) {
				try {
					String vid = entry.getKey();
					bw.write("\n> Orphan Node Vid = " + vid + "\n");
					ArrayList<String> retArr = DbMeth.showPropertiesForNode(
							TRANSID, FROMAPPID, entry.getValue());
					for (String info : retArr) {
						bw.write(info + "\n");
					}
	
					retArr = DbMeth.showAllEdgesForNode(TRANSID, FROMAPPID,
							entry.getValue());
					for (String info : retArr) {
						bw.write(info + "\n");
					}
				} catch (Exception dex) {
					String msg = "error trying to print detail info for a Orphan Node /missing dependent edge:  ";
					logline.add(msg, dex.getMessage());
					System.out.println(msg);
				}
			}

			bw.write("\n ------------- Missing Dependent Edge (but not orphan) NODES: ");
			for (Map.Entry<String, TitanVertex> entry : missingDepNodeHash
					.entrySet()) {
				try {
					String vid = entry.getKey();
					bw.write("\n>  Missing edge to Dependent Node (but has edges) Vid = "
							+ vid + "\n");
					ArrayList<String> retArr = DbMeth.showPropertiesForNode(
							TRANSID, FROMAPPID, entry.getValue());
					for (String info : retArr) {
						bw.write(info + "\n");
					}
	
					retArr = DbMeth.showAllEdgesForNode(TRANSID, FROMAPPID,
							entry.getValue());
					for (String info : retArr) {
						bw.write(info + "\n");
					}
				} catch (Exception dex) {
					String msg = "error trying to print detail info for a node missing its dependent edge but not an orphan:  ";
					logline.add(msg, dex.getMessage());
					System.out.println(msg);
				}
			}

			bw.write("\n ------------- EDGES pointing to empty/bad vertices: ");
			for (Map.Entry<String, Edge> entry : oneArmedEdgeHash.entrySet()) {
				try {
					String eid = entry.getKey();
					Edge thisE = entry.getValue();
					String badVid = emptyVertexHash.get(eid);
					bw.write("\n>  Edge pointing to bad vertex (Vid = "
							+ badVid + ") EdgeId = " + eid + "\n");
					bw.write("Label: [" + thisE.label() + "]\n");
					Iterator<Property<Object>> pI = thisE.properties();
					while (pI.hasNext()) {
						Property<Object> propKey = pI.next();
						bw.write("Prop: [" + propKey + "], val = ["
								+ propKey.value() + "]\n");
					}
				} catch (Exception pex) {
					String msg = "error trying to print empty/bad vertex data: ";
					logline.add(msg, pex.getMessage());
					System.out.println(msg);
				}
			}

			bw.write("\n ------------- Duplicates: ");
			Iterator<String> dupeIter = dupeGroups.iterator();
			int dupeSetCounter = 0;
			while (dupeIter.hasNext()) {
				dupeSetCounter++;
				String dset = (String) dupeIter.next();

				bw.write("\n --- Duplicate Group # " + dupeSetCounter
						+ " Detail -----------\n");
				try {
					// We expect each line to have at least two vid's, followed
					// by the preferred one to KEEP
					String[] dupeArr = dset.split("\\|");
					ArrayList<String> idArr = new ArrayList<String>();
					int lastIndex = dupeArr.length - 1;
					for (int i = 0; i <= lastIndex; i++) {
						if (i < lastIndex) {
							// This is not the last entry, it is one of the
							// dupes, so we want to show all its info
							bw.write("    >> Duplicate Group # "
									+ dupeSetCounter + "  Node # " + i
									+ " ----\n");
							String vidString = dupeArr[i];
							idArr.add(vidString);
							long longVertId = Long.parseLong(vidString);
							Iterator<Vertex> vtxIterator = graph.vertices(longVertId);
							TitanVertex vtx = null;
							if (vtxIterator.hasNext()) {
								vtx = (TitanVertex)vtxIterator.next();
							}
							ArrayList<String> retArr = DbMeth
									.showPropertiesForNode(TRANSID, FROMAPPID,
											vtx);
							for (String info : retArr) {
								bw.write(info + "\n");
							}

							retArr = DbMeth.showAllEdgesForNode(TRANSID,
									FROMAPPID, vtx);
							for (String info : retArr) {
								bw.write(info + "\n");
							}
						} else {
							// This is the last entry which should tell us if we
							// have a preferred keeper
							String prefString = dupeArr[i];
							if (prefString.equals("KeepVid=UNDETERMINED")) {
								bw.write("\n For this group of duplicates, could not tell which one to keep.\n");
								bw.write(" >>> This group needs to be taken care of with a manual/forced-delete.\n");
							} else {
								// If we know which to keep, then the prefString
								// should look like, "KeepVid=12345"
								String[] prefArr = prefString.split("=");
								if (prefArr.length != 2
										|| (!prefArr[0].equals("KeepVid"))) {
									String msg = "Bad format. Expecting KeepVid=999999";
									System.out.println(msg);
									throw new Exception(msg);
								} else {
									String keepVidStr = prefArr[1];
									if (idArr.contains(keepVidStr)) {
										bw.write("\n The vertex we want to KEEP has vertexId = "
												+ keepVidStr);
										bw.write("\n The others become delete candidates: \n");
										idArr.remove(keepVidStr);
										for (int x = 0; x < idArr.size(); x++) {
											cleanupCandidateCount++;
											bw.write("DeleteCandidate: Duplicate Vid = ["
													+ idArr.get(x) + "]\n");
										}
									} else {
										String msg = "ERROR - Vertex Id to keep not found in list of dupes.  dset = ["
												+ dset + "]";
										System.out.println(msg);
										throw new Exception(msg);
									}
								}
							}// else we know which one to keep
						}// else last entry
					}// for each vertex in a group
				} catch (Exception dex) {
					String msg = "error trying to print duplicate vertex data: ";
					logline.add(msg, dex.getMessage());
					System.out.println(msg);
				}

			}// while - work on each group of dupes

			bw.write("\n ------------- Mis-matched Label/aai-node-type Nodes: \n ");
			for (Map.Entry<String, String> entry : misMatchedHash.entrySet()) {
				String msg = entry.getValue();
				bw.write("MixedMsg = " + msg + "\n");
			}

			bw.write("\n ------------- Got these errors while processing: \n");
			Iterator<String> errIter = errArr.iterator();
			while (errIter.hasNext()) {
				String line = (String) errIter.next();
				bw.write(line + "\n");
			}

			bw.close();

			System.out
					.println("\n ------------- Done doing all the checks ------------ ");
			System.out.println("Output will be written to "
					+ fullOutputFileName);

			if (cleanupCandidateCount > 0) {
				// Technically, this is not an error -- but we're throwing this
				// error so that hopefully a
				// monitoring system will pick it up and do something with it.
				String emsg = "See file: [" + fullOutputFileName
						+ "] and investigate delete candidates. ";
				throw new AAIException("AAI_6123", emsg);
			}

			aaiLogger.info(logline, true, "0");
		} catch (AAIException e) {
			System.out.print("Threw a AAIException: \n");
			System.out.println(e.getErrorObject().toString());
			aaiLogger.error(e.getErrorObject(), logline, e);
			aaiLogger.info(logline, false, e.getErrorObject().getErrorCodeString());
		} catch (Exception ex) {
			System.out.print("Threw a regular Exception:\n");
			System.out.println(ex.getMessage());
			aaiLogger.error(
					ErrorLogHelper.getErrorObject("AAI_6128", ex.getMessage()
							+ ", resolve and rerun dataGrooming"), logline, ex);
			aaiLogger.info(logline, false, "AAI_6128");
		} finally {
			if (g != null) {
				// Any changes that worked correctly should have already done
				// their commits.
				g.rollback();
				graph.close();
			}

			if (bw != null) {
				try {
					bw.close();
				} catch (IOException iox) {
					String emsg = "Got an IOException trying to close bufferedWriter() \n";
					logline.add("emsg", emsg);
				}
			}
			
			if (g != null) {
				// Any changes that worked correctly should have already done
				// their commits.
				try {
					g.rollback();
				} catch (Exception ex) {
					// Don't throw anything because Titan sometimes is just saying that the graph is already closed
					System.out.println("WARNING from final graphTransaction.rollback(): " + ex.getMessage() );
				}
			}
			
			if (g2 != null) {
				// Any changes that worked correctly should have already done
				// their commits.
				try {
					g2.rollback();
				} catch (Exception ex) {
					// Don't throw anything because Titan sometimes is just saying that the graph is already closed
					System.out.println("WARNING from final graphTransaction2.rollback(): " + ex.getMessage() );
				}
			}
				
			if( finalShutdownFlag ){
				try {
					if( graph != null && graph.isOpen() ){
						graph.tx().close();
						graph.close();
					}
				} catch (Exception ex) {
					// Don't throw anything because Titan sometimes is just saying that the graph is already closed{
					System.out.println("WARNING from final graph.shutdown(): " + ex.getMessage() );
				}
				
				try {
					if( graph2 != null && graph2.isOpen() ){
						graph2.tx().close();
						graph2.close();
					}
				} catch (Exception ex) {
					// Don't throw anything because Titan sometimes is just saying that the graph is already closed{
					System.out.println("WARNING from final graph2.shutdown(): " + ex.getMessage() );
				}
			}
				
		}

		return cleanupCandidateCount;

	}// end of doTheGrooming()
	
	
	/**
	 * Vertex has these keys.
	 *
	 * @param tmpV the tmp V
	 * @param propHashWithKeys the prop hash with keys
	 * @return the boolean
	 */
	private static Boolean vertexHasTheseKeys( TitanVertex tmpV, HashMap <String, Object> propHashWithKeys) {
		Iterator <?> it = propHashWithKeys.entrySet().iterator();
		while( it.hasNext() ){
			String propName = "";
			String propVal = "";
			Map.Entry <?,?>propEntry = (Map.Entry<?,?>)it.next();
			Object propNameObj = propEntry.getKey();
			if( propNameObj != null ){
				propName = propNameObj.toString();
			}
			Object propValObj = propEntry.getValue();
			if( propValObj != null ){
				propVal = propValObj.toString();
			}
			Object checkValObj = tmpV.<Object>property(propName).orElse(null);
			if( checkValObj == null ) {
				return false;
			}
			else if( !propVal.equals(checkValObj.toString()) ){
				return false;
			}
		}
		return true;
	}	
	
	
	/**
	 * Any key fields missing.
	 *
	 * @param nType the n type
	 * @param v the v
	 * @return the boolean
	 */
	private static Boolean anyKeyFieldsMissing(String nType, Vertex v) {

		try {
			Collection<String> keyProps = DbMeth.getNodeKeyPropNames(TRANSID,
					FROMAPPID, nType, "junkversion");
			Iterator<String> keyPropI = keyProps.iterator();
			while (keyPropI.hasNext()) {
				String propName = keyPropI.next();
				Object ob = v.<Object>property(propName).orElse(null);
				if (ob == null || ob.toString().equals("")) {
					// It is missing a key property
					return true;
				}
			}
		} catch (AAIException e) {
			// Something was wrong
			return true;
		}
		return false;
	}
	

	/**
	 * Gets the delete list.
	 *
	 * @param targetDir the target dir
	 * @param fileName the file name
	 * @param edgesOnlyFlag the edges only flag
	 * @param dontFixOrphans the dont fix orphans
	 * @param dupeFixOn the dupe fix on
	 * @return the delete list
	 * @throws AAIException the AAI exception
	 */
	private static ArrayList<String> getDeleteList(String targetDir,
			String fileName, Boolean edgesOnlyFlag, Boolean dontFixOrphans,
			Boolean dupeFixOn) throws AAIException {

		// Look in the file for lines formated like we expect - pull out any
		// Vertex Id's to delete on this run
		ArrayList<String> delList = new ArrayList<String>();
		LogLine logline = new LogLine();
		logline.init("aaidbgen", TRANSID, FROMAPPID, "getDeleteList");

		String fullFileName = targetDir + AAIConstants.AAI_FILESEP + fileName;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fullFileName));
			String line = br.readLine();
			while (line != null) {
				if (!line.equals("") && line.startsWith("DeleteCandidate")) {
					if (edgesOnlyFlag && (!line.contains("Bad Edge"))) {
						// We're not going to process edge guys
					} else if (dontFixOrphans && line.contains("Orphan")) {
						// We're not going to process orphans
					} else if (!dupeFixOn && line.contains("Duplicate")) {
						// We're not going to process Duplicates
					} else {
						int begIndex = line.indexOf("id = ");
						int endIndex = line.indexOf("]");
						String vidVal = line.substring(begIndex + 6, endIndex);
						delList.add(vidVal);
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			String emsg = "Could not open input-file [" + fullFileName
					+ "], exception= " + e.getMessage();
			aaiLogger.info(logline, false, "AAI_6124");
			throw new AAIException("AAI_6124", e, emsg);
		}

		aaiLogger.info(logline, true, "0");
		return delList;

	}// end of getDeleteList

	/**
	 * Gets the preferred dupe.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param g the g
	 * @param dupeVertexList the dupe vertex list
	 * @param ver the ver
	 * @return TitanVertex
	 * @throws AAIException the AAI exception
	 */
	public static TitanVertex getPreferredDupe(String transId,
			String fromAppId, TitanTransaction g,
			ArrayList<TitanVertex> dupeVertexList, String ver)
			throws AAIException {

		// This method assumes that it is being passed a List of vertex objects
		// which
		// violate our uniqueness constraints.

		TitanVertex nullVtx = null;
		LogLine logline = new LogLine();
		logline.init("aaidbgen", transId, fromAppId, "getPreferredDupe");

		if (dupeVertexList == null) {
			return nullVtx;
		}
		int listSize = dupeVertexList.size();
		if (listSize == 0) {
			return nullVtx;
		}
		if (listSize == 1) {
			return ((TitanVertex) dupeVertexList.get(0));
		}

		TitanVertex vtxPreferred = null;
		TitanVertex currentFaveVtx = (TitanVertex) dupeVertexList.get(0);
		for (int i = 1; i < listSize; i++) {
			TitanVertex vtxB = (TitanVertex) dupeVertexList.get(i);
			vtxPreferred = pickOneOfTwoDupes(transId, fromAppId, g,
					currentFaveVtx, vtxB, ver);
			if (vtxPreferred == null) {
				// We couldn't choose one
				return nullVtx;
			} else {
				currentFaveVtx = vtxPreferred;
			}
		}

		aaiLogger.info(logline, true, "0");
		return (currentFaveVtx);

	} // end of getPreferredDupe()

	/**
	 * Pick one of two dupes.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param g the g
	 * @param vtxA the vtx A
	 * @param vtxB the vtx B
	 * @param ver the ver
	 * @return TitanVertex
	 * @throws AAIException the AAI exception
	 */
	public static TitanVertex pickOneOfTwoDupes(String transId,
			String fromAppId, TitanTransaction g, TitanVertex vtxA,
			TitanVertex vtxB, String ver) throws AAIException {

		TitanVertex nullVtx = null;
		TitanVertex preferredVtx = null;

		LogLine logline = new LogLine();
		logline.init("aaidbgen", transId, fromAppId, "pickOneOfTwoDupes");

		Long vidA = new Long(vtxA.id().toString());
		Long vidB = new Long(vtxB.id().toString());

		// System.out.println( ">> choosing between vtxId = " + vidA +
		// ", and vtxIdB = " + vidB );

		String vtxANodeType = "";
		String vtxBNodeType = "";
		Object obj = vtxA.<Object>property("aai-node-type").orElse(null);
		if (obj != null) {
			vtxANodeType = obj.toString();
		}
		obj = vtxB.<Object>property("aai-node-type").orElse(null);
		if (obj != null) {
			vtxBNodeType = obj.toString();
		}

		if (vtxANodeType.equals("") || (!vtxANodeType.equals(vtxBNodeType))) {
			// Either they're not really dupes or there's some bad data - so
			// don't pick one
			return nullVtx;
		}

		// Check that node A and B both have the same key values (or else they
		// are not dupes)
		// (We'll check dep-node later)
		Collection<String> keyProps = DbMeth.getNodeKeyPropNames(transId,
				fromAppId, vtxANodeType, ver);
		Iterator<String> keyPropI = keyProps.iterator();
		while (keyPropI.hasNext()) {
			String propName = keyPropI.next();
			String vtxAKeyPropVal = "";
			obj = vtxA.<Object>property(propName).orElse(null);
			if (obj != null) {
				vtxAKeyPropVal = obj.toString();
			}
			String vtxBKeyPropVal = "";
			obj = vtxB.<Object>property(propName).orElse(null);
			if (obj != null) {
				vtxBKeyPropVal = obj.toString();
			}

			if (vtxAKeyPropVal.equals("")
					|| (!vtxAKeyPropVal.equals(vtxBKeyPropVal))) {
				// Either they're not really dupes or they are missing some key
				// data - so don't pick one
				return nullVtx;
			}
		}

		// Collect the vid's and aai-node-types of the vertices that each vertex
		// (A and B) is connected to.
		HashMap emptyHash = new HashMap();
		ArrayList<String> vtxIdsConn2A = new ArrayList<String>();
		ArrayList<String> vtxIdsConn2B = new ArrayList<String>();
		HashMap<String, String> nodeTypesConn2A = new HashMap<String, String>();
		HashMap<String, String> nodeTypesConn2B = new HashMap<String, String>();

		ArrayList<TitanVertex> vertListA = DbMeth.getConnectedNodes("transId",
				"fromAppId", g, "", emptyHash, vtxA, ver, false);
		if (vertListA != null) {
			Iterator<TitanVertex> iter = vertListA.iterator();
			while (iter.hasNext()) {
				TitanVertex tvCon = iter.next();
				String conVid = tvCon.id().toString();
				String nt = "";
				obj = tvCon.<Object>property("aai-node-type").orElse(null);
				if (obj != null) {
					nt = obj.toString();
				}
				nodeTypesConn2A.put(nt, conVid);
				vtxIdsConn2A.add(conVid);
			}
		}

		ArrayList<TitanVertex> vertListB = DbMeth.getConnectedNodes("transId",
				"fromAppId", g, "", emptyHash, vtxB, ver, false);
		if (vertListB != null) {
			Iterator<TitanVertex> iter = vertListB.iterator();
			while (iter.hasNext()) {
				TitanVertex tvCon = iter.next();
				String conVid = tvCon.id().toString();
				String nt = "";
				obj = tvCon.<Object>property("aai-node-type").orElse(null);
				if (obj != null) {
					nt = obj.toString();
				}
				nodeTypesConn2B.put(nt, conVid);
				vtxIdsConn2B.add(conVid);
			}
		}

		// 1 - If this kind of node needs a dependent node for uniqueness, then
		// verify that they both nodes
		// point to the same dependent node (otherwise they're not really
		// duplicates)
		// Note - there are sometimes more than one dependent node type since
		// one nodeType can be used in
		// different ways. But for a particular node, it will only have one
		// dependent node that it's
		// connected to.
		ArrayList<String> depNodeTypes = DbMeth.getDepNodeTypes(transId,
				fromAppId, vtxANodeType, ver);
		if (depNodeTypes.isEmpty()) {
			// This kind of node is not dependent on any other. That is ok.
		} else {
			String depNodeVtxId4A = "";
			String depNodeVtxId4B = "";
			Iterator<String> iter = depNodeTypes.iterator();
			while (iter.hasNext()) {
				String depNodeType = iter.next();
				if (nodeTypesConn2A.containsKey(depNodeType)) {
					// This is the dependent node type that vertex A is using
					depNodeVtxId4A = nodeTypesConn2A.get(depNodeType);
				}
				if (nodeTypesConn2B.containsKey(depNodeType)) {
					// This is the dependent node type that vertex B is using
					depNodeVtxId4B = nodeTypesConn2B.get(depNodeType);
				}
			}
			if (depNodeVtxId4A.equals("")
					|| (!depNodeVtxId4A.equals(depNodeVtxId4B))) {
				// Either they're not really dupes or there's some bad data - so
				// don't pick either one
				return nullVtx;
			}
		}

		if (vtxIdsConn2A.size() == vtxIdsConn2B.size()) {
			// 2 - If they both have edges to all the same vertices, then return
			// the one with the lower vertexId.
			boolean allTheSame = true;
			Iterator<String> iter = vtxIdsConn2A.iterator();
			while (iter.hasNext()) {
				String vtxIdConn2A = iter.next();
				if (!vtxIdsConn2B.contains(vtxIdConn2A)) {
					allTheSame = false;
					break;
				}
			}

			if (allTheSame) {
				if (vidA < vidB) {
					preferredVtx = vtxA;
				} else {
					preferredVtx = vtxB;
				}
			}
		} else if (vtxIdsConn2A.size() > vtxIdsConn2B.size()) {
			// 3 - VertexA is connected to more things than vtxB.
			// We'll pick VtxA if its edges are a superset of vtxB's edges.
			boolean missingOne = false;
			Iterator<String> iter = vtxIdsConn2B.iterator();
			while (iter.hasNext()) {
				String vtxIdConn2B = iter.next();
				if (!vtxIdsConn2A.contains(vtxIdConn2B)) {
					missingOne = true;
					break;
				}
			}
			if (!missingOne) {
				preferredVtx = vtxA;
			}
		} else if (vtxIdsConn2B.size() > vtxIdsConn2A.size()) {
			// 4 - VertexB is connected to more things than vtxA.
			// We'll pick VtxB if its edges are a superset of vtxA's edges.
			boolean missingOne = false;
			Iterator<String> iter = vtxIdsConn2A.iterator();
			while (iter.hasNext()) {
				String vtxIdConn2A = iter.next();
				if (!vtxIdsConn2B.contains(vtxIdConn2A)) {
					missingOne = true;
					break;
				}
			}
			if (!missingOne) {
				preferredVtx = vtxB;
			}
		} else {
			preferredVtx = nullVtx;
		}

		aaiLogger.info(logline, true, "0");
		return (preferredVtx);

	} // end of pickOneOfTwoDupes()

	/**
	 * Check and process dupes.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param g the g
	 * @param version the version
	 * @param nType the n type
	 * @param passedVertList the passed vert list
	 * @param dupeFixOn the dupe fix on
	 * @param deleteCandidateList the delete candidate list
	 * @param singleCommits the single commits
	 * @param alreadyFoundDupeGroups the already found dupe groups
	 * @param dbMaps the db maps
	 * @return the array list
	 */
	private static ArrayList<String> checkAndProcessDupes(String transId,
			String fromAppId, TitanTransaction g, String version, String nType,
			ArrayList<TitanVertex> passedVertList, Boolean dupeFixOn,
			ArrayList<String> deleteCandidateList, Boolean singleCommits,
			ArrayList<String> alreadyFoundDupeGroups, DbMaps dbMaps ) {
		
		ArrayList<String> returnList = new ArrayList<String>();
		ArrayList<TitanVertex> checkVertList = new ArrayList<TitanVertex>();
		ArrayList<String> alreadyFoundDupeVidArr = new ArrayList<String>();
		Boolean noFilterList = true;
		Iterator<String> afItr = alreadyFoundDupeGroups.iterator();
		while (afItr.hasNext()) {
			String dupeGrpStr = afItr.next();
			String[] dupeArr = dupeGrpStr.split("\\|");
			int lastIndex = dupeArr.length - 1;
			for (int i = 0; i < lastIndex; i++) {
				// Note: we don't want the last one...
				String vidString = dupeArr[i];
				alreadyFoundDupeVidArr.add(vidString);
				noFilterList = false;
			}
		}

		// For a given set of Nodes that were found with a set of KEY
		// Parameters, (nodeType + key data) we will
		// see if we find any duplicate nodes that need to be cleaned up. Note -
		// it's legit to have more than one
		// node with the same key data if the nodes depend on a parent for
		// uniqueness -- as long as the two nodes
		// don't hang off the same Parent.
		// If we find duplicates, and we can figure out which of each set of
		// duplicates is the one that we
		// think should be preserved, we will record that. Whether we can tell
		// which one should be
		// preserved or not, we will return info about any sets of duplicates
		// found.
		//
		// Each element in the returned arrayList might look like this:
		// "1234|5678|keepVid=UNDETERMINED" (if there were 2 dupes, and we
		// couldn't figure out which one to keep)
		// or, "100017|200027|30037|keepVid=30037" (if there were 3 dupes and we
		// thought the third one was the one that should survive)

		// Because of the way the calling code loops over stuff, we can get the
		// same data multiple times - so we should
		// not process any vertices that we've already seen.

		try {
			Iterator<TitanVertex> pItr = passedVertList.iterator();
			while (pItr.hasNext()) {
				TitanVertex tvx = (TitanVertex) pItr.next();
				String passedId = tvx.id().toString();
				if (noFilterList || !alreadyFoundDupeVidArr.contains(passedId)) {
					// We haven't seen this one before - so we should check it.
					checkVertList.add(tvx);
				}
			}

			if (checkVertList.size() < 2) {
				// Nothing new to check.
				return returnList;
			}

			if (!dbMaps.NodeDependencies.containsKey(nType)) {
				// If this was a node that does NOT depend on other nodes for
				// uniqueness, and we
				// found more than one node using its key -- record the found
				// vertices as duplicates.
				String dupesStr = "";
				for (int i = 0; i < checkVertList.size(); i++) {
					dupesStr = dupesStr
							+ ((TitanVertex) (checkVertList.get(i))).id()
									.toString() + "|";
				}
				if (dupesStr != "") {
					TitanVertex prefV = getPreferredDupe(transId, fromAppId,
							g, checkVertList, version);
					if (prefV == null) {
						// We could not determine which duplicate to keep
						dupesStr = dupesStr + "KeepVid=UNDETERMINED";
						returnList.add(dupesStr);
					} else {
						dupesStr = dupesStr + "KeepVid=" + prefV.id();
						Boolean didRemove = false;
						if (dupeFixOn) {
							didRemove = deleteNonKeepersIfAppropriate(g,
									dupesStr, prefV.id().toString(),
									deleteCandidateList, singleCommits);
						}
						if (didRemove) {
							dupeGrpsDeleted++;
						} else {
							// keep them on our list
							returnList.add(dupesStr);
						}
					}
				}
			} else {
				// More than one node have the same key fields since they may
				// depend on a parent node for
				// uniqueness. Since we're finding more than one, we want to
				// check to see if any of the
				// vertices that have this set of keys are also pointing at the
				// same 'parent' node.
				// Note: for a given set of key data, it is possible that there
				// could be more than one set of
				// duplicates.
				HashMap<String, ArrayList<TitanVertex>> vertsGroupedByParentHash = groupVertsByDepNodes(
						transId, fromAppId, g, version, nType,
						checkVertList, dbMaps);
				for (Map.Entry<String, ArrayList<TitanVertex>> entry : vertsGroupedByParentHash
						.entrySet()) {
					ArrayList<TitanVertex> thisParentsVertList = entry
							.getValue();
					if (thisParentsVertList.size() > 1) {
						// More than one vertex found with the same key info
						// hanging off the same parent/dependent node
						String dupesStr = "";
						for (int i = 0; i < thisParentsVertList.size(); i++) {
							dupesStr = dupesStr
									+ ((TitanVertex) (thisParentsVertList
											.get(i))).id() + "|";
						}
						if (dupesStr != "") {
							TitanVertex prefV = getPreferredDupe(transId,
									fromAppId, g, thisParentsVertList,
									version);

							if (prefV == null) {
								// We could not determine which duplicate to
								// keep
								dupesStr = dupesStr + "KeepVid=UNDETERMINED";
								returnList.add(dupesStr);
							} else {
								Boolean didRemove = false;
								dupesStr = dupesStr + "KeepVid="
										+ prefV.id().toString();
								if (dupeFixOn) {
									didRemove = deleteNonKeepersIfAppropriate(
											g, dupesStr, prefV.id()
													.toString(),
											deleteCandidateList, singleCommits);
								}
								if (didRemove) {
									dupeGrpsDeleted++;
								} else {
									// keep them on our list
									returnList.add(dupesStr);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out
					.println(" >>> Threw an error in checkAndProcessDupes - just absorb this error and move on. "
							+ e.getMessage());
		}

		return returnList;

	}// End of checkAndProcessDupes()

	/**
	 * Group verts by dep nodes.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param g the g
	 * @param version the version
	 * @param nType the n type
	 * @param passedVertList the passed vert list
	 * @param dbMaps the db maps
	 * @return the hash map
	 * @throws AAIException the AAI exception
	 */
	private static HashMap<String, ArrayList<TitanVertex>> groupVertsByDepNodes(
			String transId, String fromAppId, TitanTransaction g, String version,
			String nType, ArrayList<TitanVertex> passedVertList, DbMaps dbMaps)
			throws AAIException {
		// Given a list of Titan Vertices, group them together by dependent
		// nodes. Ie. if given a list of
		// ip address nodes (assumed to all have the same key info) they might
		// sit under several different parent vertices.
		// Under Normal conditions, there would only be one per parent -- but
		// we're trying to find duplicates - so we
		// allow for the case where more than one is under the same parent node.

		HashMap<String, ArrayList<TitanVertex>> retHash = new HashMap<String, ArrayList<TitanVertex>>();
		if (!dbMaps.NodeDependencies.containsKey(nType)) {
			// This method really should not have been called if this is not the
			// kind of node
			// that depends on a parent for uniqueness, so just return the empty
			// hash.
			return retHash;
		}

		// Find out what types of nodes the passed in nodes can depend on
		ArrayList<String> depNodeTypeL = new ArrayList<String>();
		Collection<String> depNTColl = dbMaps.NodeDependencies.get(nType);
		Iterator<String> ntItr = depNTColl.iterator();
		while (ntItr.hasNext()) {
			depNodeTypeL.add(ntItr.next());
		}
		// For each vertex, we want find its dependent vertex and add it to
		// other vertexes that are dependent on that same guy.
		if (passedVertList != null) {
			HashMap<String, Object> emptyHash = new HashMap<String, Object>();
			Iterator<TitanVertex> iter = passedVertList.iterator();
			while (iter.hasNext()) {
				TitanVertex thisVert = iter.next();
				ArrayList<TitanVertex> connectedVList = DbMeth
						.getConnectedNodes("transId", "fromAppId", g, "",
								emptyHash, thisVert, "v3", false);
				Iterator<TitanVertex> connIter = connectedVList.iterator();
				while (connIter.hasNext()) {
					TitanVertex tvCon = connIter.next();
					String conNt = "";
					Object obj = tvCon.<Object>property("aai-node-type").orElse(null);
					if (obj != null) {
						conNt = obj.toString();
					}
					if (depNTColl.contains(conNt)) {
						// This must be the parent/dependent node
						String parentVid = tvCon.id().toString();
						if (retHash.containsKey(parentVid)) {
							// add this vert to the list for this parent key
							((ArrayList) (retHash.get(parentVid)))
									.add(thisVert);
						} else {
							// This is the first one we found on this parent
							ArrayList<TitanVertex> vList = new ArrayList<TitanVertex>();
							vList.add(thisVert);
							retHash.put(parentVid, vList);
						}
					}
				}
			}
		}

		return retHash;

	}// end of groupVertsByDepNodes()

	/**
	 * Delete non keepers if appropriate.
	 *
	 * @param g the g
	 * @param dupeInfoString the dupe info string
	 * @param vidToKeep the vid to keep
	 * @param deleteCandidateList the delete candidate list
	 * @param singleCommits the single commits
	 * @return the boolean
	 */
	private static Boolean deleteNonKeepersIfAppropriate(TitanTransaction g,
			String dupeInfoString, String vidToKeep,
			ArrayList<String> deleteCandidateList, Boolean singleCommits) {

		Boolean deletedSomething = false;
		// This assumes that the dupeInfoString is in the format of
		// pipe-delimited vid's followed by
		// ie. "3456|9880|keepVid=3456"
		if (deleteCandidateList == null || deleteCandidateList.size() == 0) {
			// No vid's on the candidate list -- so no deleting will happen on
			// this run
			return false;
		}

		String[] dupeArr = dupeInfoString.split("\\|");
		ArrayList<String> idArr = new ArrayList<String>();
		int lastIndex = dupeArr.length - 1;
		for (int i = 0; i <= lastIndex; i++) {
			if (i < lastIndex) {
				// This is not the last entry, it is one of the dupes,
				String vidString = dupeArr[i];
				idArr.add(vidString);
			} else {
				// This is the last entry which should tell us if we have a
				// preferred keeper
				String prefString = dupeArr[i];
				if (prefString.equals("KeepVid=UNDETERMINED")) {
					// They sent us a bad string -- nothing should be deleted if
					// no dupe could be tagged as preferred
					return false;
				} else {
					// If we know which to keep, then the prefString should look
					// like, "KeepVid=12345"
					String[] prefArr = prefString.split("=");
					if (prefArr.length != 2 || (!prefArr[0].equals("KeepVid"))) {
						String msg = "Bad format. Expecting KeepVid=999999";
						System.out.println(msg);
						return false;
					} else {
						String keepVidStr = prefArr[1];
						if (idArr.contains(keepVidStr)) {
							idArr.remove(keepVidStr);

							// So now, the idArr should just contain the vid's
							// that we want to remove.
							for (int x = 0; x < idArr.size(); x++) {
								boolean okFlag = true;
								String thisVid = idArr.get(x);
								if (deleteCandidateList.contains(thisVid)) {
									// This vid is a valid delete candidate from
									// a prev. run, so we can remove it.
									try {
										long longVertId = Long
												.parseLong(thisVid);
										TitanVertex vtx = g
												.getVertex(longVertId);
										vtx.remove();
										if (singleCommits) {
											g.commit();
											g = AAIGraph.getInstance().getGraph().newTransaction();
										}
									} catch (Exception e) {
										okFlag = false;
										String msg = "ERROR trying to delete VID = "
												+ thisVid;
										System.out.println(msg);
									}
									if (okFlag) {
										System.out.println(" DELETED VID = "
												+ thisVid);
										deletedSomething = true;
									}
								}
							}
						} else {
							String msg = "ERROR - Vertex Id to keep not found in list of dupes.  dupeInfoString = ["
									+ dupeInfoString + "]";
							System.out.println(msg);
							return false;
						}
					}
				}// else we know which one to keep
			}// else last entry
		}// for each vertex in a group

		return deletedSomething;

	}// end of deleteNonKeepersIfAppropriate()

	
	/**
	 * Gets the node just using key params.
	 *
	 * @param transId the trans id
	 * @param fromAppId the from app id
	 * @param graph the graph
	 * @param nodeType the node type
	 * @param keyPropsHash the key props hash
	 * @param apiVersion the api version
	 * @return the node just using key params
	 * @throws AAIException the AAI exception
	 */
	public static ArrayList <TitanVertex> getNodeJustUsingKeyParams( String transId, String fromAppId, TitanTransaction graph, String nodeType,
			HashMap<String,Object> keyPropsHash, String apiVersion ) 	 throws AAIException{
		
		ArrayList <TitanVertex> retVertList = new ArrayList <TitanVertex> ();
		
		// We assume that all NodeTypes have at least one key-property defined.  
		// Note - instead of key-properties (the primary key properties), a user could pass
		//        alternate-key values if they are defined for the nodeType.
		ArrayList<String> kName = new ArrayList<String>();
		ArrayList<Object> kVal = new ArrayList<Object>();
		if( keyPropsHash == null || keyPropsHash.isEmpty() ){
			String msg = " NO key properties passed for this getNodeJustUsingKeyParams() request.  NodeType = [" + nodeType + "]. ";
			System.out.println( msg );
			throw new AAIException("AAI_6120", msg); 
		}
		
		int i = -1;
		for( Map.Entry<String, Object> entry : keyPropsHash.entrySet() ){
			i++;
			kName.add(i, entry.getKey());
			kVal.add(i, entry.getValue());
		}
		int topPropIndex = i;
		TitanVertex tiV = null;
		String propsAndValuesForMsg = "";
		Iterable <?> verts = null;

		try { 
			if( topPropIndex == 0 ){
				propsAndValuesForMsg = " (" + kName.get(0) + " = " + kVal.get(0) + ") ";
				verts= graph.query().has(kName.get(0),kVal.get(0)).has("aai-node-type",nodeType).vertices();	
			}	
			else if( topPropIndex == 1 ){
				propsAndValuesForMsg = " (" + kName.get(0) + " = " + kVal.get(0) + ", " 
						+ kName.get(1) + " = " + kVal.get(1) + ") ";
				verts =  graph.query().has(kName.get(0),kVal.get(0)).has(kName.get(1),kVal.get(1)).has("aai-node-type",nodeType).vertices();	
			}	 		
			else if( topPropIndex == 2 ){
				propsAndValuesForMsg = " (" + kName.get(0) + " = " + kVal.get(0) + ", " 
						+ kName.get(1) + " = " + kVal.get(1) + ", " 
						+ kName.get(2) + " = " + kVal.get(2) +  ") ";
				verts= graph.query().has(kName.get(0),kVal.get(0)).has(kName.get(1),kVal.get(1)).has(kName.get(2),kVal.get(2)).has("aai-node-type",nodeType).vertices();			
			}	
			else if( topPropIndex == 3 ){
				propsAndValuesForMsg = " (" + kName.get(0) + " = " + kVal.get(0) + ", " 
						+ kName.get(1) + " = " + kVal.get(1) + ", " 
						+ kName.get(2) + " = " + kVal.get(2) + ", " 
						+ kName.get(3) + " = " + kVal.get(3) +  ") ";
				verts= graph.query().has(kName.get(0),kVal.get(0)).has(kName.get(1),kVal.get(1)).has(kName.get(2),kVal.get(2)).has(kName.get(3),kVal.get(3)).has("aai-node-type",nodeType).vertices();			
			}	 		
			else {
				String emsg = " We only support 4 keys per nodeType for now \n";
				throw new AAIException("AAI_6114", emsg); 
			}
		}
		catch( Exception ex ){
			System.out.println( " ERROR trying to get node for: [" + propsAndValuesForMsg + "]");
		}

		if( verts != null ){
			Iterator <?> vertI = verts.iterator();
			while( vertI.hasNext() ){
				tiV = (TitanVertex) vertI.next();
				retVertList.add(tiV);
			}
		}
		
		if( retVertList.size() == 0 ){
			System.out.println("DEBUG No node found for nodeType = [" + nodeType +
					"], propsAndVal = " + propsAndValuesForMsg );
		}
		
		return retVertList;
		
	}// End of getNodeJustUsingKeyParams() 
	
	
	
}
