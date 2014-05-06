package de.empulse.eclipselink.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.tools.weaving.jpa.StaticWeaveProcessor;

/**
 * Plugin which performs Eclipselink static weaving. Use the weave goal to
 * execute.
 * <p>
 * Internally the StaticWeaveProcessor is used, like described in <a href=
 * "https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Advanced_JPA_Development/Performance/Weaving/Static_Weaving#Use_the_Command_Line"
 * >the EclipseLink Wiki</a>.
 * </p>
 * <p>
 * Do not forget to add EclipseLink as dependency, otherwise the EclipseLink
 * StaticWeaveProcessor is not found.
 * </p>
 * 
 * <pre>
 * &lt;build&gt;
 *   	...
 *   &lt;plugins&gt;
 * 			 &lt;plugin&gt;
 * 				&lt;artifactId&gt;staticweave-maven-plugin&lt;/artifactId&gt;
 * 				&lt;groupId&gt;de.empulse.eclipselink&lt;/groupId&gt;
 * 				&lt;version&gt;1.0.0-SNAPSHOT&lt;/version&gt;
 * 				&lt;executions&gt;
 * 					&lt;execution&gt;
 * 						&lt;phase&gt;process-classes&lt;/phase&gt;
 * 						&lt;goals&gt;
 * 							&lt;goal&gt;weave&lt;/goal&gt;
 * 						&lt;/goals&gt;
 * 						&lt;configuration&gt;
 * 							&lt;persistenceXMLLocation&gt;META-INF/persistence.xml&lt;/persistenceXMLLocation&gt;
 * 							&lt;logLevel&gt;FINE&lt;/logLevel&gt;
 * 						&lt;/configuration&gt;
 * 					&lt;/execution&gt;
 * 				&lt;/executions&gt;
 * 				&lt;dependencies&gt;
 * 					&lt;dependency&gt;
 * 						&lt;groupId&gt;org.eclipse.persistence&lt;/groupId&gt;
 * 						&lt;artifactId&gt;org.eclipse.persistence.jpa&lt;/artifactId&gt;
 * 						&lt;version&gt;${eclipselink.version}&lt;/version&gt;
 * 					&lt;/dependency&gt;
 * 				&lt;/dependencies&gt;
 * 			&lt;/plugin&gt;
 *   		
 *   		...
 *   	&lt;/plugins&gt;
 *   	...
 * &lt;/build&gt;
 * 
 * </pre>
 * 
 * 
 * Heavily inspired by <a
 * href="https://code.google.com/p/eclipselink-staticweave-maven-plugin/"
 * >https://code.google.com/p/eclipselink-staticweave-maven-plugin</a>.
 * <p>
 * This is a updated version to be compatible with Java 7 + 8, Maven 3.x and
 * EclipseLink 2.5.1.
 * </p>
 * 
 * @author Christoph Guse
 */
@Mojo(name = "weave", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class EclipselinkStaticWeaveMojo extends AbstractMojo {

	/**
	 * Give here the location of your persistence.xml file. This property is
	 * optional. If not set the default location META-INF/persistence.xml is
	 * used.
	 * 
	 * <pre>
	 * </pre>
	 */
	@Parameter(property = "weave.persistenceXMLLocation")
	private String persistenceXMLLocation;

	/**
	 * The location of the JPA classes. This property is optional, default value
	 * is ${project.build.outputDirectory}.
	 */
	@Parameter(property = "project.build.outputDirectory")
	private String source;

	/**
	 * The location for the weaved classes. This property is optional, default
	 * value is ${project.build.outputDirectory}.
	 */
	@Parameter(property = "project.build.outputDirectory")
	private String target;

	/**
	 * The Logging level of the used EclipseLink StaticWeave class. This
	 * property is optional, default value is FINE to get informed which JPA
	 * classes were woven.
	 * <p>
	 * Possible values:
	 * </p>
	 * <ul>
	 * <li>OFF</li>
	 * <li>SEVERE</li>
	 * <li>WARNING</li>
	 * <li>INFO</li>
	 * <li>CONFIG</li>
	 * <li>FINE</li>
	 * <li>FINER</li>
	 * <li>FINEST</li>
	 * <li>ALL</li>
	 * </ul>
	 * The EclipseLink logging information is always given in Maven INFO
	 * loglevel.
	 */
	@Parameter(property = "weave.logLevel", defaultValue = SessionLog.FINE_LABEL)
	private String logLevel;

	/**
	 * The Maven project to have environment information like the classpath.
	 * This property is set by maven.
	 */
	@Component
	private MavenProject project;

	/**
	 * Execution method of the Mojo. The classpath is given to the EclipseLink
	 * StaticWeaveProcessor which does the weaving.
	 */
	public void execute() throws MojoExecutionException {

		try {

			getLog().info("Start EclipseLink static weaving...");

			List<URL> classpath = buildClassPath();

			StaticWeaveProcessor weave = new StaticWeaveProcessor(source,
					target);
			if (!classpath.isEmpty()) {
				URLClassLoader classLoader = new URLClassLoader(
						classpath.toArray(new URL[] {}), Thread.currentThread()
								.getContextClassLoader());
				weave.setClassLoader(classLoader);
			}
			if (persistenceXMLLocation != null) {
				weave.setPersistenceXMLLocation(persistenceXMLLocation);
			}
			weave.setLog(new LogWriter(getLog()));
			weave.setLogLevel(getLogLevel());
			weave.performWeaving();

			getLog().info("Finished EclipseLink static weaving.");

		} catch (MalformedURLException e) {
			throw new MojoExecutionException("Failed", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed", e);
		} catch (URISyntaxException e) {
			throw new MojoExecutionException("Failed", e);
		}
	}

	/**
	 * Setter for the loglevel used for the EclipseLink weaver.
	 * 
	 * @param logLevel
	 */
	public void setLogLevel(String logLevel) {
		if (SessionLog.OFF_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.SEVERE_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.WARNING_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.INFO_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.CONFIG_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.FINE_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.FINER_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.FINEST_LABEL.equalsIgnoreCase(logLevel)
				|| SessionLog.ALL_LABEL.equalsIgnoreCase(logLevel)) {
			this.logLevel = logLevel.toUpperCase();
		} else {
			getLog().error(
					"Unknown log level: " + logLevel
							+ " default LogLevel is used.");
		}
	}

	/**
	 * This helper method gets all URLs to jar files in the classpath.
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	private List<URL> buildClassPath() throws MalformedURLException {
		List<URL> urls = new ArrayList<URL>();

		if (project == null) {

			getLog().error(
					"MavenProject is empty, unable to build ClassPath. No Models can be woven.");

		} else {
			Set<Artifact> artifacts = (Set<Artifact>) project.getArtifacts();
			for (Artifact a : artifacts) {
				urls.add(a.getFile().toURI().toURL());
			}

		}

		return urls;
	}

	private int getLogLevel() {
		return AbstractSessionLog.translateStringToLoggingLevel(logLevel);
	}

}
