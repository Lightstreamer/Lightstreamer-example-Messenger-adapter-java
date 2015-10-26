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

package messenger_demo.adapters;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.FailureException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.SmartDataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;

public class IMDataAdapter implements SmartDataProvider {

    private static String LIST = "buddy_list";
    private static String USER_PREFIX = "im_";
    private static String ME = "me";
    private static String NOBODY = "nobody";

    /**
     * A static map, to be used by the Metadata Adapter to find the data
     * adapter instance; this allows the Metadata Adapter to forward client
     * messages to the adapter.
     * The map allows multiple instances of this Data Adapter to be included
     * in different Adapter Sets. Each instance is identified with the name
     * of the related Adapter Set; defining multiple instances in the same
     * Adapter Set is not allowed.
     */
    public static final ConcurrentHashMap<String, IMDataAdapter> feedMap =
        new ConcurrentHashMap<String, IMDataAdapter>();

    /**
     * Private logger; a specific "LS_demos_Logger.Messenger" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;

    /**
     * A map containing all active subscriptions to the nicknames;
     * it associates each item name with the item handle to be used
     * to identify the item towards Lightstreamer Kernel.
     * The map also indicates the current set of active users.
     */
    private final ConcurrentHashMap<String, Object> subscriptions =
        new ConcurrentHashMap<String, Object>();

    /**
     * Handle to identify the LIST item, as received by Lightstreamer.
     */
    private Object listHandle;

    /**
     * Mutex to tighten subscription list manipulations and updates
     * for the LIST items.
     */
    private Object listMutex = new Object();

    /**
     * The listener of updates set by Lightstreamer Kernel.
     */
    private ItemEventListener listener;

    /**
     * Used to enqueue the calls to the listener.
     */
    private final ExecutorService executor;

    public IMDataAdapter() {
        executor = Executors.newSingleThreadExecutor();
    }

    public void init(Map params, File configDir) throws DataProviderException {
        
        // Logging configuration for the demo is carried out in the init
        // method of Metadata Adapter. In order to be sure that this method 
        // is executed after log configuration was completed, this parameter 
        // must be present in the Adapter Set configuration (adapters.xml):
        // <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>
        logger = Logger.getLogger("LS_demos_Logger.Messenger");

        // Read the Adapter Set name, which is supplied by the Server as a parameter
        String adapterSetId = (String) params.get("adapters_conf.id");

        // Put a reference to this instance on a static map
        // to be read by the Metadata Adapter
        feedMap.put(adapterSetId, this);

        // Adapter ready
        logger.info("IMDataAdapter ready");

    }


    public void subscribe(String item, Object handle, boolean needsIterator)
            throws SubscriptionException, FailureException {

        if (item.equals(LIST)) {
            // returns the list of the users currently available,

            synchronized (listMutex) {
                assert(listHandle == null);

                listHandle = handle;

                Enumeration<String> keys = subscriptions.keys();
                while(keys.hasMoreElements()) {
                    String user = keys.nextElement();
                    this.updateList("ADD", user, true);
                }
                sendListEOS();
            }


        } else if (item.startsWith(USER_PREFIX)) {
            String user = item.substring(USER_PREFIX.length());

            assert(! subscriptions.containsKey(user));

            synchronized (listMutex) {
                // Add the new item to the list of subscribed items
                subscriptions.put(user, handle);

                this.updateList("ADD", user, false);
            }

        } else {
            throw new SubscriptionException("Unexpected item name: " + item);
        }

        logger.info(item + " subscribed");
    }

    public void unsubscribe(String item) throws SubscriptionException,
            FailureException {

        if (item.equals(LIST)) {
            synchronized (listMutex) {
                assert(listHandle != null);
                listHandle = null;
            }

        } else if (item.startsWith(USER_PREFIX)) {
            String user = item.substring(USER_PREFIX.length());

            assert(subscriptions.containsKey(user));

            synchronized (listMutex) {
                subscriptions.remove(user);
                this.updateList("DELETE", user, false);
            }

        } else {
            throw new SubscriptionException("Unexpected item name: " + item);
        }

    }


    public boolean isSnapshotAvailable(String item)
            throws SubscriptionException {
        if (item.equals(LIST)) {
            return true;
        }
        return false;
    }

    public void setListener(ItemEventListener listener) {
        this.listener = listener;
    }

    /**
     * Accepts message submission with a specified sender and target user
     * nicknames.
     */
    public void sendMessage(String from, String to, String message) {
        if (from.equals(to)) {
            Object handle = subscriptions.get(from);
            if (handle != null) {
                this.sendMessage(ME,ME,message,handle);
            }
        } else {
            Object toHandle = subscriptions.get(to);
            Object fromHandle = subscriptions.get(from);

            if (toHandle == null) {
                if (fromHandle != null) {
                    this.sendMessage(ME,NOBODY,message,fromHandle);
                }
            } else {
                this.sendMessage(from,ME,message,toHandle);
                if (fromHandle != null) {
                    this.sendMessage(ME,to,message,fromHandle);
                }
            }
        }

    }

    private void sendMessage(String from, String to, String message, Object handle) {
        final Object currHandle = handle;
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
                listener.smartUpdate(currHandle, update, false);
            }
        };

        //We add the task on the executor to pass to the listener the actual status
        executor.execute(updateTask);

    }

    private void updateList(String command, String key, final boolean isForSnapshot) {
        assert(command.equals("ADD") || ! isForSnapshot);
        final Object currHandle = listHandle;
        if (currHandle == null) {
            return;
        }

        final HashMap<String, String> update = new HashMap<String, String>();
        update.put("command", command);
        update.put("key", key);

        //If we have a listener create a new Runnable to be used as a task to pass the
        //new update to the listener
        Runnable updateTask = new Runnable() {
            public void run() {
                // call the update on the listener;
                // in case the listener has just been detached,
                // the listener should detect the case
                listener.smartUpdate(currHandle, update, isForSnapshot);
            }
        };

        //We add the task on the executor to pass to the listener the actual status
        executor.execute(updateTask);

    }

    private void sendListEOS() {
        final Object currHandle = listHandle;
        if (currHandle == null) {
            return;
        }

        //If we have a listener create a new Runnable to be used as a task to pass the
        //new update to the listener
        Runnable eosTask = new Runnable() {
            public void run() {
                // call the update on the listener;
                // in case the listener has just been detached,
                // the listener should detect the case
                listener.smartEndOfSnapshot(currHandle);
            }
        };

        //We add the task on the executor to pass to the listener the actual status
        executor.execute(eosTask);

    }


    public void subscribe(String arg0, boolean arg1)
            throws SubscriptionException, FailureException {
        //NEVER CALLED

    }

}