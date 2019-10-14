/*
 * Copyright 2015-2018 Real Logic Ltd, Adaptive Financial Consulting Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.artio.system_tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.co.real_logic.artio.Reply;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;
import static uk.co.real_logic.artio.TestFixtures.launchMediaDriver;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.ACCEPTOR_ID;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.INITIATOR_ID;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.PASSWORD;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.USERNAME;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.initiatingConfig;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.newInitiatingLibrary;

public class ConnectionTimeoutOnFailedAcceptorSystemTest extends AbstractGatewayToGatewaySystemTest
{
    private final ServerSocket serverSocket;

    public ConnectionTimeoutOnFailedAcceptorSystemTest() throws IOException
    {
        serverSocket = new ServerSocket(port);
        final Thread serverThread = new Thread(() ->
        {
            try
            {
                while (true)
                {
                    serverSocket.accept();
                }
            }
            catch (final Exception e)
            {
                // Deliberately blank as closing the socket will cause an exception to be thrown.
            }
        });
        serverThread.start();
    }

    @Before
    public void launch()
    {
        deleteLogs();
        mediaDriver = launchMediaDriver();

        final EngineConfiguration initiatingConfig = initiatingConfig(libraryAeronPort);
        initiatingEngine = FixEngine.launch(initiatingConfig);
        initiatingLibrary = newInitiatingLibrary(libraryAeronPort, initiatingHandler);
        testSystem = new TestSystem(initiatingLibrary);
    }

    @After
    public void stopServerThread() throws IOException
    {
        serverSocket.close();
    }

    @Test(timeout = 1000)
    public void shouldTimeoutWhenConnectingToUnresponsiveEngine()
    {
        final Reply<Session> secondConnectReply = initiateSession();
        assertEquals(Reply.State.EXECUTING, secondConnectReply.state());
        testSystem.awaitReply(secondConnectReply);
        assertEquals(secondConnectReply.toString(), Reply.State.TIMED_OUT, secondConnectReply.state());
    }

    private Reply<Session> initiateSession()
    {
        final SessionConfiguration config = SessionConfiguration.builder()
            .address("localhost", port)
            .credentials(USERNAME, PASSWORD)
            .senderCompId(INITIATOR_ID)
            .targetCompId(ACCEPTOR_ID)
            .timeoutInMs(200)
            .build();

        return initiatingLibrary.initiate(config);
    }
}