/*
 * KeyValueResolver - An dynamic Key-Value Store
 * Copyright (C) 2022 Nitrobox GmbH
 *
 * This Software is a fork of Roperty - An advanced property
 * management and retrival system
 * Copyright (C) 2013 PARSHIP GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nitrobox.keyvalueresolver;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface KeyValueResolver {

    /**
     * Get a value for a given key from KeyValueResolver
     *
     * @param key          key to query
     * @param defaultValue defaultValue is returned, when no value for the key is found
     * @param resolver     resolver to determine domain values to use during resolution
     * @param <T>          type of the objects stored under the provided key
     * @return object retrieved from KeyValueResolver or defaultValue
     */
    <T> T getOrDefault(String key, T defaultValue, DomainResolver resolver);

    <T> T getOrDefault(String key, T defaultValue, String... domainValues);

    /**
     * Get a value for a given key from KeyValueResolver. Same as calling get(key, null, resolver);
     *
     * @param key      key to query
     * @param resolver resolver to determine domain values to use during resolution
     * @param <T>      type of the objects stored under the provided key
     * @return object retrieved from KeyValueResolver or defaultValue
     */
    <T> T get(String key, DomainResolver resolver);

    <T> T get(String key, String... domainValues);

    /**
     * Get a value for a given key from KeyValueResolver. When no value is found in KeyValueResolver, the provided default is stored in
     * KeyValueResolver. Same as calling getOfDefine(key, defaultValue, resolver, null);
     *
     * @param key          key to query
     * @param defaultValue defaultValue is returned, when no value for the key is found
     * @param resolver     resolver to determine domain values to use during resolution
     * @param <T>          type of the objects stored under the provided key
     * @return object retrieved from KeyValueResolver or defaultValue
     */
    <T> T getOrDefine(String key, T defaultValue, DomainResolver resolver);

    /**
     * Get a value for a given key from KeyValueResolver. When no value is found in KeyValueResolver, the provided default is stored in
     * KeyValueResolver along with the provided description. Same as calling get(key, null, resolver);
     *
     * @param <T>          type of the objects stored under the provided key
     * @param key          key to query
     * @param defaultValue defaultValue is returned, when no value for the key is found
     * @param resolver     resolver to determine domain values to use during resolution
     * @return object retrieved from KeyValueResolver or defaultValue
     */
    <T> T getOrDefine(String key, T defaultValue, String description, DomainResolver resolver);

    <T> T getOrDefine(String key, T defaultValue, String description, String... domainValues);

    KeyValueResolver addDomains(String... domains);

    DomainSpecificValue set(String key, Object value, String description, String... domainValues);

    DomainSpecificValue set(String key, Object value, String description, DomainValues domainValues);

    DomainSpecificValue setWithChangeSet(String key, Object value, String description, String changeSet, String... domainValues);

    DomainSpecificValue setWithChangeSet(String key, Object value, String description, String changeSet, DomainValues domainValues);

    void reload();

    void reload(String key);

    StringBuilder dump();

    void dump(PrintStream out);

    KeyValues getKeyValues(String key);

    KeyValues getKeyValues(String key, DomainResolver... resolver);

    /**
     * Get all KeyValues stored in this KeyValueResolver instance.
     */
    Collection<KeyValues> getAllKeyValues();

    /**
     * Get those KeyValues stored in this KeyValueResolver instance, with only those DomainSpecificValues, where the provided resolver
     * domains match or are wildcarded.
     */
    Collection<KeyValues> getAllKeyValues(DomainResolver... resolver);

    Collection<KeyValues> getAllKeyValues(String... domainValues);

    <T> Map<String, T> getAllMappings(DomainResolver resolver);

    <T> Map<String, T> getAllMappings(String... domainValues);

    /**
     * Remove a single domain specific value with exactly the provided domainValues from the given changeSet.
     * The key is also removed, when no domain specific values are left after this removal.
     * @param key key for which to remove the domain specific value
     * @param changeSet the changeSet to remove the domain specific value from
     * @param domainValues the domainValues for the key to remove
     */
    void removeWithChangeSet(String key, String changeSet, String... domainValues);
    void removeWithChangeSet(String key, String changeSet, DomainValues domainValues);

    /**
     * Remove a single domain specific value with exactly the provided domainValues.
     * The key is also removed, when no domain specific values are left after this removal.
     * @param key key for which to remove the domain specific value
     * @param domainValues the domainValues for the key to remove
     */
    void remove(String key, String... domainValues);
    void remove(String key, DomainValues domainValues);

    /**
     * Removes all domain specific values that match the given domains.
     * The key is also removed, when no domain specific values are left after this removal.
     * <p> 
     * <strong>NOTE:</strong> This will remove the values that would have been returned for
     * the query meaning that when `a|*` and `a|b` exist `removeAllMatching(key, "*|b")` will remove `a|b`. Calling it again, will remove
     * `a|*` as that is the one matched.
     * @param key key for which to remove the domain specific values
     * @param domainValues the domain patterns for which domain specific values shall be removed. May be partial and can contain wildcards.
     */
    void removeAllMatching(String key, String... domainValues);

    void removeAllMatching(String key, DomainValues domainValues);

    /**
     * removes a complete key with all domain specific values.
     * @param key key to remove
     */
    void removeKey(String key);

    /**
     * Removes a complete changeSet with all domain specific values stored in this changeset.
     * @param changeSet changeSet to remove
     */
    void removeChangeSet(String changeSet);

    /**
     * creates a resolver by combining the domainValues passed with the domains stored in this resolver.
     * When less domainValues are passed, than domains are present, the resulting domains are wildcarded. 
     * @param domainValues domain values to set in the order of the domains stored in this resolver.
     * @return a DomainResolver with the provided values set.
     */
    DomainResolver resolverFor(String... domainValues);

    List<String> getDomains();

    /**
     * Creates a map with domain -> domainValue mappings for the provided domainSpecificValue.
     * When the domainSpecificValue does not have domainValues for all domains, the remaining domains are filled with wildcard '*'.
     * @param domainSpecificValue domainSpecificValue to map
     * @return a map containing a mapping domain -> domainValue for all domains of this resolver
     */
    Map<String, String> getDomainValuesMap(DomainSpecificValue domainSpecificValue);
}
