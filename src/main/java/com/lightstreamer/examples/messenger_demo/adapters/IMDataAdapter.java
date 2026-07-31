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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.common.Mode;
import com.lightstreamer.adapter.data.DataAdapter;
import com.lightstreamer.adapter.data.ItemListener;
import com.lightstreamer.adapter.data.ItemManager;
import com.lightstreamer.adapter.data.SubscriptionException;

public class IMDataAdapter implements DataAdapter {

    private static final String LIST = "buddy_list";
    private static final String USER_PREFIX = "im_";
    private static final String ME = "me";
    private static final String NOBODY = "nobody";

    /**
     * Private logger; a specific "LS_demos_Logger.Messenger" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;

    /**
     * A map containing all active subscriptions to the nicknames;
     * it associates each item name with the related UserItemManager.
     * The map also indicates the current set of active users.
     */
    private final ConcurrentHashMap<String, UserItemManager> subscriptions =
        new ConcurrentHashMap<String, UserItemManager>();

    /**
     * The ListItemManager for the LIST item.
     */
    private volatile ListItemManager listManager;

    /**
     * Mutex to tighten subscription list manipulations and updates
     * for the LIST items.
     */
    private final Object listMutex = new Object();

    /**
     * Used to enqueue the calls to the listener.
     */
    private final ExecutorService executor;

    public IMDataAdapter() {
        executor = Executors.newSingleThreadExecutor();
    }

    public void init(Map<String, String> params, File configDir) throws InitException {
        
        // Logging configuration for the demo is carried out in the init
        // method of Metadata Adapter. In order to be sure that this method 
        // is executed after log configuration was completed, this parameter 
        // must be present in the Adapter Set configuration (adapters.xml):
        // <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>
        logger = LogManager.getLogger("LS_demos_Logger.Messenger");

        // Adapter ready
        logger.info("IMDataAdapter ready");

    }

    @Override
    public CompletionStage<ItemManager> getItemManager(String itemName)
            throws SubscriptionException {

        if (itemName.equals(LIST)) {
            return CompletableFuture.completedFuture(new ListItemManager());
        } else if (itemName.startsWith(USER_PREFIX)) {
            return CompletableFuture.completedFuture(new UserItemManager(itemName));
        } else {
            CompletableFuture<ItemManager> f = new CompletableFuture<>();
            f.completeExceptionally(new SubscriptionException("Unexpected item name: " + itemName));
            return f;
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Accepts message submission with a specified sender and target user
     * nicknames.
     */
    public void sendMessage(String from, String to, String message) {
        if (from.equals(to)) {
            UserItemManager manager = subscriptions.get(from);
            if (manager != null) {
                manager.sendMessage(ME, ME, message);
            }
        } else {
            UserItemManager toManager = subscriptions.get(to);
            UserItemManager fromManager = subscriptions.get(from);

            if (toManager == null) {
                if (fromManager != null) {
                    fromManager.sendMessage(ME, NOBODY, message);
                }
            } else {
                toManager.sendMessage(from, ME, message);
                if (fromManager != null) {
                    fromManager.sendMessage(ME, to, message);
                }
            }
        }

    }

    private class ListItemManager implements ItemManager {
        private volatile ItemListener listener;

        @Override
        public Mode getSupportedMode() {
            return Mode.COMMAND;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) throws SubscriptionException {
            // returns the list of the users currently available,

            synchronized (listMutex) {
                assert(listManager == null);

                this.listener = listener;
                listManager = this;

                Enumeration<String> keys = subscriptions.keys();
                while(keys.hasMoreElements()) {
                    String user = keys.nextElement();
                    this.updateList(ItemListener.ADD_COMMAND, user, true);
                }
                sendListEOS();
            }

            logger.info(LIST + " subscribed");
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isSnapshotAvailable() {
            return true;
        }

        @Override
        public CompletionStage<Void> unsubscribe() throws SubscriptionException {
            synchronized (listMutex) {
                assert(listManager != null);
                listManager = null;
                this.listener = null;
            }
            return CompletableFuture.completedFuture(null);
        }

        private void sendListEOS() {
            final ItemListener currListener = this.listener;

            //If we have a listener create a new Runnable to be used as a task to pass the
            //new update to the listener
            Runnable eosTask = new Runnable() {
                public void run() {
                    // call the update on the listener;
                    // in case the listener has just been detached,
                    // the listener should detect the case
                    currListener.endOfSnapshot();
                }
            };

            //We add the task on the executor to pass to the listener the actual status
            executor.execute(eosTask);

        }

        private void updateList(String command, String key, final boolean isForSnapshot) {
            final ItemListener currListener = this.listener;

            final HashMap<String, String> update = new HashMap<String, String>();
            update.put(ItemListener.COMMAND_FIELD, command);
            update.put(ItemListener.KEY_FIELD, key);

            //If we have a listener create a new Runnable to be used as a task to pass the
            //new update to the listener
            Runnable updateTask = new Runnable() {
                public void run() {
                    // call the update on the listener;
                    // in case the listener has just been detached,
                    // the listener should detect the case
                    if (isForSnapshot) {
                        currListener.snapshot(update, null);
                    } else {
                        currListener.update(update);
                    }
                }
            };

            //We add the task on the executor to pass to the listener the actual status
            executor.execute(updateTask);

        }
    }

    private class UserItemManager implements ItemManager {
        private final String itemName;
        private volatile ItemListener listener;

        UserItemManager(String itemName) {
            this.itemName = itemName;
        }

        @Override
        public Mode getSupportedMode() {
            return Mode.DISTINCT;
        }

        @Override
        public Integer getDistinctSnapshotLength() {
            return 30;
        }

        @Override
        public CompletionStage<Void> subscribe(ItemListener listener) throws SubscriptionException {
            String user = itemName.substring(USER_PREFIX.length());

            assert(! subscriptions.containsKey(user));

            synchronized (listMutex) {
                // Add the new item to the list of subscribed items
                this.listener = listener;
                subscriptions.put(user, this);

                if (listManager != null) {
                    listManager.updateList(ItemListener.ADD_COMMAND, user, false);
                }
            }

            logger.info(itemName + " subscribed");
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isSnapshotAvailable() {
            return false;
        }

        @Override
        public CompletionStage<Void> unsubscribe() throws SubscriptionException {
            String user = itemName.substring(USER_PREFIX.length());

            assert(subscriptions.containsKey(user));

            synchronized (listMutex) {
                subscriptions.remove(user);
                this.listener = null;
                if (listManager != null) {
                    listManager.updateList(ItemListener.DELETE_COMMAND, user, false);
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        private void sendMessage(String from, String to, String message) {
            final ItemListener currListener = this.listener;
            final HashMap<String, String> update = new HashMap<String, String>();
            update.put("fromNick", from);
            update.put("toNick", to);
            update.put("message", message);

            //If we have a listener create a new Runnable to be used as a task to pass the
            //new update to the listener
            Runnable updateTask = new Runnable() {
                public void run() {
                    // call the update on the listener;
                    // in case the listener has just been detached,
                    // the listener should detect the case
                    currListener.update(update);
                }
            };

            //We add the task on the executor to pass to the listener the actual status
            executor.execute(updateTask);
        }
    }

}