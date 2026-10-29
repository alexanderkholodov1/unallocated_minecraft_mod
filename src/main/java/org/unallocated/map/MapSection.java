package org.unallocated.map;

/** A section owns only its layout box; offsets are relative to its named layout origin. */
public interface MapSection {
    String id();
    void build(MapWriter blocks);
}
