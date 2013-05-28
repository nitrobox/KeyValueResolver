/*
 * Roperty - An advanced property management and retrieval system
 * Copyright (C) 2013 PARSHIP GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parship.roperty;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author mfinsterwalder
 * @since 2013-03-25 08:07
 */
public class RopertyTest {

	private DomainResolver resolver = new DomainResolver() {
		@Override
		public String getDomainValue(final String domain) {
			return domain;
		}
	};
	private Roperty r = new Roperty();
	private RopertyWithResolver roperty = new RopertyWithResolver(r, resolver);

	@Test
	public void toStringTest() {
		assertThat(roperty.toString(), startsWith("RopertyWithResolver{roperty=Roperty{domains=[], map={}}, domainResolver="));
	}

	@Test
	public void gettingAPropertyThatDoesNotExistGivesNull() {
		String value = roperty.get("key");
		assertThat(value, nullValue());
	}

	@Test
	public void gettingAPropertyThatDoesNotExistQueriesPersistence() {
		Persistence persistenceMock = mock(Persistence.class);
		r.setPersistence(persistenceMock);
		roperty.get("key");
		verify(persistenceMock).load("key");
	}

	@Test
	public void gettingAPropertyThatDoesNotExistGivesDefaultValue() {
		String text = "default";
		String value = roperty.get("key", text);
		assertThat(value, is(text));
	}

	@Test
	public void	settingNullAsValue() {
		roperty.set("key", "value");
		assertThat((String)roperty.get("key"), is("value"));
		roperty.set("key", null);
		assertThat(roperty.get("key"), nullValue());
	}

	@Test
	public void	settingAnEmptyString() {
		roperty.set("key", "");
		assertThat((String)roperty.get("key"), is(""));
	}

	@Test
	public void definingAndGettingAStringValue() {
		String key = "key";
		String text = "some Value";
		roperty.set(key, text);
		String value = roperty.get(key, "default");
		assertThat(value, is(text));
	}

	@Test
	public void settingAValueCallsStoreOnPersistence() {
		String key = "key";
		Persistence persistenceMock = mock(Persistence.class);
		r.setPersistence(persistenceMock);
		KeyValues keyValue = new KeyValues();
		when(persistenceMock.load(key)).thenReturn(keyValue);
		roperty.set(key, "value");
		verify(persistenceMock).store(key, keyValue);
	}

	@Test
	public void gettingAValueWithoutAGivenDefaultGivesValue() {
		String text = "value";
		roperty.set("key", text);
		String value = roperty.get("key");
		assertThat(value, is(text));
	}

	@Test
	public void changingAStringValue() {
		roperty.set("key", "first");
		roperty.set("key", "other");
		String value = roperty.get("key", "default");
		assertThat(value, is("other"));
	}

	@Test
	public void gettingAnIntValueDefault() {
		int value = roperty.get("key", 3);
		assertThat(value, is(3));
	}

	@Test
	public void settingAndGettingAnIntValue() {
		roperty.set("key", 7);
		int value = roperty.get("key", 3);
		assertThat(value, is(7));
	}

	@Test(expected = ClassCastException.class)
	public void gettingAValueThatHasADifferentTypeGivesAClassCastException() {
		String text = "value";
		roperty.set("key", text);
		@SuppressWarnings("unused")
		Integer value = roperty.get("key");
	}

	@Test
	public void getOrDefineSetsAValueWithTheGivenDefault() {
		String text = "text";
		String value = roperty.getOrDefine("key", text);
		assertThat(value, is(text));
		value = roperty.getOrDefine("key", "other default");
		assertThat(value, is(text));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullDomainsAreNotAllowed() {
		r.addDomain(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyDomainsAreNotAllowed() {
		r.addDomain("");
	}

	@Test
	public void getOverriddenValue() {
		r.addDomain("domain1");
		roperty = new RopertyWithResolver(r, resolver);
		String defaultValue = "default value";
		String overriddenValue = "overridden value";
		roperty.set("key", defaultValue);
		roperty.set("key", overriddenValue, "domain1");
		String value = roperty.get("key");
		assertThat(value, is(overriddenValue));
	}

	@Test
	public void whenAKeyForASubdomainIsSetTheRootKeyGetsAnUndefinedValue() {
		roperty.set("key", "value", "subdomain");
		assertThat((String)roperty.get("key"), nullValue());
	}

	@Test
	public void theCorrectValueIsSelectedWhenAlternativeOverriddenValuesExist() {
		r.addDomain("domain1");
		roperty = new RopertyWithResolver(r, resolver);
		String overriddenValue = "overridden value";
		roperty.set("key", "other value", "other");
		roperty.set("key", overriddenValue, "domain1");
		roperty.set("key", "yet another value", "yet another");
		String value = roperty.get("key");
		assertThat(value, is(overriddenValue));
	}

	@Test
	public void theCorrectValueIsSelectedWhenAlternativeOverriddenValuesExistWithTwoDomains() {
		r.addDomain("domain1").addDomain("domain2");
		DomainResolver mockResolver = mock(DomainResolver.class);
		when(mockResolver.getDomainValue("domain1")).thenReturn("domVal1");
		when(mockResolver.getDomainValue("domain2")).thenReturn("domVal2");
		roperty = new RopertyWithResolver(r, mockResolver);
		String overriddenValue = "overridden value";
		roperty.set("key", "other value", "other");
		roperty.set("key", "domVal1", "domVal1");
		roperty.set("key", overriddenValue, "domVal1", "domVal2");
		roperty.set("key", "yet another value", "domVal1", "other");
		String value = roperty.get("key");
		assertThat(value, is(overriddenValue));
	}

	@Test
	public void getOverriddenValueTwoDomainsOnlyFirstDomainIsOverridden() {
		r.addDomain("domain1").addDomain("domain2");
		roperty = new RopertyWithResolver(r, resolver);
		String defaultValue = "default value";
		String overriddenValue1 = "overridden value domain1";
		roperty.set("key", defaultValue);
		roperty.set("key", overriddenValue1, "domain1");
		String value = roperty.get("key");
		assertThat(value, is(overriddenValue1));
	}

	@Test
	public void domainValuesAreRequestedFromAResolver() {
		roperty.getRoperty().addDomain("domain1").addDomain("domain2");
		DomainResolver mockResolver = mock(DomainResolver.class);
		roperty = new RopertyWithResolver(r, mockResolver);
		roperty.set("key", "value");
		roperty.get("key");
		verify(mockResolver).getDomainValue("domain1");
		verify(mockResolver).getDomainValue("domain2");
		verifyNoMoreInteractions(mockResolver);
	}

	@Test
	public void noDomainValuesAreRequestedWhenAKeyDoesNotExist() {
		r.addDomain("domain1").addDomain("domain2");
		DomainResolver mockResolver = mock(DomainResolver.class);
		roperty = new RopertyWithResolver(r, mockResolver);
		roperty.get("key");
		verifyNoMoreInteractions(mockResolver);
	}

	@Test
	public void wildcardIsResolvedWhenOtherDomainsMatch() {
		r.addDomain("domain1").addDomain("domain2");
		roperty = new RopertyWithResolver(r, resolver);
		String value = "overridden value";
		roperty.set("key", value, "*", "domain2");
		assertThat((String)roperty.get("key"), is(value));
	}

	@Test
	public void settingANewKeyMapReplacesAllMappings() {
		roperty.set("key", "value");
		r.setKeyValuesMap(new ConcurrentHashMap<String, KeyValues>());
		assertThat(roperty.get("key"), nullValue());
	}

	@Test
	public void aKeyThatIsNotPresentIsLoadedFromPersistenceAndThenInsertedIntoTheMap() {
		String key = "key";
		Map<String, KeyValues> mockMap = mock(HashMap.class);
		Persistence persistenceMock = mock(Persistence.class);
		KeyValues keyValues = new KeyValues();
		when(persistenceMock.load(key)).thenReturn(keyValues);
		r.setPersistence(persistenceMock);
		r.setKeyValuesMap(mockMap);
		roperty.get(key);
		verify(mockMap).put(key, keyValues);
	}

	@Test
	public void whenKeyValueIsNotInTheMapButCanBeLoadedFromPersistenceItIsOnlyInsertedInsideTheSynchronizedBlockWhenNotAlreadyThere() {
		String key = "key";
		Map<String, KeyValues> mockMap = mock(HashMap.class);
		Persistence persistenceMock = mock(Persistence.class);
		KeyValues keyValues = new KeyValues();
		when(persistenceMock.load(key)).thenReturn(keyValues);
		r.setPersistence(persistenceMock);
		when(mockMap.get(key)).thenReturn(null).thenReturn(new KeyValues());
		r.setKeyValuesMap(mockMap);
		roperty.get(key);
		verify(mockMap, never()).put(key, keyValues);
	}

	@Test
	public void domainsThatAreInitializedAreUsed() {
		Persistence persistenceMock = mock(Persistence.class);
		Roperty roperty1 = new Roperty(persistenceMock, "dom1", "dom2");
		roperty1.set("key", "value", "dom1");
		assertThat((String)roperty1.get("key", resolver), is("value"));
	}

	@Test
	public void persistenceThatIsInitializedIsUsed() {
		Persistence persistenceMock = mock(Persistence.class);
		Roperty roperty1 = new Roperty(persistenceMock, "dom1", "dom2");
		verify(persistenceMock).loadAll();
		roperty1.get("key", resolver);
		verify(persistenceMock).load("key");
	}

	@Test
	public void domainInitializerAndPersistenceAreUsedDuringInitialization() {
		Persistence persistenceMock = mock(Persistence.class);
		DomainInitializer domainInitializerMock = mock(DomainInitializer.class);
		new Roperty(persistenceMock, domainInitializerMock);
		verify(domainInitializerMock).getInitialDomains();
		verify(persistenceMock).loadAll();
	}
}
