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

/**
 * @author finsterwalder
 * @since 2013-06-03 14:34
 */
public class DefaultDomainSpecificValueFactory implements DomainSpecificValueFactory {

    @Override
    public DomainSpecificValue create(final Object value, final String changeSet, final String... domainValues) {
        return DomainSpecificValue.withChangeSet(value, changeSet, domainValues);
    }

    @Override
    public DomainSpecificValue createFromPattern(Object value, String changeSet, String pattern) {
        return DomainSpecificValue.withPattern(value, changeSet, pattern);
    }
}
