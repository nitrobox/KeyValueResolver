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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValuesStoreTest {

    @InjectMocks
    private ValuesStore valuesStore = new ValuesStore();
    @Mock
    private Persistence persistence;

    private final DomainSpecificValueFactory domainSpecificValueFactory = new DefaultDomainSpecificValueFactory();
    private final KeyValues keyValues = new KeyValues("key", domainSpecificValueFactory);

    @BeforeEach
    void before() {
        valuesStore.setDomainSpecificValueFactory(domainSpecificValueFactory);
    }

    @Test
    void valuesAreEmptyOnInitialization() {
        assertThat(valuesStore.getAllValues()).isEmpty();
        assertThat(valuesStore.getValuesFor("key")).isNull();
    }

    @Test
    void cannotModifyMapFromOuterClass() {
        assertThrows(UnsupportedOperationException.class, () -> valuesStore.getAllValues().add(keyValues));
        assertThat(valuesStore.getAllValues()).isEmpty();
    }

    @Test
    void valuesShouldBeAdded() {
        Collection<KeyValues> values = new ArrayList<>();
        values.add(keyValues);

        valuesStore.setAllValues(values);

        assertThat(valuesStore.getValuesFor("key")).isEqualTo(keyValues);
        assertThat(valuesStore.getValuesFor("another key")).isNull();
        assertThat(valuesStore.dump()).isEqualTo("\nKeyValues for \"key\": KeyValues{\n\tdescription=\"\"\n}");
        assertThat(valuesStore.getAllValues()).hasSize(1);
    }

    @Test
    void loadUnknownValuesFromPersistence() {
        when(persistence.load("key", domainSpecificValueFactory)).thenReturn(keyValues);

        KeyValues result = valuesStore.getKeyValuesFromMapOrPersistence("key");

        verify(persistence).load("key", domainSpecificValueFactory);
        assertThat(result).isEqualTo(keyValues);
        assertThat(valuesStore.getAllValues()).hasSize(1);
    }

    @Test
    void loadKnownValuesFromMap() {
        Collection<KeyValues> values = new ArrayList<>();
        values.add(keyValues);
        valuesStore.setAllValues(values);

        KeyValues result = valuesStore.getKeyValuesFromMapOrPersistence("key");

        assertThat(result).isEqualTo(keyValues);
        assertThat(valuesStore.getAllValues()).hasSize(1);
    }

    @Test
    void valueShouldBeAdded() {
        KeyValues result = valuesStore.getOrCreateKeyValues("key", "description");

        assertThat(result.getDescription()).isEqualTo("description");

        assertThat(valuesStore.getValuesFor("key")).isEqualTo(result);
    }

    @Test
    void dumpShouldBeFormatted() {
        valuesStore.getOrCreateKeyValues("key", "description");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(byteArrayOutputStream);

        valuesStore.dump(out);

        assertThat(byteArrayOutputStream.toString()).contains("KeyValues for \"key\": KeyValues")
                .contains("description=\"description\"");
        assertThat(valuesStore.getAllValues()).hasSize(1);
    }

    @Test
    void valueShouldBeRemoved() {
        valuesStore.getOrCreateKeyValues("key", "description");

        KeyValues result = valuesStore.remove("key");

        assertThat(valuesStore.getValuesFor("key")).isNotEqualTo(result);
        assertThat(valuesStore.getValuesFor("key")).isNotEqualTo(keyValues);
        assertThat(valuesStore.getAllValues()).isEmpty();
    }

    @Test
    void valuesShouldBeReloaded() {
        Collection<KeyValues> values = new ArrayList<>();
        values.add(keyValues);
        when(persistence.reload(any(Collection.class), eq(domainSpecificValueFactory))).thenReturn(values);

        valuesStore.getOrCreateKeyValues("another key", null);
        valuesStore.reload();

        assertThat(valuesStore.getValuesFor("key")).isEqualTo(keyValues);
        assertThat(valuesStore.getValuesFor("another key")).isNull();
        assertThat(valuesStore.getAllValues()).hasSize(1);
    }

    @Test
    void reloadASingleKey() {
        final String key = "key";
        valuesStore.setWithChangeSet(key, "desc", null, "value", "dom1");
        when(persistence.load(key, domainSpecificValueFactory)).thenReturn(
                new KeyValues(key, domainSpecificValueFactory, "desc", List.of(DomainSpecificValue.withoutChangeSet("newValue", "dom1"))));
        valuesStore.reload(key);
        assertThat(valuesStore.getValuesFor(key).getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("newValue", "dom1"));
    }

    @Test
    void reloadASingleKeyThatIsNoLongerPresentRemovesIt() {
        final String key = "key";
        valuesStore.setWithChangeSet(key, "desc", null, "value", "dom1");
        when(persistence.load(key, domainSpecificValueFactory)).thenReturn(null);
        valuesStore.reload(key);
        assertThat(valuesStore.getValuesFor(key)).isNull();
    }
}
