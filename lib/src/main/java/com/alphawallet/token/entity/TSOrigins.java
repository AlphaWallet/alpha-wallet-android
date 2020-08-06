package com.alphawallet.token.entity;

import org.xml.sax.SAXException;

/**
 * Created by JB on 27/07/2020.
 */
public class TSOrigins
{
    private TSOriginType type;
    private String originName;
    private EventDefinition event;

    public static class Builder
    {
        private TSOriginType type;
        private String originName;
        private EventDefinition ev;

        public Builder(TSOriginType type)
        {
            this.type = type;
            this.ev = null;
        }

        public Builder name(String name)
        {
            this.originName = name;
            return this;
        }

        public Builder event(EventDefinition event)
        {
            this.ev = event;
            return this;
        }

        public TSOrigins build() throws SAXException
        {
            TSOrigins origins = new TSOrigins();
            origins.type = this.type;
            if (originName == null) throw new SAXException("Origins must have contract or type field.");
            origins.originName = this.originName;
            if (type == TSOriginType.Event && ev == null)
            {
                throw new SAXException("Event origin must have Filter spec.");
            }

            origins.event = this.ev;

            return origins;
        }
    }

    private TSOrigins()
    {

    }

    public String getOriginName()
    {
        return originName;
    }

    public EventDefinition getOriginEvent()
    {
        return event;
    }

    public boolean isType(TSOriginType checkType)
    {
        return type == checkType;
    }
}
