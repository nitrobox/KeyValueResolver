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

import static java.lang.Math.min;

import com.nitrobox.keyvalueresolver.jmx.KeyValueResolverManager;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central class to access KeyValueResolver. Manages domains and key-to-value mappings.
 *
 * @author finsterwalder
 * @since 2013-03-25 08:07
 */
public class KeyValueResolverImpl implements KeyValueResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyValueResolverImpl.class);
    public static final String KEY_VALUE_RESOLVER_DOMAINS_TEXT = "KeyValueResolver{domains=";
    private final ValuesStore valuesStore = new ValuesStore();
    private final List<String> domains = new CopyOnWriteArrayList<>();

    public KeyValueResolverImpl(final Persistence persistence, DomainSpecificValueFactory domainSpecificValueFactory) {
        initFromPersistence(persistence, domainSpecificValueFactory);
    }

    public KeyValueResolverImpl(final Persistence persistence) {
        this(persistence, createDomainSpecificValueFactory());
    }

    public KeyValueResolverImpl(final Persistence persistence, DomainSpecificValueFactory domainSpecificValueFactory,
            final String... domains) {
        initDomains(domains);
        initFromPersistence(persistence, domainSpecificValueFactory);
    }

    private void initDomains(final String... domains) {
        addDomains(domains);
    }

    public KeyValueResolverImpl(final Persistence persistence, final String... domains) {
        this(persistence, createDomainSpecificValueFactory(), domains);
    }

    private void initFromPersistence(final Persistence persistence, final DomainSpecificValueFactory domainSpecificValueFactory) {
        Objects.requireNonNull(domainSpecificValueFactory, "\"domainSpecificValueFactory\" must not be null");
        Objects.requireNonNull(persistence, "\"persistence\" must not be null");
        valuesStore.setDomainSpecificValueFactory(domainSpecificValueFactory);
        valuesStore.setPersistence(persistence);
        valuesStore.setAllValues(persistence.loadAll(domainSpecificValueFactory));
        KeyValueResolverManager.getInstance().add(this);
    }

    public KeyValueResolverImpl(final String... domains) {
        initDomains(domains);
        initWithoutPersistence();
        KeyValueResolverManager.getInstance().add(this);
    }

    public KeyValueResolverImpl() {
        initWithoutPersistence();
        KeyValueResolverManager.getInstance().add(this);
    }

    private void initWithoutPersistence() {
        valuesStore.setDomainSpecificValueFactory(createDomainSpecificValueFactory());
    }

    private static DomainSpecificValueFactory createDomainSpecificValueFactory() {
        return new DomainSpecificValueFactoryWithStringInterning();
    }

    @Override
    public <T> T getOrDefault(String key, T defaultValue, String... domainValues) {
        return getOrDefault(key, defaultValue, resolverFor(domainValues));
    }

    @Override
    public <T> T getOrDefault(final String key, final T defaultValue, DomainResolver resolver) {
        final String trimmedKey = trimKey(key);
        KeyValues keyValues = valuesStore.getKeyValuesFromMapOrPersistence(trimmedKey);
        T result;
        if (keyValues == null) {
            result = defaultValue;
        } else {
            result = keyValues.get(domains, defaultValue, resolver);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting value for key: '{}' with given default: '{}'. Returning value: '{}'", trimmedKey, defaultValue, result);
            StringBuilder builder = new StringBuilder("DomainValues: ");
            for (String domain : domains) {
                builder.append(domain).append(" => ").append(resolver.getDomainValue(domain)).append("; ");
            }
            LOGGER.debug(builder.toString());
        }
        return result;
    }

    private static String trimKey(final String key) {
        Ensure.notEmpty(key, "key");
        return key.trim();
    }

    @Override
    public <T> T get(String key, String... domainValues) {
        return getOrDefault(key, null, resolverFor(domainValues));
    }

    @Override
    public <T> T get(final String key, DomainResolver resolver) {
        return getOrDefault(key, null, resolver);
    }

    @Override
    public <T> T getOrDefine(final String key, final T defaultValue, DomainResolver resolver) {
        return getOrDefine(key, defaultValue, null, resolver);
    }

    @Override
    public <T> T getOrDefine(String key, T defaultValue, String description, String... domainValues) {
        return getOrDefine(key, defaultValue, description, resolverFor(domainValues));
    }

    @Override
    public <T> T getOrDefine(final String key, final T defaultValue, String description, DomainResolver resolver) {
        T value = get(key, resolver);
        if (value != null) {
            return value;
        }
        set(key, defaultValue, description);
        return defaultValue;
    }

    @Override
    public KeyValueResolver addDomains(final String... domains) {
        Objects.requireNonNull(domains, "\"domains\" must not be null");
        for (String domain : domains) {
            Ensure.notEmpty(domain, "domain");
            this.domains.add(domain);
        }
        return this;
    }

    @Override
    public DomainSpecificValue set(final String key, final Object value, final String description, final String... domainValues) {
        final String trimmedKey = trimKey(key);
        LOGGER.debug("Storing value: '{}' for key: '{}' with given domains: '{}'.", value, trimmedKey, domainValues);
        valuesStore.setWithChangeSet(trimmedKey, description, null, value, domainValues);
        return DomainSpecificValue.withoutChangeSet(value, domainValues);
    }

    @Override
    public DomainSpecificValue set(final String key, final Object value, final String description, DomainValues domainValues) {
        String[] domainValuesArray = domainValues.getDomainValues(domains);
        return this.set(key, value, description, domainValuesArray);
    }

    @Override
    public DomainSpecificValue setWithChangeSet(final String key, final Object value, final String description, String changeSet,
            final String... domainValues) {
        final String trimmedKey = trimKey(key);
        LOGGER.debug("Storing value: '{}' for key: '{}' for change set: '{}' with given domains: '{}'.", value, trimmedKey, changeSet,
                domainValues);
        valuesStore.setWithChangeSet(trimmedKey, description, changeSet, value, domainValues);
        return DomainSpecificValue.withChangeSet(value, changeSet, domainValues);
    }

    @Override
    public DomainSpecificValue setWithChangeSet(String key, Object value, String description, String changeSet, DomainValues domainValues) {
        String[] domainValuesArray = domainValues.getDomainValues(domains);
        return this.setWithChangeSet(key, value, description, changeSet, domainValuesArray);
    }

    public void setPersistence(final Persistence persistence) {
        Objects.requireNonNull(persistence, "\"persistence\" must not be null");
        valuesStore.setPersistence(persistence);
        KeyValueResolverManager.getInstance().add(this);
    }

    @Override
    public void reload() {
        valuesStore.reload();
    }

    @Override
    public void reload(String key) {
        valuesStore.reload(trimKey(key));
    }

    @Override
    public String toString() {
        return KEY_VALUE_RESOLVER_DOMAINS_TEXT + domains + '}';
    }

    @Override
    public StringBuilder dump() {
        StringBuilder builder = new StringBuilder(KEY_VALUE_RESOLVER_DOMAINS_TEXT).append(domains);
        builder.append(valuesStore.dump());
        builder.append("\n}");
        return builder;
    }

    @Override
    public void dump(final PrintStream out) {
        out.print(KEY_VALUE_RESOLVER_DOMAINS_TEXT);
        out.print(domains);
        valuesStore.dump(out);
        out.println("\n}");
    }

    @Override
    public KeyValues getKeyValues(final String key) {
        return valuesStore.getValuesFor(trimKey(key));
    }

    @Override
    public KeyValues getKeyValues(String key, DomainResolver... resolver) {
        final KeyValues keyValues = getKeyValues(trimKey(key));
        return keyValues != null ? keyValues.copy(domains, resolver) : null;
    }

    public void setDomainSpecificValueFactory(final DomainSpecificValueFactory domainSpecificValueFactory) {
        valuesStore.setDomainSpecificValueFactory(domainSpecificValueFactory);
    }

    @Override
    public Collection<KeyValues> getAllKeyValues() {
        return valuesStore.getAllValues();
    }

    @Override
    public Collection<KeyValues> getAllKeyValues(DomainResolver... resolver) {
        return valuesStore.getAllValues(domains, resolver);
    }

    @Override
    public Collection<KeyValues> getAllKeyValues(String... domainValues) {
        return valuesStore.getAllValues(domains, resolverFor(domainValues));
    }

    @Override
    public <T> Map<String, T> getAllMappings(String... domainValues) {
        return getAllMappings(resolverFor(domainValues));
    }

    @Override
    public <T> Map<String, T> getAllMappings(DomainResolver resolver) {
        return getAllKeyValues().stream()
                .collect(Collector.of(HashMap::new,
                        (result, kv) -> {
                            final T s = kv.get(domains, null, resolver);
                            if (s != null) {
                                result.put(kv.getKey(), s);
                            }
                        }, (result1, result2) -> {
                            result1.putAll(result2);
                            return result1;
                        },
                        Collector.Characteristics.CONCURRENT,
                        Collector.Characteristics.UNORDERED));
    }

    @Override
    public void removeWithChangeSet(final String key, final String changeSet, final String... domainValues) {
        valuesStore.removeWithChangeSet(trimKey(key), changeSet, domainValues);
    }

    @Override
    public void removeWithChangeSet(final String key, final String changeSet, DomainValues domainValues) {
        String[] domainValuesArray = domainValues.getDomainValues(domains);
        valuesStore.removeWithChangeSet(trimKey(key), changeSet, domainValuesArray);
    }

    @Override
    public void remove(final String key, final String... domainValues) {
        removeWithChangeSet(key, null, domainValues);
    }

    @Override
    public void remove(final String key, DomainValues domainValues) {
        String[] domainValuesArray = domainValues.getDomainValues(domains);
        removeWithChangeSet(key, null, domainValuesArray);
    }

    @Override
    public void removeAllMatching(final String key, final String... domainValues) {
        final String trimmedKey = trimKey(key);
        valuesStore.removeAllMatching(trimmedKey, domains, domainValues);
    }

    @Override
    public void removeAllMatching(final String key, DomainValues domainValues) {
        String[] domainValuesArray = domainValues.getDomainValues(domains);
        valuesStore.removeAllMatching(key, domains, domainValuesArray);
    }


    @Override
    public void removeKey(final String key) {
        valuesStore.remove(trimKey(key));
    }

    @Override
    public void removeChangeSet(String changeSet) {
        Objects.requireNonNull(changeSet, "\"changeSet\" must not be null");
        valuesStore.removeChangeSet(changeSet);
    }

    public DomainResolver resolverFor(String... domainValues) {
        return resolverFor(domains, domainValues);
    }

    public static DomainResolver resolverFor(List<String> domains, String... domainValues) {
        MapBackedDomainResolver resolver = new MapBackedDomainResolver();
        for (int i = 0; i < min(domains.size(), domainValues.length); i++) {
            resolver.set(domains.get(i), domainValues[i]);
        }
        return resolver;
    }

    @Override
    public List<String> getDomains() {
        return Collections.unmodifiableList(domains);
    }

    @Override
    public Map<String, String> getDomainValuesMap(DomainSpecificValue domainSpecificValue) {
        final String[] domainValues = domainSpecificValue.getDomainValues();
        final Map<String, String> result = new HashMap<>();
        int bothHaveValuesIndex = 0;
        for (; bothHaveValuesIndex < min(domains.size(), domainValues.length); bothHaveValuesIndex++) {
            result.put(domains.get(bothHaveValuesIndex), domainValues[bothHaveValuesIndex]);
        }
        for (int extraDomainsIndex = bothHaveValuesIndex; extraDomainsIndex < domains.size(); extraDomainsIndex++) {
            result.put(domains.get(extraDomainsIndex), "*");
        }
        return result;
    }
}
