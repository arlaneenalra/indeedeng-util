package com.indeed.util.core;

/**
 * Simple interface used by quicksort-type algorithms
 * good balance between simplicity and performance
 * 
 * I made this so I can work with
 * arrays of primitives and
 * parallel arrays more easily
 * 
 * @author ahudson
 */
public interface Quicksortable {
    public void swap(int i, int j);
    public int compare(int i, int j);
}
