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

package com.nitrobox.keyvalueresolver.jmx;

import com.nitrobox.keyvalueresolver.KeyValues;
import com.nitrobox.keyvalueresolver.KeyValueResolver;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author finsterwalder
 * @since 2013-05-28 12:08
 */
public class KeyValueResolverManager implements KeyValueResolverManagerMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyValueResolverManager.class);
    private static final KeyValueResolverManager instance = new KeyValueResolverManager();

    private final Map<KeyValueResolver, KeyValueResolver> roperties = new WeakHashMap<>();

    public static KeyValueResolverManager getInstance() {
        return instance;
    }

    public KeyValueResolverManager() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(this, new ObjectName("com.nitrobox.keyvalueresolver", "type", KeyValueResolverManagerMBean.class.getSimpleName()));
        } catch (InstanceAlreadyExistsException e) {
            // nothing to do
        } catch (Exception e) {
            LOGGER.warn("Could not register MBean for KeyValueResolver", e);
        }
    }

    public void add(KeyValueResolver keyValueResolver) {
        Objects.requireNonNull(keyValueResolver, "\"keyValueResolver\" must not be null");
        roperties.put(keyValueResolver, null);
    }

    @Override
    public String dump(String key) {
        StringBuilder builder = new StringBuilder(roperties.keySet().size() * 8);
        for (KeyValueResolver keyValueResolver : roperties.keySet()) {
            KeyValues keyValues = keyValueResolver.getKeyValues(key);
            if (keyValues != null) {
                builder.append(keyValues);
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    @Override
    public String dump() {
        StringBuilder builder = new StringBuilder(roperties.keySet().size() * 8);
        for (KeyValueResolver keyValueResolver : roperties.keySet()) {
            builder.append(keyValueResolver.dump());
            builder.append("\n\n");
        }
        return builder.toString();
    }

    @Override
    public void dumpToSystemOut() {
        for (KeyValueResolver keyValueResolver : roperties.keySet()) {
            keyValueResolver.dump(System.out);
            System.out.println();
        }
    }

    @Override
    public void reload() {
        for (KeyValueResolver keyValueResolver : roperties.keySet()) {
            keyValueResolver.reload();
        }
    }

    @Override
    public String listRoperties() {
        return roperties.keySet().toString();
    }

    public void reset() {
        roperties.clear();
    }

    public void remove(final KeyValueResolver keyValueResolver) {
        roperties.remove(keyValueResolver);
    }
}
