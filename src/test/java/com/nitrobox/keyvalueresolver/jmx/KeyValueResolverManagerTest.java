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

package com.nitrobox.keyvalueresolver.jmx;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.nitrobox.keyvalueresolver.KeyValueResolver;
import com.nitrobox.keyvalueresolver.KeyValueResolverImpl;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author finsterwalder
 * @since 2013-06-07 08:49
 */
public class KeyValueResolverManagerTest {

    private KeyValueResolverManager manager = KeyValueResolverManager.getInstance();

    @BeforeEach
    void before() {
        manager.reset();
    }

    @Test
    void keyValueResolverInstancesRegisterThemselvesWithTheManager() {
        assertThat(manager.dump()).isEqualTo("");
        new KeyValueResolverImpl();
        assertThat(manager.dump()).isEqualTo("KeyValueResolver{domains=[]\n}\n\n");
    }

    @Test
    void keyValueResolverInstancesAreRemovedFromManagerAfterDestruction() {
        KeyValueResolver keyValueResolver = new KeyValueResolverImpl();
        assertThat(manager.dump()).isEqualTo("KeyValueResolver{domains=[]\n}\n\n");
        keyValueResolver = null;
        System.gc();
        assertThat(manager.dump()).isEqualTo("");
    }

    @Test
    void dumpSingleKeyIsDelegatedToAllRoperties() {
        final String key = "key";
        KeyValueResolver keyValueResolver1 = new KeyValueResolverImpl();
        keyValueResolver1.set(key, "value1", "descr");
        KeyValueResolver keyValueResolver2 = new KeyValueResolverImpl();
        keyValueResolver2.set(key, "value2", "descr");
        String dump = manager.dump("key");
        assertThat(dump).contains("KeyValues{\n" +
            "\tdescription=\"descr\"\n\tDomainSpecificValue{pattern=\"\", ordering=1, value=\"value1\"}\n}");
        assertThat(dump).contains("KeyValues{\n" +
            "\tdescription=\"descr\"\n\tDomainSpecificValue{pattern=\"\", ordering=1, value=\"value2\"}\n}");
    }

    @Test
    void reloadIsDelegatedToAllRoperties() {
        KeyValueResolver keyValueResolverMock1 = mock(KeyValueResolverImpl.class);
        manager.add(keyValueResolverMock1);
        KeyValueResolver keyValueResolverMock2 = mock(KeyValueResolverImpl.class);
        manager.add(keyValueResolverMock2);
        manager.reload();
        verify(keyValueResolverMock1).reload();
        verify(keyValueResolverMock2).reload();
    }

    @Test
    void removedRopertiesAreNotCalled() {
        KeyValueResolver keyValueResolverMock1 = mock(KeyValueResolverImpl.class);
        manager.add(keyValueResolverMock1);
        KeyValueResolver keyValueResolverMock2 = mock(KeyValueResolverImpl.class);
        manager.add(keyValueResolverMock2);
        manager.reload();
        verify(keyValueResolverMock1).reload();
        verify(keyValueResolverMock2).reload();
        manager.remove(keyValueResolverMock1);
        manager.reload();
        verify(keyValueResolverMock1).reload();
        verify(keyValueResolverMock2, times(2)).reload();
    }

    @Test
    void listRoperties() {
        KeyValueResolver r1 = new KeyValueResolverImpl().addDomains("dom1");
        KeyValueResolver r2 = new KeyValueResolverImpl().addDomains("dom2");
        assertThat(manager.listRoperties()).contains("KeyValueResolver{domains=[dom1]}");
        assertThat(manager.listRoperties()).contains("KeyValueResolver{domains=[dom2]}");
    }

    @Test
    void dumpsToSystemOut() {
        PrintStream out = mock(PrintStream.class);
        System.setOut(out);

        KeyValueResolver keyValueResolver1 = mock(KeyValueResolver.class);
        manager.add(keyValueResolver1);

        KeyValueResolver keyValueResolver2 = mock(KeyValueResolver.class);
        manager.add(keyValueResolver2);

        manager.dumpToSystemOut();

        verify(keyValueResolver1).dump(out);
        verify(keyValueResolver2).dump(out);

        verify(out, times(2)).println();
    }

    @Test
    void ignoresInstanceAlreadyExistsException() {
        new KeyValueResolverManager();
        new KeyValueResolverManager();
    }
}
