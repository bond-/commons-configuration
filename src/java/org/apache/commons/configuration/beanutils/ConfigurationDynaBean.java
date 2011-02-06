/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.configuration.beanutils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationMap;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <tt>ConfigurationDynaBean</tt> dynamically reads and writes
 * configurations properties from a wrapped configuration-collection
 * {@link org.apache.commons.configuration.Configuration} instance. It also
 * implements a {@link java.util.Map} interface so that it can be used in
 * JSP 2.0 Expression Language expressions.
 *
 * <p>The <code>ConfigurationDynaBean</code> maps nested and mapped properties
 * to the appropriate <code>Configuration</code> subset using the
 * {@link org.apache.commons.configuration.Configuration#subset}
 * method. Similarly, indexed properties reference lists of configuration
 * properties using the
 * {@link org.apache.commons.configuration.Configuration#getList(String)}
 * method. Setting an indexed property is supported, too.</p>
 *
 * <p>Note: Some of the methods expect that a dot (&quot;.&quot;) is used as
 * property delimiter for the wrapped configuration. This is true for most of
 * the default configurations. Hierarchical configurations, for which a specific
 * expression engine is set, may cause problems.</p>
 *
 * @author <a href="mailto:ricardo.gladwell@btinternet.com">Ricardo Gladwell</a>
 * @version $Id$
 * @since 1.0-rc1
 */
public class ConfigurationDynaBean extends ConfigurationMap implements DynaBean
{
    /** Constant for the property delimiter.*/
    private static final String PROPERTY_DELIMITER = ".";

    /** The logger.*/
    private static Log log = LogFactory.getLog(ConfigurationDynaBean.class);

    /**
     * Creates a new instance of <code>ConfigurationDynaBean</code> and sets
     * the configuration this bean is associated with.
     *
     * @param configuration the configuration
     */
    public ConfigurationDynaBean(Configuration configuration)
    {
        super(configuration);
        if (log.isTraceEnabled())
        {
            log.trace("ConfigurationDynaBean(" + configuration + ")");
        }
    }

    public void set(String name, Object value)
    {
        if (log.isTraceEnabled())
        {
            log.trace("set(" + name + "," + value + ")");
        }

        if (value == null)
        {
            throw new NullPointerException("Error trying to set property to null.");
        }

        if (value instanceof Collection)
        {
            Collection collection = (Collection) value;
            Iterator iterator = collection.iterator();
            while (iterator.hasNext())
            {
                getConfiguration().addProperty(name, iterator.next());
            }
        }
        else if (value.getClass().isArray())
        {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++)
            {
                getConfiguration().addProperty(name, Array.get(value, i));
            }
        }
        else
        {
            getConfiguration().setProperty(name, value);
        }
    }

    public Object get(String name)
    {
        if (log.isTraceEnabled())
        {
            log.trace("get(" + name + ")");
        }

        // get configuration property
        Object result = getConfiguration().getProperty(name);
        if (result == null)
        {
            // otherwise attempt to create bean from configuration subset
            Configuration subset = new SubsetConfiguration(getConfiguration(), name, PROPERTY_DELIMITER);
            if (!subset.isEmpty())
            {
                result = new ConfigurationDynaBean(subset);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(name + "=[" + result + "]");
        }

        if (result == null)
        {
            throw new IllegalArgumentException("Property '" + name + "' does not exist.");
        }
        return result;
    }

    public boolean contains(String name, String key)
    {
        Configuration subset = getConfiguration().subset(name);
        if (subset == null)
        {
            throw new IllegalArgumentException("Mapped property '" + name + "' does not exist.");
        }

        return subset.containsKey(key);
    }

    public Object get(String name, int index)
    {
        if (!checkIndexedProperty(name))
        {
            throw new IllegalArgumentException("Property '" + name
                    + "' is not indexed.");
        }

        List list = getConfiguration().getList(name);
        return list.get(index);
    }

    public Object get(String name, String key)
    {
        Configuration subset = getConfiguration().subset(name);
        if (subset == null)
        {
            throw new IllegalArgumentException("Mapped property '" + name + "' does not exist.");
        }

        return subset.getProperty(key);
    }

    public DynaClass getDynaClass()
    {
        return new ConfigurationDynaClass(getConfiguration());
    }

    public void remove(String name, String key)
    {
        Configuration subset = new SubsetConfiguration(getConfiguration(), name, PROPERTY_DELIMITER);
        subset.setProperty(key, null);
    }

    public void set(String name, int index, Object value)
    {
        if (!checkIndexedProperty(name) && index > 0)
        {
            throw new IllegalArgumentException("Property '" + name
                    + "' is not indexed.");
        }

        Object property = getConfiguration().getProperty(name);

        if (property instanceof List)
        {
            List list = (List) property;
            list.set(index, value);
            getConfiguration().setProperty(name, list);
        }
        else if (property.getClass().isArray())
        {
            Array.set(property, index, value);
        }
        else if (index == 0)
        {
            getConfiguration().setProperty(name, value);
        }
    }

    public void set(String name, String key, Object value)
    {
        getConfiguration().setProperty(name + "." + key, value);
    }

    /**
     * Tests whether the given name references an indexed property. This
     * implementation tests for properties of type list or array. If the
     * property does not exist, an exception is thrown.
     *
     * @param name the name of the property to check
     * @return a flag whether this is an indexed property
     * @throws IllegalArgumentException if the property does not exist
     */
    private boolean checkIndexedProperty(String name)
    {
        Object property = getConfiguration().getProperty(name);

        if (property == null)
        {
            throw new IllegalArgumentException("Property '" + name
                    + "' does not exist.");
        }

        return (property instanceof List) || property.getClass().isArray();
    }
}
