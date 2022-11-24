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

import java.util.Collection;
import java.util.Objects;


/**
 * Defines a value overridden for a specific domain. DomainSpecificValues are ordered according to the KeyValueResolver precedence rules for
 * resolution of domain values. DomainSpecificValues can belong to a changeSet, which allows temporary changes which take precedence, when
 * the changeSet is active and are ignored, when the changeSet is not active. ChangeSets are activated by the DomainResolver.
 *
 * @author finsterwalder
 * @since 2013-04-09 18:20
 */
public class DomainSpecificValue implements Comparable<DomainSpecificValue> {

    private final String pattern;
    private final int ordering;
    private Object value;
    private final Matcher matcher;
    private final String changeSet;

    public static DomainSpecificValue withChangeSet(Object value, String changeSet, String... domainValues) {
        return new DomainSpecificValue(value, changeSet, domainValues);
    }

    public static DomainSpecificValue withoutChangeSet(Object value, String... domainValues) {
        return new DomainSpecificValue(value, null, domainValues);
    }

    public static DomainSpecificValue withPattern(Object value, String changeSet, String pattern) {
        if (pattern == null || pattern.trim().length() == 0) {
            return new DomainSpecificValue(value, changeSet, "", 1);
        }
        if (!pattern.endsWith("|")) {
            throw new IllegalArgumentException("Pattern must end with a pipe character: '|'");
        }

        final String[] domainValues = pattern.split("\\|");
        int order = getOrder(domainValues);
        return new DomainSpecificValue(value, changeSet, pattern, order);
    }

    private static int getOrder(String[] domainValues) {
        int order = 1;
        int i = 0;
        for (String domainValue : domainValues) {
            i++;
            if (!"*".equals(domainValue)) {
                order = order | (int) Math.pow(2, i);
            }
        }
        return order;
    }

    private DomainSpecificValue(Object value, String changeSet, String pattern, int ordering) {
        this.ordering = ordering;
        this.pattern = pattern;
        this.value = value;
        this.changeSet = changeSet;
        this.matcher = createMatcher(pattern);
    }

    private DomainSpecificValue(Object value, String changeSet, String[] domainValues) {
        this.ordering = getOrder(domainValues);
        this.pattern = domainValues.length == 0 ? "" : String.join("|", domainValues) + "|";
        this.value = value;
        this.changeSet = changeSet;
        this.matcher = createMatcher(pattern);
    }

    private Matcher createMatcher(String pattern) {
        if (pattern.contains("*")) {
            return new RegexMatcher(pattern.replaceAll("\\|", "\\\\|").replaceAll("\\*", "[^|]*") + ".*");
        } else {
            return new StringPrefixMatcher(pattern);
        }
    }

    /**
     * Sort DomainSpecificValue in reverse order as specified by ordering, changeSet and patternStr. This ordering defines the order of
     * resolution that KeyValueResolver uses when a key is accessed. Values with a changeSet are ordered before values without a changeSet.
     * Values with a changeSet are ordered alphabetically with other changeSets. A value from changeSet "A_ChangeSet" is chosen before a
     * value in changeSet "B_ChangeSet". Values with the same ordering (and changeSet) are ordered by patternStr, just to define a
     * consistent ordering.
     */
    @Override
    public int compareTo(final DomainSpecificValue other) {
        int order = other.ordering - this.ordering;
        if (order == 0) {
            if (changeSet != null && other.changeSet != null) {
                int changeSetCompare = changeSet.compareTo(other.changeSet);
                if (changeSetCompare != 0) {
                    return changeSetCompare;
                } else {
                    return pattern.compareTo(other.pattern);
                }
            }
            if (changeSet != null) { // other.changeSet is null here
                return -1;
            }
            if (other.changeSet != null) { // changeSet is null here
                return 1;
            }
            return pattern.compareTo(other.pattern);
        }
        return order;
    }

	@Override
	public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DomainSpecificValue that = (DomainSpecificValue) o;

        if (ordering != that.ordering)
            return false;
        if (!pattern.equals(that.pattern))
            return false;
        if (!value.equals(that.value))
            return false;
        return Objects.equals(changeSet, that.changeSet);
    }

    @Override
    public int hashCode() {
        int result = pattern.hashCode();
        result = 31 * result + ordering;
        result = 31 * result + value.hashCode();
        result = 31 * result + (changeSet != null ? changeSet.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DomainSpecificValue{" +
                "pattern=\"" + pattern +
                "\", ordering=" + ordering +
                (changeSet != null ? ", changeSet=\"" + changeSet + '"' : "") +
                ", value=\"" + value + "\"}";
    }

    public String getPattern() {
        return pattern;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    /**
     * This method is used to determine, whether this DomainSpecificValue matches the provided domain string
     */
    public boolean patternMatches(final String domainStr) {
        return matcher.matches(domainStr);
    }

    public boolean isInChangeSets(final Collection<String> activeChangeSets) {
        return noChangeSet() || activeChangeSets.contains(changeSet);
    }

    public boolean changeSetIs(final String changeSet) {
        return Objects.equals(this.changeSet, changeSet);
    }

    public boolean noChangeSet() {
        return changeSet == null;
    }

    public int compareChangeSet(DomainSpecificValue other) {
        return changeSet.compareTo(other.changeSet);
    }

    public String getChangeSet() {
        return changeSet;
    }

    /**
     * This method is used for finding all DomainSpecificValues, that are either default or are in a specific (partial) domain.
     */
    public boolean patternMatches(Matcher matcher, DomainResolver resolver) {
        return isInChangeSets(resolver.getActiveChangeSets()) && matcher.matches(pattern);
    }

    public boolean isDefault() {
        return pattern.length() == 0;
    }

    public String[] getDomainValues() {
        if (isDefault()) {
            return new String[0];
        }
        return pattern.split("\\|");
    }
}
