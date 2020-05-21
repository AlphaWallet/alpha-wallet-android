package com.alphawallet.app.web3j.datatypes;

import com.alphawallet.app.web3j.TypeReference;

import org.web3j.abi.datatypes.Type;

import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.web3j.Utils.convert;

public class Event {
    private String name;
    private List<TypeReference<Type>> parameters;

    public Event(String name, List<TypeReference<?>> parameters) {
        this.name = name;
        this.parameters = convert(parameters);
    }

    public String getName() {
        return name;
    }

    public List<TypeReference<Type>> getParameters() {
        return parameters;
    }

    public List<TypeReference<Type>> getIndexedParameters() {
        final List<TypeReference<Type>> result = new ArrayList<>(parameters.size());
        for (TypeReference<Type> parameter : parameters) {
            if (parameter.isIndexed()) {
                result.add(parameter);
            }
        }
        return result;
    }

    public List<TypeReference<Type>> getNonIndexedParameters() {
        final List<TypeReference<Type>> result = new ArrayList<>(parameters.size());
        for (TypeReference<Type> parameter : parameters) {
            if (!parameter.isIndexed()) {
                result.add(parameter);
            }
        }
        return result;
    }
}