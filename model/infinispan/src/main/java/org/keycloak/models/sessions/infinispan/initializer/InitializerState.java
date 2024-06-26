/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.sessions.infinispan.initializer;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.jboss.logging.Logger;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Note that this state is <b>NOT</b> thread safe. Currently it is only used from single thread so it's fine
 * but further optimizations might need to revisit this (see {@link InfinispanCacheInitializer}).
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.INITIALIZER_STATE)
public class InitializerState extends SessionEntity {

    private static final Logger log = Logger.getLogger(InitializerState.class);

    private final int segmentsCount;
    private final BitSet segments;

    public InitializerState(int segmentsCount) {
        this.segmentsCount = segmentsCount;
        this.segments = new BitSet(segmentsCount);

        log.debugf("segmentsCount: %d", segmentsCount);
    }

    @ProtoFactory
    InitializerState(String realmId, int segmentsCount, BitSet segments) {
        super(realmId);
        this.segmentsCount = segmentsCount;
        this.segments = segments;

        log.debugf("segmentsCount: %d", segmentsCount);
    }

    /**
     * Getter for the segments count.
     * @return The number of segments of the state
     */
    @ProtoField(2)
    public int getSegmentsCount() {
        return segmentsCount;
    }

    @ProtoField(3)
    BitSet getSegments() {
        return segments;
    }

    /** Return true just if computation is entirely finished (all segments are true) */
    public boolean isFinished() {
        return segments.cardinality() == segmentsCount;
    }

    /** Return indication of progress - changes upon progress */
    public int getProgressIndicator() {
        return segments.hashCode();
    }

    /** Return next un-finished segments in the next row of segments.
     * @param segmentToLoad The segment we are loading
     * @param maxSegmentCount The max segment to load
     * @return The list of segments to work on this step
     */
    public List<Integer> getSegmentsToLoad(int segmentToLoad, int maxSegmentCount) {
        List<Integer> result = new LinkedList<>();
        for (int i = segmentToLoad; i < (segmentToLoad + maxSegmentCount) && i < segmentsCount; i++) {
            if (!segments.get(i)) {
                result.add(i);
            }
        }
        return result;
    }

    public void markSegmentFinished(int index) {
        segments.set(index);
    }

    @Override
    public String toString() {
        int finished = segments.cardinality();
        int nonFinished = segmentsCount - finished;

        return "finished segments count: " + finished
          + (", non-finished segments count: " + nonFinished);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.segmentsCount;
        hash = 97 * hash + Objects.hashCode(this.segments);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InitializerState other = (InitializerState) obj;
        return this.segmentsCount == other.segmentsCount && Objects.equals(this.segments, other.segments);
    }

}
