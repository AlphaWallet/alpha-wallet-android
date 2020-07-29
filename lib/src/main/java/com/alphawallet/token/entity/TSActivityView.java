package com.alphawallet.token.entity;

/**
 * Created by JB on 27/07/2020.
 */
public class TSActivityView
{
    private final TSOrigins eventOrigins;
    //views
    private TSTokenViewHolder tokenViews = new TSTokenViewHolder();

    public TSActivityView(TSOrigins origins)
    {
        eventOrigins = origins;
    }

    public void addView(String viewName, TSTokenView view)
    {
        tokenViews.views.put(viewName, view);
    }

    /**
     * Gets the corresponding view
     * @param viewType either 'item-view' or 'view'
     * @return
     */
    public TSTokenView getView(String viewType)
    {
        return tokenViews.views.get(viewType);
    }

    public String getEventName()
    {
        return eventOrigins.getOriginName();
    }

    public String getActivityFilter()
    {
        return eventOrigins.getOriginEvent().filter;
    }
}
