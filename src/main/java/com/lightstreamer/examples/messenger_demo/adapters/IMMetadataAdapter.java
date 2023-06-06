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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapters.metadata.LiteralBasedProvider;
import com.lightstreamer.interfaces.metadata.CreditsException;
import com.lightstreamer.interfaces.metadata.ItemsException;
import com.lightstreamer.interfaces.metadata.MetadataProviderException;
import com.lightstreamer.interfaces.metadata.NotificationException;


public class IMMetadataAdapter extends LiteralBasedProvider {

    /**
     * Prefix for client-specific item names provided by the Messenger
     * Data Adapter.
     */
    private static String NICK_PREFIX = "im_";

    /**
     * The associated feed to which messages will be forwarded;
     * it is the Data Adapter itself.
     */
    private volatile IMDataAdapter IMFeed;

    /**
     * Unique identification of the related Messenger Data Adapter instance;
     * see feedMap on the IMDataAdapter.
     */
    private String adapterSetId;

    /**
     * Private logger; a specific "LS_demos_Logger.Messenger" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;

    public IMMetadataAdapter() {
    }

    public void init(Map params, File configDir) throws MetadataProviderException {
        //Call super's init method to handle basic Metadata Adapter features
        super.init(params,configDir);

        logger = LogManager.getLogger("LS_demos_Logger.Messenger");

        // Read the Adapter Set name, which is supplied by the Server as a parameter
        this.adapterSetId = (String) params.get("adapters_conf.id");

        /*
         * Note: the IMDataAdapter instance cannot be looked for
         * here to initialize the "IMFeed" variable, because the Messenger
         * Data Adapter may not be loaded and initialized at this moment.
         * We need to wait until the first "sendMessage" occurrence;
         * then we can store the reference for later use.
         */

        logger.info("MessengerMetadataAdapter ready");
    }

    public String[] getItems(String user, String session, String id, String dataAdapter) throws ItemsException {
        String[] broken = super.getItems(user, session, id, dataAdapter);

        for (int i=0; i < broken.length; i++) {
            checkIMName(broken[i], user);
        }
        return broken;
    }

    /**
     * Triggered by a client "sendMessage" call.
     * The message encodes an instant message from the client.
     */
    public CompletableFuture<String> notifyUserMessage(String user, String session, String message)
        throws NotificationException, CreditsException {

        // we won't introduce blocking operations, hence we can proceed inline

        if (message == null) {
            logger.warn("Null message received");
            throw new NotificationException("Null message received");
        }

        //Split the string on the | character
        //The message must be of the form "IM|fromId|toId|message"
        String[] pieces = message.split("\\|", -1);

        this.loadIMFeed();
        this.handleIMMessage(pieces, message, user);

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Checks the item names used for user-related requests
     * to the Messenger Data Adapter.
     */
    private void checkIMName(String name, String user) throws ItemsException {
        if (name.startsWith(NICK_PREFIX)) {
            String nick = name.substring(NICK_PREFIX.length());

            // here we should check that the <user> is enabled
            // to subscribe to the messages pertaining to the <nick>
        }
    }

    private void loadIMFeed() throws CreditsException {
        if (this.IMFeed == null) {
            try {
                // Get the IMDataAdapter instance to bind it with this
                // Metadata Adapter and send instant messages through it
                this.IMFeed = IMDataAdapter.feedMap.get(this.adapterSetId);
            } catch (Throwable t) {
                // It can happen if the Messenger Data Adapter jar was not even
                // included in the Adapter Set lib directory (the Messenger
                // Data Adapter could not be included in the Adapter Set as
                // well)
                logger.error("IMDataAdapter class was not loaded: " + t);
                throw new CreditsException(0, "No IM feed available",
                        "No IM feed available");
            }

            if (this.IMFeed == null) {
                // The feed is not yet available on the static map, maybe the
                // Messenger Data Adapter was not included in the Adapter Set
                logger.error("IMDataAdapter not found");
                throw new CreditsException(0, "No IM feed available",
                        "No IM feed available");
            }
        }
    }

    private void handleIMMessage(String[] pieces, String fullMessage, String user)
            throws CreditsException {

        // Check the message, it must be of the form "IM|fromId|toId|message"
        if (pieces[0].equals("IM")) {
            if (pieces.length < 4) {
                logger.warn("Wrong message received: " + fullMessage);
                throw new CreditsException(1, "Wrong message received", "Wrong message");
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
            this.IMFeed.sendMessage(from, to, message);
        }
    }

}