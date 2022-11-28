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

import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyValueResolverGetAllMappingsTest {

    private final KeyValueResolverImpl keyValueResolver = new KeyValueResolverImpl();
    private final MapBackedDomainResolver resolver = new MapBackedDomainResolver();

    @Test
    void getAnEmptyMapFromAnEmptyKeyValueResolver() {
        assertThat(keyValueResolver.getAllMappings(resolver)).isEmpty();
    }

    @Test
    void keyValueResolverWithOneDefaultValueIsReturnedInMapping() {
        keyValueResolver.set("key", "value", "desc");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1)
                .containsAllEntriesOf(Map.of("key", "value"));
    }

    @Test
    void onlyTheBestMatchingValueIsUsed() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value", "desc");
        keyValueResolver.set("key", "value1", "desc", "*", "dom2");
        keyValueResolver.set("key", "value2", "desc", "dom1", "dom2");
        resolver.set("domain1", "dom1").set("domain2", "dom2");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1)
                .containsAllEntriesOf(Map.of("key", "value2"));
    }

    @Test
    void onlyTheBestMatchingValueIsUsedWithDomainValues() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value", "desc");
        MapBackedDomainValues domainValues1 = new MapBackedDomainValues().set("domain1","*").set("domain2","dom2");
        keyValueResolver.set("key", "value1", "desc", domainValues1);
        MapBackedDomainValues domainValue2 = new MapBackedDomainValues().set("domain2","dom2").set("domain1","dom1");
        keyValueResolver.set("key", "value2", "desc", domainValue2);
        resolver.set("domain1", "dom1").set("domain2", "dom2");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1)
                .containsAllEntriesOf(Map.of("key", "value2"));
    }

    @Test
    void partialMappingsAreReturned() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value1", "desc", "*", "dom2");
        keyValueResolver.set("key", "value2", "desc", "dom1", "dom2");
        keyValueResolver.set("key", "value3", "desc", "dom1", "*");
        keyValueResolver.set("key2", "otherValue", "desc");
        assertThat(keyValueResolver.getAllMappings("dom1")).hasSize(2)
                .containsAllEntriesOf(Map.of("key", "value3", "key2", "otherValue"));
    }

    @Test
    void keysWithoutAMatchingDomainValueAreNotReturned() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value1", "desc", "*", "dom2");
        keyValueResolver.set("key", "value2", "desc", "dom1", "dom2");
        keyValueResolver.set("key2", "otherValue", "desc");
        resolver.set("domain1", "dom1");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1)
                .containsAllEntriesOf(Map.of("key2", "otherValue"));
    }
}
