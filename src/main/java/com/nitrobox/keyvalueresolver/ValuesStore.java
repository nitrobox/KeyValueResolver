package com.nitrobox.keyvalueresolver;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The internal in memory storage for KeyValueResolver KeyValues
 */
public class ValuesStore {

    private final Map<String, KeyValues> keyValuesMap = new HashMap<>();
    private final ReadWriteLockTool lock = new ReadWriteLockTool();
    private DomainSpecificValueFactory domainSpecificValueFactory;
    private Persistence persistence;

    public Collection<KeyValues> getAllValues() {
        return lock.readLocked(() -> Collections.unmodifiableCollection(keyValuesMap.values()));
    }

    public Collection<KeyValues> getAllValues(List<String> domains, DomainResolver... resolver) {
        return lock.readLocked(() -> keyValuesMap.values().stream()
                .map(keyValues -> keyValues.copy(domains, resolver))
                .filter(keyValues -> !keyValues.isEmpty())
                .collect(Collectors.toUnmodifiableList()));
    }

    public void setAllValues(Collection<? extends KeyValues> values) {
        lock.writeLocked(() -> {
            keyValuesMap.clear();
            values.forEach(kv -> keyValuesMap.put(kv.getKey(), kv));
        });
    }

    public void setWithChangeSet(String key, String description, String changeSet, final Object value, final String... domainValues) {
        KeyValues keyValues = getOrCreateKeyValues(key, description);
        final DomainSpecificValue domainSpecificValue = keyValues.putWithChangeSet(changeSet, value, domainValues);
        store(key, keyValues, domainSpecificValue);
    }

    /*package*/ KeyValues getOrCreateKeyValues(final String key, final String description) {
        KeyValues keyValues = getKeyValuesFromMapOrPersistence(key);
        if (keyValues != null) {
            return keyValues;
        }
        return lock.writeLocked(() -> keyValuesMap.computeIfAbsent(key, k -> new KeyValues(key, domainSpecificValueFactory, description)));
    }

    private void store(final String key, final KeyValues keyValues, DomainSpecificValue domainSpecificValue) {
        if (persistence != null) {
            persistence.store(key, keyValues, domainSpecificValue);
        }
    }

    public KeyValues getKeyValuesFromMapOrPersistence(final String key) {
        final KeyValues keyValues = lock.readLocked(() -> keyValuesMap.get(key));
        if (keyValues != null) {
            return keyValues;
        }
        final KeyValues loadedKeyValues = load(key);
        if (loadedKeyValues == null) {
            return null;
        }
        return lock.writeLocked(() -> {
            KeyValues keyValuesSecondTry = keyValuesMap.get(key);
            if (keyValuesSecondTry == null) {
                keyValuesMap.put(key, loadedKeyValues);
                return loadedKeyValues;
            } else {
                return keyValuesSecondTry;
            }
        });
    }

    public String dump() {
        return lock.readLocked(() -> {
            StringBuilder builder = new StringBuilder(keyValuesMap.size() * 16);
            for (Map.Entry<String, KeyValues> entry : keyValuesMap.entrySet()) {
                builder.append('\n').append("KeyValues for \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            }
            return builder.toString();
        });
    }

    public void dump(PrintStream out) {
        lock.readLocked(() -> {
            for (Map.Entry<String, KeyValues> entry : keyValuesMap.entrySet()) {
                out.println();
                out.print("KeyValues for \"");
                out.print(entry.getKey());
                out.print("\": ");
                out.print(entry.getValue());
            }
        });
    }

    public KeyValues getValuesFor(String key) {
        return lock.readLocked(() -> keyValuesMap.get(key));
    }

    public KeyValues remove(String key) {
        return lock.writeLocked(() -> keyValuesMap.remove(key));
    }

    private KeyValues load(final String key) {
        if (persistence != null) {
            return persistence.load(key, domainSpecificValueFactory);
        }
        return null;
    }

    public void setDomainSpecificValueFactory(DomainSpecificValueFactory domainSpecificValueFactory) {
        this.domainSpecificValueFactory = domainSpecificValueFactory;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public void reload() {
        if (persistence != null) {
            setAllValues(persistence.reload(getAllValues(), domainSpecificValueFactory));
        }
    }

    public void removeChangeSet(String changeSet) {
        lock.readLocked(() -> {
            for (KeyValues keyValues : keyValuesMap.values()) {
                final Collection<DomainSpecificValue> domainSpecificValues = keyValues.removeChangeSet(changeSet);
                if (keyValues.isEmpty()) {
                    persistence.remove(keyValues.getKey());
                } else {
                    for (DomainSpecificValue value : domainSpecificValues) {
                        persistence.remove(keyValues.getKey(), value);
                    }
                }
            }
        });
    }
}
