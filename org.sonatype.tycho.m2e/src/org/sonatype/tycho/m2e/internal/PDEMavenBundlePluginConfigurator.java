/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sonatype.tycho.m2e.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import static org.sonatype.tycho.m2e.felix.internal.MavenBundlePluginConfigurator.PARAM_MANIFESTLOCATION;

@SuppressWarnings( "restriction" )
public class PDEMavenBundlePluginConfigurator
    extends AbstractProjectConfigurator
    implements IJavaProjectConfigurator
{
    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        List<MojoExecution> executions = getMojoExecutions( request, monitor );

        if ( executions.size() > 1 )
        {
            List<MavenProblemInfo> errors = new ArrayList<MavenProblemInfo>();

            for ( MojoExecution mojoExecution : executions )
            {
                SourceLocation location = SourceLocationHelper.findLocation( mojoExecution.getPlugin(), "executions" );
                MavenProblemInfo problem =
                    new MavenProblemInfo( "Duplicate " + mojoExecution.getGoal()
                        + " executions found. Please remove any explicitly defined " + mojoExecution.getGoal()
                        + " executions in your pom.xml.", IMarker.SEVERITY_ERROR, location );
                errors.add( problem );
            }

            this.markerManager.addErrorMarkers( request.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, errors );
        }

        // bundle manifest is generated in #configureRawClasspath, which is invoked earlier during project configuration

        IProject project = request.getProject();
        IMavenProjectFacade facade = request.getMavenProjectFacade();

        IPath metainfPath = getMetainfPath( facade, executions, monitor );

        PDEProjectHelper.addPDENature( project, metainfPath, monitor );
    }

    @Override
    public void configureClasspath( IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor )
        throws CoreException
    {
    }

    @Override
    public void configureRawClasspath( ProjectConfigurationRequest request, IClasspathDescriptor classpath,
                                       IProgressMonitor monitor )
        throws CoreException
    {
        // export maven dependencies classpath container, so dependent project can compile against embedded transitive
        // dependencies. This breaks JDT classpath of plain maven dependents of this project, i.e. such dependents will
        // be exposed to more classes compared to CLI build. Not sure what to do about it yet.
        for ( IClasspathEntryDescriptor entry : classpath.getEntryDescriptors() )
        {
            if ( IClasspathManager.CONTAINER_ID.equals( entry.getPath().segment( 0 ) ) )
            {
                entry.setExported( true );
            }
        }
    }

    private IPath getMetainfPath( IMavenProjectFacade facade, List<MojoExecution> executions, IProgressMonitor monitor )
        throws CoreException
    {
        IMaven maven = MavenPlugin.getMaven();
        for ( MojoExecution execution : executions )
        {
            MavenProject mavenProject = facade.getMavenProject( monitor );
            File location =
                maven.getMojoParameterValue( mavenProject, execution, PARAM_MANIFESTLOCATION, File.class, monitor );
            if ( location != null )
            {
                return facade.getProjectRelativePath( location.getAbsolutePath() );
            }
        }

        return null;
    }

    @Override
    public boolean hasConfigurationChanged( IMavenProjectFacade newFacade,
                                            ILifecycleMappingConfiguration oldProjectConfiguration,
                                            MojoExecutionKey key, IProgressMonitor monitor )
    {
        return false;
    }
}
