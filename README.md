# Introduction
Plugin which performs Eclipselink static weaving. Use the weave goal to execute. 

Heavily inspired by https://code.google.com/p/eclipselink-staticweave-maven-plugin. 
This is an updated and enhanced version to be compatible with Java 7 + 8, Maven 3.x and EclipseLink 2.5.1. 

Internally the StaticWeaveProcessor is used, like described in the EclipseLink Wiki https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Advanced_JPA_Development/Performance/Weaving/Static_Weaving#Use_the_Command_Line. 

Do not forget to add EclipseLink as dependency, otherwise the EclipseLink StaticWeaveProcessor is not found. 

# Common Usage
```xml
 <build>
   	...
   <plugins>
 			 <plugin>
 			 	<groupId>de.empulse.eclipselink</groupId>
 				<artifactId>staticweave-maven-plugin</artifactId>
 				<version>1.0.0</version>
 				<executions>
 					<execution>
 						<goals>
 							<goal>weave</goal>
 						</goals>
 						<configuration>
 							<persistenceXMLLocation>META-INF/persistence.xml</persistenceXMLLocation>
 							<logLevel>FINE</logLevel>
 						</configuration>
 					</execution>
 				</executions>
 				<dependencies>
 					<dependency>
 						<groupId>org.eclipse.persistence</groupId>
 						<artifactId>org.eclipse.persistence.jpa</artifactId>
 						<version>${eclipselink.version}</version>
 					</dependency>
 				</dependencies>
 			</plugin>
   		
   		...
   	</plugins>
   	...
 </build>
```
# Plugin Options

## persistenceXMLLocation
Give here the location of your persistence.xml file. This property is optional. If not set the default location META-INF/persistence.xml is used.

## source
The location of the JPA classes. This property is optional, default value is ${project.build.outputDirectory}.

## target
The location for the weaved classes. This property is optional, default value is ${project.build.outputDirectory}.

## logLevel
The Logging level of the used EclipseLink StaticWeave class. This property is optional, default value is FINE to get informed which JPA classes were woven.
	 
Possible values:
	 
* OFF
* SEVERE
* WARNING
* INFO
* CONFIG
* FINE
* FINER
* FINEST
* ALL

The EclipseLink logging information is always given in Maven INFO loglevel.

Happy weaving!
