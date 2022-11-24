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
 * Created by Benjamin Jochheim on 10.11.15.
 */
class DomainSpecificValueFactoryWithStringInterningTest {

    private final DomainSpecificValueFactory factory = new DomainSpecificValueFactoryWithStringInterning();

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
    void testInterningOfStrings() {

        final String value1 = new String("testString");
        final String value2 = new String("testString");
        assertThat(value1).isNotSameAs(value2)
                .isEqualTo(value2);

        DomainSpecificValue dsv1 = factory.create(value1, null, "DE", "de_DE");
        DomainSpecificValue dsv2 = factory.create(value2, null, "DE", "de_DE");

        assertThat(dsv1.getValue()).isSameAs(dsv2.getValue());
        assertThat((String) dsv1.getValue()).isEqualTo("testString");
    }
}
