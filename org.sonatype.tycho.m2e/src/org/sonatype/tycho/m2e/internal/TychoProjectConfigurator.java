/*******************************************************************************
 * Copyright (c) 2019 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sonatype.tycho.m2e.internal;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;

@SuppressWarnings( "restriction" )
public class TychoProjectConfigurator extends AbstractProjectConfigurator {

	private PDEProjectHelper pdeHelper = PDEProjectHelper.getInstance();
	
	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = request.getMavenProject();
        IProject project = request.getProject();

        String packaging = mavenProject.getPackaging();
        if ( "eclipse-plugin".equals( packaging ) || "eclipse-test-plugin".equals( packaging ) )
        {
            pdeHelper.configurePDEBundleProject( project, mavenProject, monitor );
        }
        else if ( "eclipse-feature".equals( packaging ) )
        {
            // see org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation
            if ( !project.hasNature( PDE.FEATURE_NATURE ) )
            {
                CoreUtility.addNatureToProject( project, PDE.FEATURE_NATURE, monitor );
            }
        }
        else if ( "eclipse-update-site".equals( packaging ) )
        {
            // see org.eclipse.pde.internal.ui.wizards.site.NewSiteProjectCreationOperation
            if ( !project.hasNature( PDE.SITE_NATURE ) )
            {
                CoreUtility.addNatureToProject( project, PDE.SITE_NATURE, monitor );
            }
        }
	}

}
