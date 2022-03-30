package com.nitrobox.keyvalueresolver;

import static com.nitrobox.keyvalueresolver.KeyValueResolverImpl.resolverFor;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        lock.writeLocked(() -> {
            KeyValues keyValues = getOrCreateKeyValues(key, description);
            final DomainSpecificValue domainSpecificValue = keyValues.putWithChangeSet(changeSet, value, domainValues);
            store(key, keyValues, domainSpecificValue);
        });
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
        return lock.writeLocked(() -> {
            final KeyValues keyValues = keyValuesMap.remove(key);
            if (persistence != null) {
                persistence.remove(key);
            }
            return keyValues;
        });
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

    public void reload(String key) {
        if (persistence != null) {
            final KeyValues keyValues = persistence.load(key, domainSpecificValueFactory);
            lock.writeLocked(() -> {
                if (keyValues != null) {
                    keyValuesMap.put(key, keyValues);
                } else {
                    remove(key);
                }
            });
        }
    }

    public void removeWithChangeSet(final String key, final String changeSet, final String... domainValues) {
        lock.writeLocked(() -> {
            KeyValues keyValues = getKeyValuesFromMapOrPersistence(key);
            if (keyValues != null) {
                removeFromPersistence(key, keyValues.remove(changeSet, domainValues));
            }
        });
    }

    public void removeAllMatching(String key, List<String> domains, String... domainValues) {
        lock.writeLocked(() -> {
            final KeyValues keyValues = getKeyValuesFromMapOrPersistence(key);
            if (keyValues != null) {
                keyValues.removeAll(domains, resolverFor(domains, domainValues))
                        .forEach(domainSpecificValue -> removeFromPersistence(key, domainSpecificValue));
                if (keyValues.isEmpty()) {
                    remove(key);
                }
            }
        });
    }

    private void removeFromPersistence(final String key, final DomainSpecificValue domainSpecificValue) {
        if (persistence != null && domainSpecificValue != null) {
            persistence.remove(key, domainSpecificValue);
        }
    }

    public void removeChangeSet(String changeSet) {
        lock.writeLocked(() -> {
            for (KeyValues keyValues : keyValuesMap.values()) {
                final Collection<DomainSpecificValue> domainSpecificValues = keyValues.removeChangeSet(changeSet);
                for (DomainSpecificValue value : domainSpecificValues) {
                    removeFromPersistence(keyValues.getKey(), value);
                }
            }
        });
    }
}
