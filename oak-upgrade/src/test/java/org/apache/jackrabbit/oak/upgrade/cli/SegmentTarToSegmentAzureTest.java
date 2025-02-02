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

package org.apache.jackrabbit.oak.upgrade.cli;

import java.io.IOException;

import org.apache.jackrabbit.oak.blob.cloud.azure.blobstorage.AzuriteDockerRule;
import org.apache.jackrabbit.oak.upgrade.cli.container.NodeStoreContainer;
import org.apache.jackrabbit.oak.upgrade.cli.container.SegmentAzureNodeStoreContainer;
import org.apache.jackrabbit.oak.upgrade.cli.container.SegmentTarNodeStoreContainer;
import org.junit.ClassRule;

public class SegmentTarToSegmentAzureTest extends AbstractOak2OakTest {

    private final NodeStoreContainer source;

    private final NodeStoreContainer destination;

    @ClassRule
    public static AzuriteDockerRule azurite = new AzuriteDockerRule();

    public SegmentTarToSegmentAzureTest() throws IOException {
        source = new SegmentTarNodeStoreContainer();
        destination = new SegmentAzureNodeStoreContainer(azurite);
    }

    @Override
    protected NodeStoreContainer getSourceContainer() {
        return source;
    }

    @Override
    protected NodeStoreContainer getDestinationContainer() {
        return destination;
    }

    @Override
    protected String[] getArgs() {
        return new String[] { source.getDescription(), destination.getDescription() };
    }

    @Override
    protected boolean supportsCheckpointMigration() {
        return true;
    }
}
