package com.alphawallet.app.web3j.datatypes;

import com.alphawallet.app.web3j.TypeReference;

import org.web3j.abi.datatypes.Type;

import java.util.List;

import static com.alphawallet.app.web3j.Utils.convert;

public class Function {
    private String name;
    private List<Type> inputParameters;
    private List<TypeReference<Type>> outputParameters;

    public Function(String name, List<Type> inputParameters,
                    List<TypeReference<?>> outputParameters) {
        this.name = name;
        this.inputParameters = inputParameters;
        this.outputParameters = convert(outputParameters);
    }

    public String getName() {
        return name;
    }

    public List<Type> getInputParameters() {
        return inputParameters;
    }

    public List<TypeReference<Type>> getOutputParameters() {
        return outputParameters;
    }
}

