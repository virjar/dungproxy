package com.virjar.dungproxy.server.core.beanmapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Pylon bean mapper, which give out the ability to support java object mapping
 * 
 * @author weijia.deng
 */
public interface BeanMapper {
    /**
     * Maps the properties of <code>sourceObject</code> onto <code>destinationObject</code>.
     * 
     * @param sourceObject the object from which to read the properties
     * @param destinationObject the object onto which the properties should be mapped
     */
    <S, D> D map(S sourceObject, Class<D> destinationClass);

    /**
     * Maps the properties of <code>sourceObject</code> onto <code>destinationObject</code>.
     * 
     * @param sourceObject the object from which to read the properties
     * @param destinationObject the object onto which the properties should be mapped
     */
    <S, D> void map(S sourceObject, D destinationObject);

    /**
     * Maps the source iterable into a new Set parameterized by <code>destinationClass</code>.
     * 
     * @param source the Iterable from which to map
     * @param destinationClass the type of elements to be contained in the returned Set.
     * @return a new Set containing elements of type <code>destinationClass</code> mapped from the elements of
     *         <code>source</code>.
     */
    <S, D> Set<D> mapAsSet(Iterable<S> source, Class<D> destinationClass);

    /**
     * Maps the source Array into a new Set parameterized by <code>destinationClass</code>.
     * 
     * @param source the Array from which to map
     * @param destinationClass the type of elements to be contained in the returned Set.
     * @return a new Set containing elements of type <code>destinationClass</code> mapped from the elements of
     *         <code>source</code>.
     */
    <S, D> Set<D> mapAsSet(S[] source, Class<D> destinationClass);

    /**
     * Maps the source Iterable into a new List parameterized by <code>destinationClass</code>.
     * 
     * @param source the Iterable from which to map
     * @param destinationClass the type of elements to be contained in the returned Set.
     * @return a new List containing elements of type <code>destinationClass</code> mapped from the elements of
     *         <code>source</code>.
     */
    <S, D> List<D> mapAsList(Iterable<S> source, Class<D> destinationClass);

    /**
     * Maps the source Array into a new List parameterized by <code>destinationClass</code>.
     * 
     * @param source the Array from which to map
     * @param destinationClass the type of elements to be contained in the returned Set.
     * @return a new List containing elements of type <code>destinationClass</code> mapped from the elements of
     *         <code>source</code>.
     */
    <S, D> List<D> mapAsList(S[] source, Class<D> destinationClass);

    /**
     * Maps the source interable into a new Array of type<code>D</code>.
     * 
     * @param destination
     * @param source the Array from which to map
     * @param destinationClass the type of elements to be contained in the returned Set.
     * @param context the context from the current mapping request
     * @return a new Array containing elements of type <code>destinationClass</code> mapped from the elements of
     *         <code>source</code>.
     */
    <S, D> D[] mapAsArray(D[] destination, Iterable<S> source, Class<D> destinationClass);

    /**
     * Maps the source array into a new Array of type<code>D</code>.
     * 
     * @param destination the destination array which is also returned
     * @param source the source array
     * @param destinationClass the destination class
     * @return a new Array containing elements of type <code>destinationClass</code> mapped from the elements of
     *         <code>source</code>.
     */
    <S, D> D[] mapAsArray(D[] destination, S[] source, Class<D> destinationClass);

    /**
     * Maps the source Iterable into the destination Collection
     * 
     * @param source the source Iterable
     * @param destination the destination Collection
     * @param destinationClass the destination class
     */
    <S, D> void mapAsCollection(Iterable<S> source, Collection<D> destination, Class<D> destinationClass);

    /**
     * Map an array onto an existing collection
     * 
     * @param source the source array
     * @param destination the destination collection
     * @param destinationClass the type of elements in the destination
     */
    <S, D> void mapAsCollection(S[] source, Collection<D> destination, Class<D> destinationClass);
}