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

package org.openecomp.aai.db.schema;

import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.schema.SchemaStatus;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.core.schema.TitanManagement.IndexBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.openecomp.aai.introspection.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageTitanSchema {

    private static final Logger log = LoggerFactory.getLogger(ManageTitanSchema.class);
    private TitanManagement graphMgmt;
    private TitanGraph graph;
    private List<DBProperty> aaiProperties;
    private List<DBIndex> aaiIndexes;
    private List<EdgeProperty> aaiEdgeProperties;
    private Auditor oxmInfo = null;
    private Auditor graphInfo = null;

    /**
     * Instantiates a new manage titan schema.
     *
     * @param graph the graph
     */
    public ManageTitanSchema(final TitanGraph graph) {
        this.graph = graph;
        oxmInfo = AuditorFactory.getOXMAuditor(Version.v8);
        graphInfo = AuditorFactory.getGraphAuditor(graph);
    }


    /**
     * Builds the schema.
     */
    public void buildSchema() {

        this.graphMgmt = graph.openManagement();
        aaiProperties = new ArrayList<>();
        aaiEdgeProperties = new ArrayList<>();
        aaiIndexes = new ArrayList<>();
        aaiProperties.addAll(oxmInfo.getAuditDoc().getProperties());
        aaiIndexes.addAll(oxmInfo.getAuditDoc().getIndexes());
        aaiEdgeProperties.addAll(oxmInfo.getAuditDoc().getEdgeLabels());
        try {
            createPropertyKeys();
            createIndexes();
            createEdgeLabels();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            graphMgmt.rollback();
        }
        graphMgmt.commit();
    }

    /**
     * Creates the property keys.
     */
    private void createPropertyKeys() {

        for (DBProperty prop : aaiProperties) {

            if (graphMgmt.containsPropertyKey(prop.getName())) {
                PropertyKey key = graphMgmt.getPropertyKey(prop.getName());
                boolean isChanged = false;
                if (!prop.getCardinality().equals(key.cardinality())) {
                    isChanged = true;
                }
                if (!prop.getTypeClass().equals(key.dataType())) {
                    isChanged = true;
                }
                if (isChanged) {
                    //must modify!
                    this.replaceProperty(prop);
                }
            } else {
                //create a new property key
                log.info(String.format("Key: %s not found - adding", prop.getName()));
                graphMgmt.makePropertyKey(prop.getName()).dataType(prop.getTypeClass())
                    .cardinality(prop.getCardinality()).make();
            }
        }
    }

    /**
     * Creates the indexes.
     */
    private void createIndexes() {

        for (DBIndex index : aaiIndexes) {
            Set<DBProperty> props = index.getProperties();
            boolean isChanged = false;
            boolean isNew = false;
            List<PropertyKey> keyList = new ArrayList<>();
            for (DBProperty prop : props) {
                keyList.add(graphMgmt.getPropertyKey(prop.getName()));
            }
            if (graphMgmt.containsGraphIndex(index.getName())) {
                isChanged = handleTitanGraphIndex(index, keyList);
            } else {
                isNew = true;
            }
            if (!keyList.isEmpty()) {
                this.createIndex(graphMgmt, index.getName(), keyList, index.isUnique(), isNew, isChanged);
            }
        }
    }

    private boolean handleTitanGraphIndex(DBIndex index, List<PropertyKey> keyList) {
        boolean isChanged = false;
        TitanGraphIndex titanIndex = graphMgmt.getGraphIndex(index.getName());
        PropertyKey[] dbKeys = titanIndex.getFieldKeys();
        if (dbKeys.length != keyList.size()) {
            isChanged = true;
        } else {
            int i = 0;
            for (PropertyKey key : keyList) {
                if (!dbKeys[i].equals(key)) {
                    isChanged = true;
                    break;
                }
                i++;
            }
        }
        return isChanged;
    }

    // Use EdgeRules to make sure edgeLabels are defined in the db.  NOTE: the multiplicty used here is
    // always "MULTI".  This is not the same as our internal "Many2Many", "One2One", "One2Many" or "Many2One"
    // We use the same edge-label for edges between many different types of nodes and our internal
    // multiplicty definitions depends on which two types of nodes are being connected.

    /**
     * Creates the edge labels.
     */
    private void createEdgeLabels() {

        for (EdgeProperty prop : aaiEdgeProperties) {

            if (graphMgmt.containsEdgeLabel(prop.getName())) {
                // see what changed
            } else {
                graphMgmt.makeEdgeLabel(prop.getName()).multiplicity(prop.getMultiplicity()).make();
            }
        }
    }

    /**
     * Creates the property.
     *
     * @param mgmt the mgmt
     * @param prop the prop
     */
    private void createProperty(TitanManagement mgmt, DBProperty prop) {
        if (mgmt.containsPropertyKey(prop.getName())) {
            PropertyKey key = mgmt.getPropertyKey(prop.getName());
            boolean isChanged = false;
            if (!prop.getCardinality().equals(key.cardinality())) {
                isChanged = true;
            }
            if (!prop.getTypeClass().equals(key.dataType())) {
                isChanged = true;
            }
            if (isChanged) {
                //must modify!
                this.replaceProperty(prop);
            }
        } else {
            //create a new property key
            log.info(String.format("Key: %s not found - adding", prop.getName()));
            mgmt.makePropertyKey(prop.getName()).dataType(prop.getTypeClass()).cardinality(prop.getCardinality())
                .make();
        }
    }

    /**
     * Creates the index.
     *
     * @param mgmt the mgmt
     * @param indexName the index name
     * @param keys the keys
     * @param isUnique the is unique
     * @param isNew the is new
     * @param isChanged the is changed
     */
    private void createIndex(TitanManagement mgmt, String indexName, List<PropertyKey> keys, boolean isUnique,
        boolean isNew, boolean isChanged) {

		/*if (isChanged) {
            log.info("Changing index: " + indexName);
			TitanGraphIndex oldIndex = mgmt.getGraphIndex(indexName);
			mgmt.updateIndex(oldIndex, SchemaAction.DISABLE_INDEX);
			mgmt.commit();
			//cannot remove indexes
			//graphMgmt.updateIndex(oldIndex, SchemaAction.REMOVE_INDEX);
		}*/
        if (isNew || isChanged) {

            if (isNew) {
                IndexBuilder builder = mgmt.buildIndex(indexName, Vertex.class);
                for (PropertyKey k : keys) {
                    builder.addKey(k);
                }
                if (isUnique) {
                    builder.unique();
                }
                builder.buildCompositeIndex();
                log.info(String.format("Built index for %s with keys: %s", indexName, keys));

                //mgmt.commit();
            }

            //mgmt = graph.getManagementSystem();
            //mgmt.updateIndex(mgmt.getGraphIndex(indexName), SchemaAction.REGISTER_INDEX);
            //mgmt.commit();

            try {
                //waitForCompletion(indexName);
                //TitanIndexRepair.hbaseRepair(AAIConstants.AAI_CONFIG_FILENAME, indexName, "");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                graph.tx().rollback();
                graph.close();
                log.error(e.getLocalizedMessage(), e);
            }

            //mgmt = graph.getManagementSystem();
            //mgmt.updateIndex(mgmt.getGraphIndex(indexName), SchemaAction.REINDEX);

            //mgmt.updateIndex(mgmt.getGraphIndex(indexName), SchemaAction.ENABLE_INDEX);

            //mgmt.commit();

        }
    }

    /**
     * Wait for completion.
     *
     * @param name the name
     * @throws InterruptedException the interrupted exception
     */
    private void waitForCompletion(String name) throws InterruptedException {

        boolean registered = false;
        long before = System.currentTimeMillis();
        while (!registered) {
            Thread.sleep(500L);
            TitanManagement mgmt = graph.openManagement();
            TitanGraphIndex idx = mgmt.getGraphIndex(name);
            registered = true;
            for (PropertyKey k : idx.getFieldKeys()) {
                SchemaStatus s = idx.getIndexStatus(k);
                registered &= s.equals(SchemaStatus.REGISTERED);
            }
            mgmt.rollback();
        }
        log.info("Index REGISTERED in " + (System.currentTimeMillis() - before) + " ms");
    }

    /**
     * Replace property.
     *
     * @param key the key
     */
    private void replaceProperty(DBProperty key) {

    }

    /**
     * Update index.
     *
     * @param index the index
     */
    public void updateIndex(DBIndex index) {

        TitanManagement mgmt = graph.openManagement();
        List<PropertyKey> keys = new ArrayList<>();
        boolean isNew = false;
        boolean isChanged = false;
        for (DBProperty prop : index.getProperties()) {
            createProperty(mgmt, prop);
            keys.add(mgmt.getPropertyKey(prop.getName()));
        }
        if (mgmt.containsGraphIndex(index.getName())) {
            log.info("index already exists");
            isNew = false;
            isChanged = true;
        } else {
            isNew = true;
            isChanged = false;
        }
        this.createIndex(mgmt, index.getName(), keys, index.isUnique(), isNew, isChanged);

        mgmt.commit();
    }
}
