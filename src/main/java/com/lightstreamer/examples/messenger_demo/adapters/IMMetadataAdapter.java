/*
  Copyright (c) Lightstreamer Srl

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.lightstreamer.examples.messenger_demo.adapters;


import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.metadata.CreditsException;
import com.lightstreamer.adapter.metadata.ItemsException;
import com.lightstreamer.adapter.metadata.LoginInfo;
import com.lightstreamer.adapter.metadata.MessageException;
import com.lightstreamer.adapter.metadata.MetadataAdapter;
import com.lightstreamer.adapter.metadata.SessionRequestHandler;
import com.lightstreamer.adapter.metadata.SubscriptionHandler;
import com.lightstreamer.adapter.metadata.SubscriptionInfo;


public class IMMetadataAdapter implements MetadataAdapter {

    /**
     * Prefix for client-specific item names provided by the Messenger
     * Data Adapter.
     */
    private static final String NICK_PREFIX = "im_";

    /**
     * The associated feed to which messages will be forwarded;
     * it is the Data Adapter itself.
     */
    private volatile IMDataAdapter IMFeed;

    /**
     * The name of the currently bound Data Adapter, used to match
     * onDataAdapterUnavailable with onDataAdapterAvailable.
     */
    private volatile String currentDataAdapterName;

    /**
     * Private logger; a specific "LS_demos_Logger.Messenger" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;

    public IMMetadataAdapter() {
    }

    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        logger = LogManager.getLogger("LS_demos_Logger.Messenger");

        logger.info("MessengerMetadataAdapter ready");
    }

    @Override
    public void onDataAdapterAvailable(String dataAdapterName, Object dataAdapter) {
        if (!(dataAdapter instanceof IMDataAdapter)) {
            return;
        }
        if (this.IMFeed != null) {
            logger.warn("A second IMDataAdapter instance is available; ignoring it");
            return;
        }
        this.IMFeed = (IMDataAdapter) dataAdapter;
        this.currentDataAdapterName = dataAdapterName;
    }

    @Override
    public void onDataAdapterUnavailable(String dataAdapterName) {
        if (dataAdapterName.equals(this.currentDataAdapterName)) {
            this.IMFeed = null;
            this.currentDataAdapterName = null;
        }
    }

    @Override
    public CompletionStage<SessionRequestHandler> loginAsync(LoginInfo loginInfo) {
        //////////////////////////////////////////////////////////////////////////
        // WARNING: No authentication logic is implemented; any login is accepted.
        // This is suitable for demos only and MUST NOT be used in production.
        //////////////////////////////////////////////////////////////////////////
        String user = loginInfo.getUser();

        return CompletableFuture.completedFuture(new SessionRequestHandler() {

            @Override
            public CompletionStage<SubscriptionHandler> newSubscription(SubscriptionInfo info)
                    throws ItemsException {
                // Check the item names used for user-related requests
                // to the Messenger Data Adapter.
                for (String name : info.getItemRefs()) {
                    if (name.startsWith(NICK_PREFIX)) {
                        String nick = name.substring(NICK_PREFIX.length());

                        // here we should check that the <user> is enabled
                        // to subscribe to the messages pertaining to the <nick>
                    }
                }
                return CompletableFuture.completedFuture(new SubscriptionHandler(info));
            }

            @Override
            public CompletionStage<String> userMessageAsync(String message)
                    throws CreditsException, MessageException {
                // Triggered by a client "sendMessage" call.
                // The message encodes an instant message from the client.

                // we won't introduce blocking operations, hence we can proceed inline

                if (message == null) {
                    logger.warn("Null message received");
                    throw new MessageException("Null message received");
                }

                //Split the string on the | character
                //The message must be of the form "IM|fromId|toId|message"
                String[] pieces = message.split("\\|", -1);

                if (IMFeed == null) {
                    // The Messenger Data Adapter is not yet available,
                    // or was not included in the Adapter Set
                    logger.error("IMDataAdapter not available");
                    throw new CreditsException("No IM feed available", 0,
                            "No IM feed available");
                }

                handleIMMessage(pieces, message, user);

                return CompletableFuture.completedFuture(null);
            }

            private void handleIMMessage(String[] pieces, String fullMessage, String user)
                    throws CreditsException {

                // Check the message, it must be of the form "IM|fromId|toId|message"
                if (pieces[0].equals("IM")) {
                    if (pieces.length < 4) {
                        logger.warn("Wrong message received: " + fullMessage);
                        throw new CreditsException("Wrong message received", 1, "Wrong message");
                    }
                    String from = pieces[1];
                    String to = pieces[2];

                    // here we should check that the <user> is enabled to send
                    // messages on behalf of the <from> nickname

                    String message = pieces[3];
                    if (pieces.length > 4) {
                        // Ooops! There are '|' characters in the message
                        // (the client should prevent '|' characters in the nicknames)
                        for (int i = 4; i < pieces.length; i++) {
                            message += "|";
                            message += pieces[i];
                        }
                    }
                    IMFeed.sendMessage(from, to, message);
                }
            }
        });
    }

    @Override
    public void shutdown() {
    }

}