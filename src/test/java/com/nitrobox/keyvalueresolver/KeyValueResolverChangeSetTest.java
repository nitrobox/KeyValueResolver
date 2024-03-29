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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author finsterwalder
 * @since 2013-09-23 16:45
 */
class KeyValueResolverChangeSetTest {

    private final KeyValueResolver keyValueResolver = new KeyValueResolverImpl();

    @Test
    void whenChangeSetsAreActiveTheValuesForTheChangeSetAreReturned() {
        keyValueResolver.set("key", "value", "descr");
        keyValueResolver.setWithChangeSet("key", "valueForChangeSet", "descr", "changeSet");

        DomainResolver resolverWithoutChangeSet = mock(DomainResolver.class);
        assertThat((String) keyValueResolver.get("key", resolverWithoutChangeSet)).isEqualTo("value");

        DomainResolver resolver = mock(DomainResolver.class);
        when(resolver.getActiveChangeSets()).thenReturn(Collections.singletonList("changeSet"));
        assertThat((String) keyValueResolver.get("key", resolver)).isEqualTo("valueForChangeSet");
    }

    @Test
    void whenSetWithChangeSetIsCalledChangeSetWillBePersisted() {
        KeyValueResolverImpl keyValueResolver = new KeyValueResolverImpl();
        Persistence persistenceMock = mock(Persistence.class);
        keyValueResolver.setPersistence(persistenceMock);

        keyValueResolver.setWithChangeSet("key", "valueForChangeSet", "descr", "changeSet");

        verify(persistenceMock).store(eq("key"), any(KeyValues.class), any(DomainSpecificValue.class));
    }
}
