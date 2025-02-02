/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.jackrabbit.oak.commons.properties.SystemPropertySupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public abstract class AbstractDocumentStoreTest {

    protected String dsname;
    protected DocumentStore ds;
    protected DocumentStoreFixture dsf;
    protected DataSource rdbDataSource;
    protected List<String> removeMe = new ArrayList<String>();
    protected List<String> removeMeSettings = new ArrayList<String>();
    protected List<String> removeMeJournal = new ArrayList<String>();
    protected List<String> removeMeClusterNodes = new ArrayList<String>();

    static final Logger LOG = LoggerFactory.getLogger(AbstractDocumentStoreTest.class);

    private static final boolean SKIP_MONGO = SystemPropertySupplier.create("oak.skipMongo", false).loggingTo(LOG).get();

    public AbstractDocumentStoreTest(DocumentStoreFixture dsf) {
        this.dsf = dsf;
        this.ds = dsf.createDocumentStore(getBuilder().setClusterId(1));
        this.dsname = dsf.getName();
        this.rdbDataSource = dsf.getRDBDataSource();
    }

    public DocumentMK.Builder getBuilder() {
        return new DocumentMK.Builder();
    }

    @Before
    public void startUp() throws Exception {
        logNodesPresent(true);
    }

    @After
    public void cleanUp() throws Exception {
        removeTestNodes(org.apache.jackrabbit.oak.plugins.document.Collection.NODES, removeMe);
        removeTestNodes(org.apache.jackrabbit.oak.plugins.document.Collection.SETTINGS, removeMeSettings);
        removeTestNodes(org.apache.jackrabbit.oak.plugins.document.Collection.JOURNAL, removeMeJournal);
        removeTestNodes(org.apache.jackrabbit.oak.plugins.document.Collection.CLUSTER_NODES, removeMeClusterNodes);
        ds.dispose();
        dsf.dispose();
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> fixtures() {
        return fixtures(false);
    }

    protected static Collection<Object[]> fixtures(boolean multi) {
        Collection<Object[]> result = new ArrayList<>();
        Collection<String> names = new ArrayList<>();

        DocumentStoreFixture candidates[] = new DocumentStoreFixture[] { DocumentStoreFixture.MEMORY, DocumentStoreFixture.MONGO,
                DocumentStoreFixture.RDB_H2, DocumentStoreFixture.RDB_DERBY, DocumentStoreFixture.RDB_PG,
                DocumentStoreFixture.RDB_DB2, DocumentStoreFixture.RDB_MYSQL, DocumentStoreFixture.RDB_ORACLE,
                DocumentStoreFixture.RDB_MSSQL };

        for (DocumentStoreFixture dsf : candidates) {
            if (SKIP_MONGO && dsf instanceof DocumentStoreFixture.MongoFixture) {
                LOG.info("Mongo fixture '{}' skipped.", dsf.getName());
            } else if (dsf.isAvailable()) {
                if (!multi || dsf.hasSinglePersistence()) {
                    result.add(new DocumentStoreFixture[] { dsf });
                    names.add(dsf.getName());
                }
            }
        }

        LOG.info("Running document store test with fixtures {}.", names);

        return result;
    }

    /**
     * Generate a random string of given size, with or without non-ASCII characters.
     */
    public static String generateString(int length, boolean asciiOnly) {
        char[] s = new char[length];
        for (int i = 0; i < length; i++) {
            if (asciiOnly) {
                s[i] = (char) (32 + (int) (95 * Math.random()));
            } else {
                s[i] = (char) (32 + (int) ((0xd7ff - 32) * Math.random()));
            }
        }
        return new String(s);
    }

    /**
     * Generate a constant string of given size, with or without non-ASCII characters.
     */
    public static String generateConstantString(int length) {
        char[] s = new char[length];
        for (int i = 0; i < length; i++) {
            s[i] = (char)('0' + (i % 10));
        }
        return new String(s);
    }

    private <T extends Document> void removeTestNodes(org.apache.jackrabbit.oak.plugins.document.Collection<T> col,
            List<String> ids) {

        if (!ids.isEmpty()) {
            long start = System.nanoTime();
            try {
                ds.remove(col, ids);
            } catch (Exception ex) {
                // retry one by one
                for (String id : ids) {
                    try {
                        ds.remove(col, id);
                    } catch (Exception ex2) {
                        // best effort
                    }
                }
            }
            if (ids.size() > 1) {
                long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                float rate = (((float) ids.size()) / (elapsed == 0 ? 1 : elapsed));
                LOG.info(ids.size() + " documents removed in " + elapsed + "ms (" + rate + "/ms)");
            }
        }

        logNodesPresent(false);
    }

    /**
     * Check for presence of entries in NODE table
     * @param before
     */
    private void logNodesPresent(boolean before) {
        List<NodeDocument> nodes = ds.query(org.apache.jackrabbit.oak.plugins.document.Collection.NODES, "\u0000", "\uFFFF", 10);
        if (!nodes.isEmpty()) {
            LOG.info("At least {} document(s) present in '{}' NODES collection {} test {}: {}", nodes.size(), dsf,
                    before ? "before" : "after",
                    this.getClass().getName().replace("org.apache.jackrabbit.oak.plugins.document.", "o.a.j.o.o.d.")
                            .replace("org.apache.jackrabbit.oak.", "o.a.j.o."),
                    nodes);
        }
    }
}
