package io.stormbird.wallet.service;

import android.util.Log;
import io.stormbird.wallet.entity.AWEvent;
import io.stormbird.wallet.entity.FragmentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by James on 9/03/2019.
 * Stormbird in Singapore
 */
public class EventService
{
    private final Map<Integer, List<AWEvent>> eventMap = new ConcurrentHashMap<>();
    private final Map<FragmentType, ConcurrentLinkedQueue<AWEvent>> queueMap = new HashMap<>();
    private int eventCounter;

    public EventService()
    {
        //create fragment queues.
        //Why not put each one in the fragment itself? The fragment may not have been created yet.
        //So - either set up an elaborate interlock mechanism or simply keep track of fragment queues in a service
        //which is guaranteed to be present by the architecture, and pull events from it.

        queueMap.put(FragmentType.WALLET_FRAGMENT, new ConcurrentLinkedQueue<>());
        queueMap.put(FragmentType.TRANSACTIONS_FRAGMENT, new ConcurrentLinkedQueue<>());
        eventCounter = 0;
    }

    public void checkEvents()
    {
        List<AWEvent> currentEvents = eventMap.get(eventCounter);
        if (currentEvents != null)
        {
            eventMap.remove(eventCounter);
            //filter into respective fragment queue
            for (AWEvent event : currentEvents)
            {
                switch (event.eventType)
                {
                    case UPDATE_TOKEN_BALANCE:
                        queueMap.get(FragmentType.WALLET_FRAGMENT).add(event);
                        break;
                    case CHECK_TOKEN_TRANSACTIONS:
                        queueMap.get(FragmentType.TRANSACTIONS_FRAGMENT).add(event);
                        break;
                    default:
                        break;
                }
            }
        }
        eventCounter++;
    }

    public void addEvent(int timeDuration, AWEvent event)
    {
        if (timeDuration < 2)
        {
            Log.d("EventService", "Bad event time: " + timeDuration);
            timeDuration = 2;
        }

        List<AWEvent> futureEvents = eventMap.get(eventCounter + timeDuration);
        if (futureEvents == null)
        {
            futureEvents = new ArrayList<>();
            eventMap.put(eventCounter + timeDuration, futureEvents);
        }
        futureEvents.add(event);
    }

    public AWEvent getNextEvent(FragmentType fragmentType)
    {
        AWEvent pendingEvent = null;
        ConcurrentLinkedQueue<AWEvent> fragmentQueue = queueMap.get(fragmentType);
        if (fragmentQueue != null && !fragmentQueue.isEmpty())
        {
            pendingEvent = fragmentQueue.poll();
        }

        return pendingEvent;
    }
}
