/*
 * Copyright (c) 2000, 2017 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Payara Services Ltd.
 * Copyright (c) 2020, 2021 Contributors to Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.messaging.jmq.util.lists;

import java.util.*;
import java.lang.ref.*;

public class WeakValueHashMap<K, V> implements Map<K, V> {
    String name = "Unknown";

    HashMap<K, Reference<V>> baseMap = new HashMap<>();
    /**
     * Reference queue for cleared WeakEntries
     */
    private ReferenceQueue<K> myqueue = new ReferenceQueue<>();

    public WeakValueHashMap(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "WeakValueHashMap(" + name + ")";
    }

    private void cleanupMap() {
        Reference<? extends K> r;

        while ((r = myqueue.poll()) != null) {
            MyWeakValueReference<K, V> wr = (MyWeakValueReference<K, V>) r;
            baseMap.remove(wr.getOriginalKey());
        }

    }

    static class MyWeakValueReference<K, T> extends java.lang.ref.WeakReference<T> {
        K mykey = null;

        MyWeakValueReference(K key, T value, ReferenceQueue<? super T> q) {
            super(value, q);
            this.mykey = key;
        }

        @Override
        public int hashCode() {
            return mykey.hashCode();
        }

        public K getOriginalKey() {
            return mykey;
        }

        @Override
        public String toString() {
            return mykey.toString();
        }
    }

    // Query Operations

    /**
     * Returns the number of key-value mappings in this map. If the map contains more than <tt>Integer.MAX_VALUE</tt>
     * elements, returns <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        cleanupMap();
        return baseMap.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        cleanupMap();
        return baseMap.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key. More formally, returns <tt>true</tt> if
     * and only if this map contains at a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>. (There can be at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     *
     * @throws ClassCastException if the key is of an inappropriate type for this map (optional).
     * @throws NullPointerException if the key is <tt>null</tt> and this map does not not permit <tt>null</tt> keys
     * (optional).
     */
    @Override
    public boolean containsKey(Object key) {
        WeakReference<V> k = (WeakReference<V>) baseMap.get(key);
        return k != null && !k.isEnqueued();
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value. More formally, returns <tt>true</tt>
     * if and only if this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>. This operation will probably require time linear in the map size
     * for most implementations of the <tt>Map</tt> interface.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the specified value.
     * @throws ClassCastException if the value is of an inappropriate type for this map (optional).
     * @throws NullPointerException if the value is <tt>null</tt> and this map does not not permit <tt>null</tt> values
     * (optional).
     */
    @Override
    public boolean containsValue(Object value) {
        cleanupMap();
        return baseMap.containsValue(value);
    }

    /**
     * Returns the value to which this map maps the specified key. Returns <tt>null</tt> if the map contains no mapping for
     * this key. A return value of <tt>null</tt> does not <i>necessarily</i> indicate that the map contains no mapping for
     * the key; it's also possible that the map explicitly maps the key to <tt>null</tt>. The <tt>containsKey</tt> operation
     * may be used to distinguish these two cases.
     *
     * <p>
     * More formally, if this map contains a mapping from a key <tt>k</tt> to a value <tt>v</tt> such that
     * <tt>(key==null ? k==null :
     * key.equals(k))</tt>, then this method returns <tt>v</tt>; otherwise it returns <tt>null</tt>. (There can be at most
     * one such mapping.)
     *
     * @param key key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or <tt>null</tt> if the map contains no mapping for this
     * key.
     *
     * @throws ClassCastException if the key is of an inappropriate type for this map (optional).
     * @throws NullPointerException key is <tt>null</tt> and this map does not not permit <tt>null</tt> keys (optional).
     *
     * @see #containsKey(Object)
     */
    @Override
    public V get(Object key) {
        cleanupMap();
        Reference<V> ref = baseMap.get(key);
        return (ref == null ? null : (ref.isEnqueued() ? null : ref.get()));
    }

    // Modification Operations

    /**
     * Associates the specified value with the specified key in this map (optional operation). If the map previously
     * contained a mapping for this key, the old value is replaced by the specified value. (A map <tt>m</tt> is said to
     * contain a mapping for a key <tt>k</tt> if and only if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.))
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     * <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the specified key, if
     * the implementation supports <tt>null</tt> values.
     *
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by this map.
     * @throws ClassCastException if the class of the specified key or value prevents it from being stored in this map.
     * @throws IllegalArgumentException if some aspect of this key or value prevents it from being stored in this map.
     * @throws NullPointerException this map does not permit <tt>null</tt> keys or values, and the specified key or value is
     * <tt>null</tt>.
     */
    @Override
    public V put(K key, V value) {
        cleanupMap();
        MyWeakValueReference<K, V> ref = new MyWeakValueReference(key, value, myqueue);
        WeakReference<V> oldref = (WeakReference<V>) baseMap.put(key, ref);
        return (oldref == null ? null : (oldref.isEnqueued() ? null : oldref.get()));
    }

    /**
     * Removes the mapping for this key from this map if it is present (optional operation). More formally, if this map
     * contains a mapping from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping is removed. (The map can contain at most one such
     * mapping.)
     *
     * <p>
     * Returns the value to which the map previously associated the key, or <tt>null</tt> if the map contained no mapping
     * for this key. (A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the
     * specified key if the implementation supports <tt>null</tt> values.) The map will not contain a mapping for the
     * specified key once the call returns.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt> if there was no mapping for key.
     *
     * @throws ClassCastException if the key is of an inappropriate type for this map (optional).
     * @throws NullPointerException if the key is <tt>null</tt> and this map does not not permit <tt>null</tt> keys
     * (optional).
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is not supported by this map.
     */
    @Override
    public V remove(Object key) {
        cleanupMap();
        WeakReference<V> ref = (WeakReference<V>) baseMap.remove(key);
        return (ref == null ? null : ref.get());
    }

    // Bulk Operations

    /**
     * Copies all of the mappings from the specified map to this map (optional operation). The effect of this call is
     * equivalent to that of calling {@link #put(Object,Object) put(k, v)} on this map once for each mapping from key
     * <tt>k</tt> to value <tt>v</tt> in the specified map. The behavior of this operation is unspecified if the specified
     * map is modified while the operation is in progress.
     *
     * @param t Mappings to be stored in this map.
     *
     * @throws UnsupportedOperationException if the <tt>putAll</tt> method is not supported by this map.
     *
     * @throws ClassCastException if the class of a key or value in the specified map prevents it from being stored in this
     * map.
     *
     * @throws IllegalArgumentException some aspect of a key or value in the specified map prevents it from being stored in
     * this map.
     * @throws NullPointerException the specified map is <tt>null</tt>, or if this map does not permit <tt>null</tt> keys or
     * values, and the specified map contains <tt>null</tt> keys or values.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> t) {
        cleanupMap();
        for (Map.Entry<? extends K, ? extends V> entry: t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes all mappings from this map (optional operation).
     *
     * @throws UnsupportedOperationException clear is not supported by this map.
     */
    @Override
    public void clear() {
        cleanupMap();
        baseMap.clear();
    }

    // Views

    /**
     * Returns a set view of the keys contained in this map. The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa. If the map is modified while an iteration over the set is in progress, the
     * results of the iteration are undefined. The set supports element removal, which removes the corresponding mapping
     * from the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt> <tt>retainAll</tt>, and
     * <tt>clear</tt> operations. It does not support the add or <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {
        cleanupMap();
        return baseMap.keySet();
    }

    /**
     * Returns a collection view of the values contained in this map. The collection is backed by the map, so changes to the
     * map are reflected in the collection, and vice-versa. If the map is modified while an iteration over the collection is
     * in progress, the results of the iteration are undefined. The collection supports element removal, which removes the
     * corresponding mapping from the map, via the <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support the add or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    private Collection<V> values = null;

    @Override
    public Collection<V> values() {
        cleanupMap();
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    private class ValueIterator implements Iterator<V> {
        WeakReference<V> next = null;
        Iterator<Reference<V>> itr = null;

        ValueIterator(Iterator<Reference<V>> itr) {
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (!itr.hasNext()) {
                return false;
            }
            next = (WeakReference<V>) itr.next();
            while (next.isEnqueued()) {
                itr.remove();
                if (itr.hasNext()) {
                    next = (WeakReference<V>) itr.next();
                } else {
                    next = null;
                    break;
                }
            }
            return next != null;

        }

        @Override
        public V next() {
            if (next == null) {
                if (!hasNext()) {
                    return null;
                }
            }
            while (next.isEnqueued()) {
                itr.remove();
                next = null;
                if (!hasNext()) {
                    break;
                }
            }
            V refval = (next == null ? null : next.get());
            next = null;
            return refval;
        }

        @Override
        public void remove() {
            itr.remove();
        }
    }

    private class Values extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator(baseMap.values().iterator());
        }

        @Override
        public int size() {
            return WeakValueHashMap.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            WeakValueHashMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            Collection<V> c = new ArrayList<>(size());
            for (Iterator<V> i = iterator(); i.hasNext();) {
                c.add(i.next());
            }
            return c.toArray();
        }

        @Override
        public Object[] toArray(Object a[]) {
            Collection<V> c = new ArrayList<>(size());
            for (Iterator<V> i = iterator(); i.hasNext();) {
                c.add(i.next());
            }
            return c.toArray(a);
        }
    }

    /**
     * Returns a set view of the mappings contained in this map. Each element in the returned set is a {@link Map.Entry}.
     * The set is backed by the map, so changes to the map are reflected in the set, and vice-versa. If the map is modified
     * while an iteration over the set is in progress, the results of the iteration are undefined. The set supports element
     * removal, which removes the corresponding mapping from the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map.
     */
    private Set<Map.Entry<K,V>> entrySet = null;

    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        cleanupMap();
        Set<Map.Entry<K,V>> vs = entrySet;
        return (vs != null ? vs : (entrySet = new EntrySet()));
    }

    private class newEntryIterator implements Iterator<Map.Entry<K,V>> {
        Map.Entry<K,V> nextentry = null;
        Iterator<Map.Entry<K, Reference<V>>> itr = null;

        newEntryIterator(Iterator<Map.Entry<K, Reference<V>>> itr) {
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            if (nextentry != null) {
                return true;
            }
            if (!itr.hasNext()) {
                return false;
            }
            Map.Entry<K, Reference<V>> mentry = itr.next();

            WeakReference<V> nextref = (WeakReference<V>) mentry.getValue();
            while (nextref.isEnqueued()) {
                itr.remove();
                mentry = itr.next();
                nextref = (WeakReference<V>) mentry.getValue();
            }
            nextentry = new WeakValueEntry(mentry.getKey(), nextref.get());
            return nextentry != null;

        }

        @Override
        public Map.Entry<K, V> next() {
            if (nextentry == null) {
                if (!hasNext()) {
                    return null;
                }
            }
            Map.Entry<K,V> returnval = nextentry;
            nextentry = null;
            return returnval;
        }

        @Override
        public void remove() {
            itr.remove();
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new newEntryIterator(baseMap.entrySet().iterator());
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<K, V> e = (Map.Entry<K, V>) o;
            return WeakValueHashMap.this.containsKey(e.getKey());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<K, V> e = (Map.Entry<K, V>) o;
            return WeakValueHashMap.this.remove(e.getKey()) != null;
        }

        @Override
        public int size() {
            return WeakValueHashMap.this.size();
        }

        @Override
        public void clear() {
            WeakValueHashMap.this.clear();
        }
    }

    /**
     * A map entry (key-value pair). The <tt>Map.entrySet</tt> method returns a collection-view of the map, whose elements
     * are of this class. The <i>only</i> way to obtain a reference to a map entry is from the iterator of this
     * collection-view. These <tt>Map.Entry</tt> objects are valid <i>only</i> for the duration of the iteration; more
     * formally, the behavior of a map entry is undefined if the backing map has been modified after the entry was returned
     * by the iterator, except through the iterator's own <tt>remove</tt> operation, or through the <tt>setValue</tt>
     * operation on a map entry returned by the iterator.
     *
     * @see Map#entrySet()
     * @since 1.2
     */
    class WeakValueEntry implements Map.Entry<K, V> {

        K key = null;
        V value = null;

        WeakValueEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry.
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry. If the mapping has been removed from the backing map (by the
         * iterator's <tt>remove</tt> operation), the results of this call are undefined.
         *
         * @return the value corresponding to this entry.
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified value (optional operation). (Writes through to the
         * map.) The behavior of this call is undefined if the mapping has already been removed from the map (by the iterator's
         * <tt>remove</tt> operation).
         *
         * @param value new value to be stored in this entry.
         * @return old value corresponding to the entry.
         *
         * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the backing map.
         * @throws ClassCastException if the class of the specified value prevents it from being stored in the backing map.
         * @throws IllegalArgumentException if some aspect of this value prevents it from being stored in the backing map.
         * @throws NullPointerException the backing map does not permit <tt>null</tt> values, and the specified value is
         * <tt>null</tt>.
         */
        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("not supported");
        }

        /**
         * Compares the specified object with this entry for equality. Returns <tt>true</tt> if the given object is also a map
         * entry and the two entries represent the same mapping. More formally, two entries <tt>e1</tt> and <tt>e2</tt>
         * represent the same mapping if
         *
         * <pre>
         * (e1.getKey() == null ? e2.getKey() == null : e1.getKey().equals(e2.getKey()))
         *         && (e1.getValue() == null ? e2.getValue() == null : e1.getValue().equals(e2.getValue()))
         * </pre>
         *
         * This ensures that the <tt>equals</tt> method works properly across different implementations of the
         * <tt>Map.Entry</tt> interface.
         *
         * @param o object to be compared for equality with this map entry.
         * @return <tt>true</tt> if the specified object is equal to this map entry.
         */
        @Override
        public boolean equals(Object o) {

            if (o instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) o;

                return (getKey() == null ? entry.getKey() == null : getKey().equals(entry.getKey()))
                        && (getValue() == null ? entry.getValue() == null : getValue().equals(entry.getValue()));
            } else {
                return super.equals(o);
            }
        }

        /**
         * Returns the hash code value for this map entry. The hash code of a map entry <tt>e</tt> is defined to be:
         *
         * <pre>
         * (e.getKey() == null ? 0 : e.getKey().hashCode()) ^ (e.getValue() == null ? 0 : e.getValue().hashCode())
         * </pre>
         *
         * This ensures that <tt>e1.equals(e2)</tt> implies that <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
         * <tt>e1</tt> and <tt>e2</tt>, as required by the general contract of <tt>Object.hashCode</tt>.
         *
         * @return the hash code value for this map entry.
         * @see Object#hashCode()
         * @see Object#equals(Object)
         * @see #equals(Object)
         */
        @Override
        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
        }
    }

    // Comparison and hashing

    /**
     * Compares the specified object with this map for equality. Returns <tt>true</tt> if the given object is also a map and
     * the two Maps represent the same mappings. More formally, two maps <tt>t1</tt> and <tt>t2</tt> represent the same
     * mappings if <tt>t1.entrySet().equals(t2.entrySet())</tt>. This ensures that the <tt>equals</tt> method works properly
     * across different implementations of the <tt>Map</tt> interface.
     *
     * @param o object to be compared for equality with this map.
     * @return <tt>true</tt> if the specified object is equal to this map.
     */
    @Override
    public boolean equals(Object o) {
        cleanupMap();
        if (o instanceof WeakValueHashMap) {
            return baseMap.equals(((WeakValueHashMap) o).baseMap);
        } else {
            return baseMap.equals(o);
        }
    }

    /**
     * Returns the hash code value for this map. The hash code of a map is defined to be the sum of the hashCodes of each
     * entry in the map's entrySet view. This ensures that <tt>t1.equals(t2)</tt> implies that
     * <tt>t1.hashCode()==t2.hashCode()</tt> for any two maps <tt>t1</tt> and <tt>t2</tt>, as required by the general
     * contract of Object.hashCode.
     *
     * @return the hash code value for this map.
     * @see Map.Entry#hashCode()
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        cleanupMap();
        return baseMap.hashCode();
    }

    public static void main(String args[]) {
        WeakValueHashMap<Object, String> hm = new WeakValueHashMap<>("test");
        Object key1 = new Object();
        Object key2 = new Object();
        String value1 = "value1";
        String value2 = "value2";
        hm.put(key1, value1);
        hm.put(key2, value2);
        for (int i = 0; i < 10; i++) {
            System.gc();
        }
        System.out.println("K1 get " + hm.get(key1));
        System.out.println("K2 get " + hm.get(key2));
        value2 = null;
        value1 = null;
        for (int i = 0; i < 100; i++) {
            System.gc();
        }
        System.out.println("K1 get " + hm.get(key1));
        System.out.println("K2 get " + hm.get(key2));
    }

}
