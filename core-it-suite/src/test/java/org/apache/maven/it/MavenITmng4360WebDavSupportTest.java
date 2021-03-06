package org.apache.maven.it;

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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4360">MNG-4360</a>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class MavenITmng4360WebDavSupportTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4360WebDavSupportTest()
    {
        super( "[2.1.0-M1,)" );
    }

    /**
     * Verify that WebDAV works in principle. This test is not actually concerned about proper transfers but more 
     * that the Jackrabbit based wagon can be properly loaded and doesn't die due to some class realm issue.
     */
    public void testitJackrabbitBasedImpl()
        throws Exception
    {
        test( "jackrabbit" );
    }

    /**
     * Verify that WebDAV works in principle. This test is not actually concerned about proper transfers but more 
     * that the Slide based wagon can be properly loaded and doesn't die due to some class realm issue.
     */
    public void testitSlideBasedImpl()
        throws Exception
    {
        test( "slide" );
    }

    private void test( String project )
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4360" );

        testDir = new File( testDir, project );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                PrintWriter writer = response.getWriter();

                response.setStatus( HttpServletResponse.SC_OK );

                if ( request.getRequestURI().endsWith( ".pom" ) )
                {
                    writer.println( "<project>" );
                    writer.println( "  <modelVersion>4.0.0</modelVersion>" );
                    writer.println( "  <groupId>org.apache.maven.its.mng4360</groupId>" );
                    writer.println( "  <artifactId>dep</artifactId>" );
                    writer.println( "  <version>0.1</version>" );
                    writer.println( "</project>" );
                }
                else if ( request.getRequestURI().endsWith( ".jar" ) )
                {
                    writer.println( "empty" );
                }
                else if ( request.getRequestURI().endsWith( ".md5" ) || request.getRequestURI().endsWith( ".sha1" ) )
                {
                    response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                }

                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );
        server.start();

        try
        {
            int port = server.getConnectors()[0].getLocalPort();

            verifier.setAutoclean( false );
            verifier.deleteArtifacts( "org.apache.maven.its.mng4360" );
            verifier.deleteDirectory( "target" );
            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.setProperty( "@port@", Integer.toString( port ) );
            verifier.filterFile( "../settings-template.xml", "settings.xml", "UTF-8", filterProps );
            verifier.addCliOption( "--settings" );
            verifier.addCliOption( "settings.xml" );
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }
        finally
        {
            server.stop();
        }

        List<String> cp = verifier.loadLines( "target/classpath.txt", "UTF-8" );
        assertTrue( cp.toString(), cp.contains( "dep-0.1.jar" ) );
    }

}
