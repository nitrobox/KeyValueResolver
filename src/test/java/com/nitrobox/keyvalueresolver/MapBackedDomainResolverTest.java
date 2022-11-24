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

import org.junit.jupiter.api.Test;

/**
 * @author finsterwalder
 * @since 2013-05-15 15:26
 */
class MapBackedDomainResolverTest {

    private final MapBackedDomainResolver resolver = new MapBackedDomainResolver().set("dom1", "val1").set("dom2", "val2");

    @Test
    void setAndGetDomainValues() {
        assertThat(resolver.getDomainValue("dom1")).isEqualTo("val1");
    }

    @Test
    void setAndGetActiveChangeSets() {
        resolver.addActiveChangeSets("CS1", "CS2");
        assertThat(resolver.getActiveChangeSets()).containsExactlyInAnyOrder("CS1", "CS2");
        resolver.addActiveChangeSets("CS3");
        assertThat(resolver.getActiveChangeSets()).containsExactlyInAnyOrder("CS1", "CS2", "CS3");
    }

    @Test
    void toStringTest() {
        assertThat(resolver).hasToString("com.nitrobox.keyvalueresolver.MapBackedDomainResolver with {dom1=val1, dom2=val2}");
    }

    @Test
    void resolversWithSameValuesShouldBeEqual() {
        MapBackedDomainResolver aResolver = new MapBackedDomainResolver();
        aResolver.set("domain", "value");

        MapBackedDomainResolver anotherResolver = new MapBackedDomainResolver();
        anotherResolver.set("domain", "value");

        assertThat(aResolver).isEqualTo(anotherResolver)
                .hasSameHashCodeAs(anotherResolver.hashCode());
    }

    @Test
    void resolverShouldBeEqualToItself() {
        MapBackedDomainResolver aResolver = new MapBackedDomainResolver();
        assertThat(aResolver).isEqualTo(aResolver);
    }

    @Test
    void resolverShouldNotBeEqualToNull() {
        MapBackedDomainResolver aResolver = new MapBackedDomainResolver();
        assertThat(aResolver).isNotNull();
    }

    @Test
    void domainResolverOfDifferentClassShouldNotBeEqual() {
        MapBackedDomainResolver aResolver = new MapBackedDomainResolver();
        assertThat(aResolver).isNotEqualTo(new Object());
    }

    @Test
    void differentResolversShouldNotBeEqual() {
        MapBackedDomainResolver aResolver = new MapBackedDomainResolver();
        aResolver.set("domain", "value");

        MapBackedDomainResolver anotherResolver = new MapBackedDomainResolver();
        anotherResolver.set("anotherDomain", "anotherValue");

        assertThat(aResolver).isNotEqualTo(anotherResolver);
    }
}
