package org.apache.maven.reporting.exec;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * 
 */
@Component( role = MavenPluginManagerHelper.class )
public class DefaultMavenPluginManagerHelper
    implements MavenPluginManagerHelper
{
    @Requirement
    private Logger logger;

    @Requirement
    protected MavenPluginManager mavenPluginManager;

    private Boolean isEclipseAether;

    private Method setupPluginRealm;

    private Method getPluginDescriptor;

    private Method getRepositorySession;

    public DefaultMavenPluginManagerHelper()
    {
        try
        {
            for ( Method m : MavenPluginManager.class.getMethods() )
            {
                if ( "setupPluginRealm".equals( m.getName() ) )
                {
                    setupPluginRealm = m;
                }
                else if ( "getPluginDescriptor".equals( m.getName() ) )
                {
                    getPluginDescriptor = m;
                }
            }
        }
        catch ( SecurityException e )
        {
            logger.warn( "unable to find MavenPluginManager.setupPluginRealm() method", e );
        }

        try
        {
            for ( Method m : MavenSession.class.getMethods() )
            {
                if ( "getRepositorySession".equals( m.getName() ) )
                {
                    getRepositorySession = m;
                    break;
                }
            }
        }
        catch ( SecurityException e )
        {
            logger.warn( "unable to find MavenSession.getRepositorySession() method", e );
        }
    }

    private boolean isEclipseAether()
    {
        if ( isEclipseAether == null )
        {
            try
            {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                cl.loadClass( "org.sonatype.aether.graph.DependencyFilter" );
                isEclipseAether = false;
            }
            catch ( ClassNotFoundException e )
            {
                isEclipseAether = true;
            }
        }

        return isEclipseAether.booleanValue();
    }

    private Object createExclusionsDependencyFilter( List<String> artifactIdsList )
    {
        if ( isEclipseAether() )
        {
            return new org.eclipse.aether.util.filter.ExclusionsDependencyFilter( artifactIdsList );
        }
        else
        {
            return new org.sonatype.aether.util.filter.ExclusionsDependencyFilter( artifactIdsList );
        }
    }

    public PluginDescriptor getPluginDescriptor( Plugin plugin, MavenSession session )
        throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException
    {
        try
        {
            Object repositorySession = getRepositorySession.invoke( session );
            List<?> remoteRepositories = session.getCurrentProject().getRemotePluginRepositories();

            return (PluginDescriptor) getPluginDescriptor.invoke( mavenPluginManager, plugin, remoteRepositories,
                                                                  repositorySession );
        }
        catch ( IllegalArgumentException e )
        {
            logger.warn( "IllegalArgumentException during MavenPluginManager.getPluginDescriptor() call", e );
        }
        catch ( IllegalAccessException e )
        {
            logger.warn( "IllegalAccessException during MavenPluginManager.getPluginDescriptor() call", e );
        }
        catch ( InvocationTargetException e )
        {
            Throwable target = e.getTargetException();
            if ( target instanceof PluginResolutionException )
            {
                throw (PluginResolutionException) target;
            }
            if ( target instanceof PluginDescriptorParsingException )
            {
                throw (PluginDescriptorParsingException) target;
            }
            if ( target instanceof InvalidPluginDescriptorException )
            {
                throw (InvalidPluginDescriptorException) target;
            }
            if ( target instanceof RuntimeException )
            {
                throw (RuntimeException) target;
            }
            if ( target instanceof Error )
            {
                throw (Error) target;
            }
            logger.warn( "Exception during MavenPluginManager.getPluginDescriptor() call", e );
        }

        return null;
    }

    public void setupPluginRealm( PluginDescriptor pluginDescriptor, MavenSession session, ClassLoader parent,
                                  List<String> imports, List<String> excludeArtifactIds )
        throws PluginResolutionException, PluginContainerException
    {
        try
        {
            setupPluginRealm.invoke( mavenPluginManager, pluginDescriptor, session, parent, imports,
                                     createExclusionsDependencyFilter( excludeArtifactIds ) );
        }
        catch ( IllegalArgumentException e )
        {
            logger.warn( "IllegalArgumentException during MavenPluginManager.setupPluginRealm() call", e );
        }
        catch ( IllegalAccessException e )
        {
            logger.warn( "IllegalAccessException during MavenPluginManager.setupPluginRealm() call", e );
        }
        catch ( InvocationTargetException e )
        {
            Throwable target = e.getTargetException();
            if ( target instanceof PluginResolutionException )
            {
                throw (PluginResolutionException) target;
            }
            if ( target instanceof PluginContainerException )
            {
                throw (PluginContainerException) target;
            }
            if ( target instanceof RuntimeException )
            {
                throw (RuntimeException) target;
            }
            if ( target instanceof Error )
            {
                throw (Error) target;
            }
            logger.warn( "Exception during MavenPluginManager.setupPluginRealm() call", e );
        }
    }
}
