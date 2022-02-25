/*
 * Roperty - An advanced property management and retrival system
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author finsterwalder
 * @since 2013-03-25 08:07
 */
@ExtendWith(MockitoExtension.class)
public class KeyValueResolverImplTest {

    @Mock(lenient = true)
    private DomainResolver resolverMock;

    @Mock
    private Persistence persistenceMock;

    private KeyValueResolverImpl keyValueResolver;

    @BeforeEach
    void setUp() {
        when(resolverMock.getActiveChangeSets()).thenReturn(new ArrayList<>());
        when(resolverMock.getDomainValue(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        keyValueResolver = new KeyValueResolverImpl();
    }

    @Test
    void keyMayNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.get(null));
    }

    @Test
    void keyMayNotBeNullWithDefault() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.getOrDefine(null, "default", resolverMock));
    }

    @Test
    void keyMayNotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.get(""));
    }

    @Test
    void keyMayNotBeEmptyWithDefault() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.getOrDefine("", "default", resolverMock));
    }

    @Test
    void canNotSetValueForNullKey() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.set(null, "value", "descr"));
    }

    @Test
    void canNotSetValueForEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.set("", "value", "descr"));
    }

    @Test
    void gettingAPropertyThatDoesNotExistGivesNull() {
        String value = keyValueResolver.get("key", resolverMock);
        assertThat(value, nullValue());
    }

    @Test
    void gettingAPropertyThatDoesNotExistQueriesPersistence() {
        keyValueResolver.setPersistence(persistenceMock);
        keyValueResolver.get("key", resolverMock);
        verify(persistenceMock).load(eq("key"), any(DomainSpecificValueFactory.class));
    }

    @Test
    void gettingAPropertyThatDoesNotExistGivesDefaultValue() {
        String text = "default";
        String value = keyValueResolver.getOrDefault("key", text, resolverMock);
        assertThat(value, is(text));
    }

    @Test
    void settingNullAsValue() {
        keyValueResolver.set("key", "value", null);
        assertThat(keyValueResolver.get("key", resolverMock), is("value"));
        keyValueResolver.set("key", null, null);
        assertThat(keyValueResolver.get("key", resolverMock), nullValue());
    }

    @Test
    void settingAnEmptyString() {
        keyValueResolver.set("key", "", null);
        assertThat(keyValueResolver.get("key", resolverMock), is(""));
    }

    @Test
    void keysAreAlwaysTrimmed() {
        keyValueResolver.set("  key   ", "val", "descr");
        assertThat(keyValueResolver.get(" key", resolverMock), is("val"));
    }

    @Test
    void definingAndGettingAStringValue() {
        String key = "key";
        String text = "some Value";
        keyValueResolver.set(key, text, null);
        String value = keyValueResolver.getOrDefault(key, "default", resolverMock);
        assertThat(value, is(text));
    }

    @Test
    void settingAValueCallsStoreOnPersistence() {
        String key = "key";
        keyValueResolver.setPersistence(persistenceMock);
        KeyValues keyValue = new KeyValues(key, new DefaultDomainSpecificValueFactory(), null);
        when(persistenceMock.load(eq(key), any(DomainSpecificValueFactory.class))).thenReturn(keyValue);
        keyValueResolver.set(key, "value", null);
        verify(persistenceMock).store(eq(key), eq(keyValue), any());
    }

    @Test
    void gettingAValueWithoutAGivenDefaultGivesValue() {
        String text = "value";
        keyValueResolver.set("key", text, null);
        String value = keyValueResolver.get("key", resolverMock);
        assertThat(value, is(text));
    }

    @Test
    void changingAStringValue() {
        keyValueResolver.set("key", "first", null);
        keyValueResolver.set("key", "other", null);
        String value = keyValueResolver.getOrDefault("key", "default", resolverMock);
        assertThat(value, is("other"));
    }

    @Test
    void gettingAnIntValueThatDoesNotExistGivesDefault() {
        int value = keyValueResolver.getOrDefault("key", 3, resolverMock);
        assertThat(value, is(3));
    }

    @Test
    void settingAndGettingAnIntValueWithDefaultGivesStoredValue() {
        keyValueResolver.set("key", 7, null);
        int value = keyValueResolver.getOrDefault("key", 3, resolverMock);
        assertThat(value, is(7));
    }

    @Test
    void gettingAValueThatHasADifferentTypeGivesAClassCastException() {
        String text = "value";
        keyValueResolver.set("key", text, null);
        assertThrows(ClassCastException.class, () -> {
            Integer value = keyValueResolver.get("key", resolverMock);
        });
    }

    @Test
    void getOrDefineSetsAValueWithTheGivenDefault() {
        String text = "text";
        String value = keyValueResolver.getOrDefine("key", text, "descr", resolverMock);
        assertThat(value, is(text));
        value = keyValueResolver.getOrDefine("key", "other default", resolverMock);
        assertThat(value, is(text));
    }

    @Test
    void nullDomainsAreNotAllowed() {
        assertThrows(NullPointerException.class, () -> keyValueResolver.addDomains((String[]) null));
    }

    @Test
    void emptyDomainsAreNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> keyValueResolver.addDomains(""));
    }

    @Test
    void getOverriddenValue() {
        keyValueResolver.addDomains("domain1");
        String defaultValue = "default value";
        String overriddenValue = "overridden value";
        keyValueResolver.set("key", defaultValue, null);
        keyValueResolver.set("key", overriddenValue, null, "domain1");
        String value = keyValueResolver.get("key", resolverMock);
        assertThat(value, is(overriddenValue));
    }

    @Test
    void whenAKeyForASubdomainIsSetTheRootKeyGetsANullValue() {
        keyValueResolver.set("key", "value", "descr", "subdomain");
        assertThat(keyValueResolver.get("key", resolverMock), nullValue());
    }

    @Test
    void theCorrectValueIsSelectedWhenAlternativeOverriddenValuesExist() {
        keyValueResolver.addDomains("domain1");
        String overriddenValue = "overridden value";
        keyValueResolver.set("key", "other value", null, "other");
        keyValueResolver.set("key", overriddenValue, null, "domain1");
        keyValueResolver.set("key", "yet another value", null, "yet another");
        String value = keyValueResolver.get("key", resolverMock);
        assertThat(value, is(overriddenValue));
    }

    @Test
    void theCorrectValueIsSelectedWhenAlternativeOverriddenValuesExistWithTwoDomains() {
        keyValueResolver.addDomains("domain1", "domain2");
        DomainResolver mockResolver = mock(DomainResolver.class);
        when(mockResolver.getDomainValue("domain1")).thenReturn("domVal1");
        when(mockResolver.getDomainValue("domain2")).thenReturn("domVal2");
        String overriddenValue = "overridden value";
        keyValueResolver.set("key", "other value", null, "other");
        keyValueResolver.set("key", "domVal1", null, "domVal1");
        keyValueResolver.set("key", overriddenValue, null, "domVal1", "domVal2");
        keyValueResolver.set("key", "yet another value", null, "domVal1", "other");
        String value = keyValueResolver.get("key", mockResolver);
        assertThat(value, is(overriddenValue));
    }

    @Test
    void getOverriddenValueTwoDomainsOnlyFirstDomainIsOverridden() {
        keyValueResolver.addDomains("domain1", "domain2");
        String defaultValue = "default value";
        String overriddenValue1 = "overridden value domain1";
        keyValueResolver.set("key", defaultValue, null);
        keyValueResolver.set("key", overriddenValue1, null, "domain1");
        String value = keyValueResolver.get("key", resolverMock);
        assertThat(value, is(overriddenValue1));
    }

    @Test
    void domainValuesAreRequestedFromAResolver() {
        keyValueResolver.addDomains("domain1", "domain2");
        DomainResolver mockResolver = mock(DomainResolver.class);
        keyValueResolver.set("key", "value", null);
        keyValueResolver.get("key", mockResolver);
        verify(mockResolver).getDomainValue("domain1");
        verify(mockResolver).getDomainValue("domain2");
        verify(mockResolver).getActiveChangeSets();
        verifyNoMoreInteractions(mockResolver);
    }

    @Test
    void noDomainValuesAreRequestedWhenAKeyDoesNotExist() {
        keyValueResolver.addDomains("domain1", "domain2");
        DomainResolver mockResolver = mock(DomainResolver.class);
        keyValueResolver.get("key", mockResolver);
        verifyNoMoreInteractions(mockResolver);
    }

    @Test
    void wildcardIsResolvedWhenOtherDomainsMatch() {
        keyValueResolver.addDomains("domain1", "domain2");
        String value = "overridden value";
        keyValueResolver.set("key", value, null, "*", "domain2");
        assertThat(keyValueResolver.get("key", resolverMock), is(value));
    }

    @Test
    void aKeyThatIsNotPresentIsLoadedFromPersistenceAndThenInsertedIntoTheValueStore() {
        String key = "key";
        KeyValues keyValues = new KeyValues(key, new DefaultDomainSpecificValueFactory(), null);
        when(persistenceMock.load(eq(key), any(DomainSpecificValueFactory.class))).thenReturn(keyValues);
        keyValueResolver.setPersistence(persistenceMock);
        keyValueResolver.get(key);
        assertThat(keyValueResolver.getKeyValues(key), notNullValue());
    }

    @Test
    void domainsThatAreInitializedAreUsed() {
        KeyValueResolver keyValueResolver1 = new KeyValueResolverImpl(persistenceMock, "dom1", "dom2");
        keyValueResolver1.set("key", "value", "dom1");
        assertThat(keyValueResolver1.get("key", resolverMock), is("value"));
    }

    @Test
    void persistenceThatIsInitializedIsUsed() {
        KeyValueResolver keyValueResolver1 = new KeyValueResolverImpl(persistenceMock, "dom1", "dom2");
        verify(persistenceMock).loadAll(any(DomainSpecificValueFactory.class));
        keyValueResolver1.get("key", resolverMock);
        verify(persistenceMock).load(eq("key"), any(DomainSpecificValueFactory.class));
    }

    @Test
    void domainInitializerAndPersistenceAreUsedDuringInitialization() {
        new KeyValueResolverImpl(persistenceMock);
        verify(persistenceMock).loadAll(any(DomainSpecificValueFactory.class));
    }

    @Test
    void reloadReplacesKeyValuesMap() {
        KeyValueResolverImpl keyValueResolver1 = new KeyValueResolverImpl(persistenceMock);
        verify(persistenceMock).loadAll(any(DomainSpecificValueFactory.class));
        keyValueResolver1.set("key", "value", "descr");
        assertThat(keyValueResolver1.get("key"), is("value"));
        keyValueResolver1.reload();
        verify(persistenceMock).reload(any(Collection.class), any(DomainSpecificValueFactory.class));
    }

    @Test
    void reloadWithoutPersistenceDoesNothing() {
        keyValueResolver.set("key", "value", "descr");
        keyValueResolver.reload();
        assertThat(keyValueResolver.get("key"), is("value"));
    }

    @Test
    void domainsThatAreInitializedArePresent() {
        keyValueResolver = new KeyValueResolverImpl("domain1", "domain2");
        assertThat(keyValueResolver.dump().toString(), is("KeyValueResolver{domains=[domain1, domain2]\n}"));
    }

    @Test
    void getKeyValues() {
        String key = "key";
        keyValueResolver.set(key, "value", null);
        KeyValues keyValues = keyValueResolver.getKeyValues(key);
        assertThat(keyValues.getDomainSpecificValues(), hasSize(1));
        String value = keyValues.get(new ArrayList<>(), null, null);
        assertThat(value, is("value"));
    }

    @Test
    void getKeyValuesTrimsTheKey() {
        keyValueResolver.set("key", "value", null);
        assertThat(keyValueResolver.getKeyValues("  key"), notNullValue());
    }

    @Test
    void keyValueResolverToString() {
        assertThat(keyValueResolver.toString(), is("KeyValueResolver{domains=[]}"));
    }

    @Test
    void toStringEmptyKeyValueResolver() {
        assertThat(keyValueResolver.dump().toString(), is("KeyValueResolver{domains=[]\n}"));
        keyValueResolver.addDomains("domain");
        assertThat(keyValueResolver.dump().toString(), is("KeyValueResolver{domains=[domain]\n}"));
    }

    @Test
    void toStringFilledKeyValueResolver() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value", null);
        keyValueResolver.set("key", "value2", null, "domain1");
        keyValueResolver.set(" otherKey ", "otherValue", null); // keys are always trimmed
        assertThat(keyValueResolver.dump().toString(), containsString(""));

        assertThat(keyValueResolver.dump().toString(), containsString("KeyValueResolver{domains=[domain1, domain2]\n"));
        assertThat(keyValueResolver.dump().toString(), containsString("KeyValues for \"otherKey\": KeyValues{\n"));
        assertThat(keyValueResolver.dump().toString(), containsString("\tdescription=\"\"\n"));
        assertThat(keyValueResolver.dump().toString(), containsString("\tDomainSpecificValue{pattern=\"\", ordering=1, value=\"otherValue\""));

        assertThat(keyValueResolver.dump().toString(), containsString("KeyValues for \"key\": KeyValues{\n"));
        assertThat(keyValueResolver.dump().toString(), containsString("\tdescription=\"\"\n"));
        assertThat(keyValueResolver.dump().toString(),
            containsString("\tDomainSpecificValue{pattern=\"domain1|\", ordering=3, value=\"value2\""));
        assertThat(keyValueResolver.dump().toString(), containsString("\tDomainSpecificValue{pattern=\"\", ordering=1, value=\"value\""));

    }

    @Test
    void dumpToStdout() throws UnsupportedEncodingException {
        keyValueResolver.addDomains("dom1");
        keyValueResolver.set("key", "value", "descr");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        keyValueResolver.dump(new PrintStream(os));
        String output = os.toString("UTF8");
        assertThat(output,
            is("KeyValueResolver{domains=[dom1]\nKeyValues for \"key\": KeyValues{\n\tdescription=\"descr\"\n\tDomainSpecificValue{pattern=\"\", ordering=1, value=\"value\"}\n}\n}\n"));
    }

    @Test
    void domainResolverToNullIsIgnored() {
        DomainResolver domainResolver = new MapBackedDomainResolver().set("dom", "domVal");
        keyValueResolver.addDomains("dom", "dom2", "dom3");
        keyValueResolver.get("key", domainResolver);
        keyValueResolver.set("key", "value", "desc");
        keyValueResolver.set("key", "valueDom", "desc", "domVal");
        keyValueResolver.set("key", "valueDom2", "desc", "domVal", "dom2");
        keyValueResolver.set("key", "valueDom3", "desc", "domVal", "dom2", "dom3");
        assertThat(keyValueResolver.get("key", domainResolver), is("valueDom"));
    }

    @Test
    void removeDefaultValue() {
        KeyValueResolverImpl kvrWithPersistence = new KeyValueResolverImpl(persistenceMock);
        kvrWithPersistence.addDomains("dom1");
        kvrWithPersistence.set("key", "value", "desc");
        kvrWithPersistence.set("key", "domValue", "desc", "dom1");

        kvrWithPersistence.remove("key");

        verify(persistenceMock).remove("key", new DefaultDomainSpecificValueFactory().create("value", null));
        assertThat(kvrWithPersistence.get("key", mock(DomainResolver.class)), nullValue());
        assertThat(kvrWithPersistence.get("key", resolverMock), is("domValue"));
    }

    @Test
    void removeDomainSpecificValue() {
        KeyValueResolverImpl kvrWithPersistence = new KeyValueResolverImpl(persistenceMock);
        kvrWithPersistence.addDomains("dom1", "dom2");
        kvrWithPersistence.set("key", "value", "desc");
        kvrWithPersistence.set("key", "domValue1", "desc", "dom1");
        kvrWithPersistence.set("key", "domValue2", "desc", "dom1", "dom2");

        kvrWithPersistence.remove("key", "dom1");

        verify(persistenceMock).remove("key", new DefaultDomainSpecificValueFactory().create("domValue1", null, "dom1"));
        assertThat(kvrWithPersistence.get("key", mock(DomainResolver.class)), is("value"));
        assertThat(kvrWithPersistence.get("key", resolverMock), is("domValue2"));
    }

    @Test
    void removeDoesNotCallPersistenceWhenNoDomainSpecificValueExists() {
        KeyValueResolverImpl kvrWithPersistence = new KeyValueResolverImpl(persistenceMock);
        kvrWithPersistence.remove("key", "dom1");
        verify(persistenceMock, never()).remove(any(), any());
    }

    @Test
    void removeACompleteKey() {
        KeyValueResolverImpl kvrWithPersistence = new KeyValueResolverImpl(persistenceMock);
        kvrWithPersistence.set("key", "value", "desc");
        kvrWithPersistence.set("key", "domValue1", "desc", "dom1");
        kvrWithPersistence.removeKey("key");
        verify(persistenceMock).remove("key");
        assertThat(kvrWithPersistence.get("key", resolverMock), nullValue());
    }

    @Test
    void removeCallsPersistenceEvenWhenNoKeyExists() {
        KeyValueResolverImpl kvrWithPersistence = new KeyValueResolverImpl(persistenceMock);
        kvrWithPersistence.removeKey("key");
        verify(persistenceMock).remove("key");
    }

    @Test
    void removeKeyFromChangeSet() {
        keyValueResolver.set("key", "value", "descr");
        keyValueResolver.setWithChangeSet("key", "valueChangeSet", "descr", "changeSet");
        DomainResolver resolver = new MapBackedDomainResolver().addActiveChangeSets("changeSet");
        assertThat(keyValueResolver.get("key", resolver), is("valueChangeSet"));
        keyValueResolver.removeWithChangeSet("key", "changeSet");
        assertThat(keyValueResolver.get("key", resolver), is("value"));
    }

    @Test
    void removeAChangeSet() {
        KeyValueResolverImpl kvrWithPersistence = new KeyValueResolverImpl(persistenceMock);
        kvrWithPersistence.set("key", "value", "descr");
        kvrWithPersistence.setWithChangeSet("key", "valueChangeSet", "descr", "changeSet");
        kvrWithPersistence.setWithChangeSet("otherKey", "otherValueChangeSet", "descr", "changeSet");
        DomainResolver resolver = new MapBackedDomainResolver().addActiveChangeSets("changeSet");
        assertThat(kvrWithPersistence.get("key", resolver), is("valueChangeSet"));
        assertThat(kvrWithPersistence.get("otherKey", resolver), is("otherValueChangeSet"));
        kvrWithPersistence.removeChangeSet("changeSet");
        verify(persistenceMock).remove("key", new DefaultDomainSpecificValueFactory().create("valueChangeSet", "changeSet"));
        verify(persistenceMock).remove("otherKey", new DefaultDomainSpecificValueFactory().create("otherValueChangeSet", "changeSet"));
        assertThat(kvrWithPersistence.get("key", resolver), is("value"));
        assertThat(kvrWithPersistence.<String>get("otherKey", resolver), nullValue());
    }

    @Test
    void removeChangeSetThrowsIllegalArgumentException() {
        KeyValueResolverImpl keyValueResolver = new KeyValueResolverImpl(persistenceMock);

        assertThrows(NullPointerException.class, () -> keyValueResolver.removeChangeSet(null));
    }

    @Test
    void removeUnexistingChangeSet() {
        keyValueResolver.removeChangeSet("notExistingChangeSet");

        verifyNoInteractions(persistenceMock);
    }
}
