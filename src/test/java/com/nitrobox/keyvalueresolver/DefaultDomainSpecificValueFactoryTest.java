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

import org.junit.jupiter.api.Test;

/**
 * User: mjaeckel Date: 15.11.13 Time: 10:47
 */
class DefaultDomainSpecificValueFactoryTest {

    private final DomainSpecificValueFactory factory = new DefaultDomainSpecificValueFactory();

    @Test
    void factoryCreatesCorrectDSVForBaseKey() {
        String value = "value";
        DomainSpecificValue dsv = factory.create(value, null);
        assertThat((String) dsv.getValue()).isEqualTo(value);
        assertThat(dsv.getPattern()).isEmpty();
    }

    @Test
    void factoryCreatesCorrectDSVForOverriddenKey() {
        String value = "overriddenValue";

        DomainSpecificValue dsv = factory.create(value, null, "DE", "de_DE");

        assertThat((String) dsv.getValue()).isEqualTo(value);
        assertThat(dsv.getPattern()).isEqualTo("DE|de_DE|");
    }

    @Test
    void factorySetReverseOrderForMoreSpecificValues() {
        String value = "overriddenValue";

        DomainSpecificValue dsv = factory.create(value, null, "DE");
        DomainSpecificValue moreSpecificDsv = factory.create(value, null, "DE", "de_DE");

        assertThat(dsv.compareTo(moreSpecificDsv)).isGreaterThan(0);
    }
    
    @Test
    void valueCreatedFromPatternIsTheSameAsFromDomainValues() {
        final DomainSpecificValue value = factory.createFromPattern("value", null, "DE|de|*|XXX|");
        final DomainSpecificValue valueCompare = factory.create("value", null, "DE", "de", "*", "XXX");
        assertThat(value).isEqualTo(valueCompare);
    }
    
    @Test
    void patternMustEndWithPipe() {
        assertThrows(IllegalArgumentException.class, () -> factory.createFromPattern("value", null, "asdf"));
    }
}
