/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alphawallet.token.web.Ethereum.web3j;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.AbiTypes;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Pair;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.alphawallet.token.web.Ethereum.TokenscriptFunction.convertArgToBytes;
import static org.web3j.crypto.Hash.sha3;
import static org.web3j.crypto.Hash.sha3String;

/**
 * This class is here due to a bug in Web3j which prevented EIP712 working for some structures
 * The fix has been submitted to Web3j repo, when fixed this code can be removed. See also class StructuredData
 */

public class StructuredDataEncoder {
    public final StructuredData.EIP712Message jsonMessageObject;

    // Matches array declarations like arr[5][10], arr[][], arr[][34][], etc.
    // Doesn't match array declarations where there is a 0 in any dimension.
    // Eg- arr[0][5] is not matched.
    final String arrayTypeRegex = "^([a-zA-Z_$][a-zA-Z_$0-9]*)((\\[([1-9]\\d*)?\\])+)$";
    final Pattern arrayTypePattern = Pattern.compile(arrayTypeRegex);

    final String bytesTypeRegex = "^bytes[0-9][0-9]?$";
    final Pattern bytesTypePattern = Pattern.compile(bytesTypeRegex);

    // This regex tries to extract the dimensions from the
    // square brackets of an array declaration using the ``Regex Groups``.
    // Eg- It extracts ``5, 6, 7`` from ``[5][6][7]``
    final String arrayDimensionRegex = "\\[([1-9]\\d*)?\\]";
    final Pattern arrayDimensionPattern = Pattern.compile(arrayDimensionRegex);

    // Fields of Entry Objects need to follow a regex pattern
    // Type Regex matches to a valid name or an array declaration.
    final String typeRegex = "^[a-zA-Z_$][a-zA-Z_$0-9]*(\\[([1-9]\\d*)*\\])*$";
    final Pattern typePattern = Pattern.compile(typeRegex);
    // Identifier Regex matches to a valid name, but can't be an array declaration.
    final String identifierRegex = "^[a-zA-Z_$][a-zA-Z_$0-9]*$";
    final Pattern identifierPattern = Pattern.compile(identifierRegex);

    public StructuredDataEncoder(String jsonMessageInString) throws IOException, RuntimeException {
        // Parse String Message into object and validate
        this.jsonMessageObject = parseJSONMessage(jsonMessageInString);
    }

    public Set<String> getDependencies(String primaryType) {
        // Find all the dependencies of a type
        HashMap<String, List<StructuredData.Entry>> types = jsonMessageObject.getTypes();
        Set<String> deps = new HashSet<>();

        if (!types.containsKey(primaryType)) {
            return deps;
        }

        List<String> remainingTypes = new ArrayList<>();
        remainingTypes.add(primaryType);

        while (remainingTypes.size() > 0) {
            String structName = remainingTypes.get(remainingTypes.size() - 1);
            remainingTypes.remove(remainingTypes.size() - 1);
            deps.add(structName);

            for (StructuredData.Entry entry : types.get(primaryType)) {
                if (!types.containsKey(entry.getType())) {
                    // Don't expand on non-user defined types
                } else if (deps.contains(entry.getType())) {
                    // Skip types which are already expanded
                } else {
                    // Encountered a user defined type
                    remainingTypes.add(entry.getType());
                }
            }
        }

        return deps;
    }

    public String encodeStruct(String structName) {
        HashMap<String, List<StructuredData.Entry>> types = jsonMessageObject.getTypes();

        StringBuilder structRepresentation = new StringBuilder(structName + "(");
        for (StructuredData.Entry entry : types.get(structName)) {
            structRepresentation.append(String.format("%s %s,", entry.getType(), entry.getName()));
        }

        //if (structRepresentation.lastIndexOf("(") != (structRepresentation.length()-1))
        if (types.get(structName).size() > 0)
        {
            structRepresentation =
                    new StringBuilder(
                            structRepresentation.substring(0, structRepresentation.length() - 1));
        }
        structRepresentation.append(")");

        return structRepresentation.toString();
    }

    public String encodeType(String primaryType) {
        Set<String> deps = getDependencies(primaryType);
        deps.remove(primaryType);

        // Sort the other dependencies based on Alphabetical Order and finally add the primaryType
        List<String> depsAsList = new ArrayList<>(deps);
        Collections.sort(depsAsList);
        depsAsList.add(0, primaryType);

        StringBuilder result = new StringBuilder();
        for (String structName : depsAsList) {
            result.append(encodeStruct(structName));
        }

        return result.toString();
    }

    public byte[] typeHash(String primaryType) {
        return Numeric.hexStringToByteArray(sha3String(encodeType(primaryType)));
    }

    public List<Integer> getArrayDimensionsFromDeclaration(String declaration) {
        // Get the dimensions which were declared in Schema.
        // If any dimension is empty, then it's value is set to -1.
        Matcher arrayTypeMatcher = arrayTypePattern.matcher(declaration);
        arrayTypeMatcher.find();
        String dimensionsString = arrayTypeMatcher.group(1);
        Matcher dimensionTypeMatcher = arrayDimensionPattern.matcher(dimensionsString);
        List<Integer> dimensions = new ArrayList<>();
        while (dimensionTypeMatcher.find()) {
            String currentDimension = dimensionTypeMatcher.group(1);
            if (currentDimension == null) {
                dimensions.add(Integer.parseInt("-1"));
            } else {
                dimensions.add(Integer.parseInt(currentDimension));
            }
        }

        if (dimensions.size() == 0) {
            dimensions.add(Integer.parseInt("-1"));
        }

        return dimensions;
    }

    @SuppressWarnings("unchecked")
    public List<Pair> getDepthsAndDimensions(Object data, int depth) {
        if (!(data instanceof List)) {
            // Nothing more to recurse, since the data is no more an array
            return new ArrayList<>();
        }

        List<Pair> list = new ArrayList<>();
        List<Object> dataAsArray = (List<Object>) data;
        list.add(new Pair(depth, dataAsArray.size()));
        for (Object subdimensionalData : dataAsArray) {
            list.addAll(getDepthsAndDimensions(subdimensionalData, depth + 1));
        }

        return list;
    }

    public List<Integer> getArrayDimensionsFromData(Object data) throws RuntimeException {
        List<Pair> depthsAndDimensions = getDepthsAndDimensions(data, 0);
        // groupedByDepth has key as depth and value as List(pair(Depth, Dimension))
        Map<Object, List<Pair>> groupedByDepth =
                depthsAndDimensions.stream().collect(Collectors.groupingBy(Pair::getFirst));

        // depthDimensionsMap is aimed to have key as depth and value as List(Dimension)
        Map<Integer, List<Integer>> depthDimensionsMap = new HashMap<>();
        for (Map.Entry<Object, List<Pair>> entry : groupedByDepth.entrySet()) {
            List<Integer> pureDimensions = new ArrayList<>();
            for (Pair depthDimensionPair : entry.getValue()) {
                pureDimensions.add((Integer) depthDimensionPair.getSecond());
            }
            depthDimensionsMap.put((Integer) entry.getKey(), pureDimensions);
        }

        List<Integer> dimensions = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : depthDimensionsMap.entrySet()) {
            Set<Integer> setOfDimensionsInParticularDepth = new TreeSet<>(entry.getValue());
            if (setOfDimensionsInParticularDepth.size() != 1) {
                throw new RuntimeException(
                        String.format(
                                "Depth %d of array data has more than one dimensions",
                                entry.getKey()));
            }
            dimensions.add(setOfDimensionsInParticularDepth.stream().findFirst().get());
        }

        return dimensions;
    }

    public List<Object> flattenMultidimensionalArray(Object data) {
        if (!(data instanceof List)) {
            return new ArrayList<Object>() {
                {
                    add(data);
                }
            };
        }

        List<Object> flattenedArray = new ArrayList<>();
        for (Object arrayItem : (List) data) {
            flattenedArray.addAll(flattenMultidimensionalArray(arrayItem));
        }

        return flattenedArray;
    }

    private byte[] convertToEncodedItem(String baseType, Object data) {
        byte[] hashBytes;
        try {
            if (baseType.toLowerCase().startsWith("uint") || baseType.toLowerCase().startsWith("int")) {
                hashBytes = convertToBigInt(data).toByteArray();
            }
            else if (baseType.equals("string")) {
                hashBytes = ((String) data).getBytes();
            }
            else if (baseType.equals("bytes")) {
                hashBytes = Numeric.hexStringToByteArray((String) data);
            }
            else {
                byte[] b = convertArgToBytes((String) data);
                BigInteger bi = new BigInteger(1, b);
                hashBytes = Numeric.toBytesPadded(bi, 32);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            hashBytes = new byte[0];
        }

        return hashBytes;
    }

    private List<Object> getArrayItems(StructuredData.Entry field, Object value)
    {
        List<Integer> expectedDimensions =
                getArrayDimensionsFromDeclaration(field.getType());
        // This function will itself give out errors in case
        // that the data is not a proper array
        List<Integer> dataDimensions = getArrayDimensionsFromData(value);

        final String format =
                String.format(
                        "Array Data %s has dimensions %s, "
                                + "but expected dimensions are %s",
                        value.toString(),
                        dataDimensions.toString(),
                        expectedDimensions.toString());
        if (expectedDimensions.size() != dataDimensions.size()) {
            // Ex: Expected a 3d array, but got only a 2d array
            throw new RuntimeException(format);
        }
        for (int i = 0; i < expectedDimensions.size(); i++) {
            if (expectedDimensions.get(i) == -1) {
                // Skip empty or dynamically declared dimensions
                continue;
            }
            if (!expectedDimensions.get(i).equals(dataDimensions.get(i))) {
                throw new RuntimeException(format);
            }
        }

        return flattenMultidimensionalArray(value);
    }

    @SuppressWarnings("unchecked")
    public byte[] encodeData(String primaryType, HashMap<String, Object> data)
            throws RuntimeException {
        HashMap<String, List<StructuredData.Entry>> types = jsonMessageObject.getTypes();

        List<String> encTypes = new ArrayList<>();
        List<Object> encValues = new ArrayList<>();

        // Add typehash
        encTypes.add("bytes32");
        encValues.add(typeHash(primaryType));

        // Add field contents
        for (StructuredData.Entry field : types.get(primaryType)) {
            Object value = data.get(field.getName());

            if (value == null) continue;

            if (field.getType().equals("string")) {
                encTypes.add("bytes32");
                byte[] hashedValue = Numeric.hexStringToByteArray(sha3String((String) value));
                encValues.add(hashedValue);
            } else if (field.getType().equals("bytes")) {
                encTypes.add(("bytes32"));
                encValues.add(sha3(Numeric.hexStringToByteArray((String) value)));
            } else if (types.containsKey(field.getType())) {
                // User Defined Type
                byte[] hashedValue =
                        sha3(encodeData(field.getType(), (HashMap<String, Object>) value));
                encTypes.add("bytes32");
                encValues.add(hashedValue);
            } else if (bytesTypePattern.matcher(field.getType()).find()) {
                encTypes.add(field.getType());
                encValues.add(Numeric.hexStringToByteArray((String) value));
            } else if (arrayTypePattern.matcher(field.getType()).find()) {
                String baseTypeName = field.getType().substring(0, field.getType().indexOf('['));
                List<Object> arrayItems = getArrayItems(field, value);
                ByteArrayOutputStream concatenatedArrayEncodingBuffer = new ByteArrayOutputStream();

                for (Object arrayItem : arrayItems) {
                    byte[] arrayItemEncoding;
                    if (types.containsKey(baseTypeName)) {
                        arrayItemEncoding = sha3(encodeData(baseTypeName, (HashMap<String, Object>) arrayItem)); //need to hash each user type before adding
                    }
                    else {
                        arrayItemEncoding = convertToEncodedItem(baseTypeName, arrayItem); //add raw item, packed to 32 bytes
                    }

                    concatenatedArrayEncodingBuffer.write(
                            arrayItemEncoding, 0, arrayItemEncoding.length);
                }

                byte[] concatenatedArrayEncodings = concatenatedArrayEncodingBuffer.toByteArray();
                byte[] hashedValue = sha3(concatenatedArrayEncodings);
                encTypes.add("bytes32");
                encValues.add(hashedValue);
            } else if (field.getType().startsWith("uint")
                    || field.getType().startsWith("int")) {
                encTypes.add(field.getType());
                // convert to BigInteger for ABI constructor compatibility
                try {
                    encValues.add(convertToBigInt(value));
                } catch (NumberFormatException | NullPointerException e) {
                    encValues.add(value); // value null or failed to convert, fallback to add string as before
                }
            } else {
                encTypes.add(field.getType());
                encValues.add(value);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < encTypes.size(); i++) {
            Class<Type> typeClazz = (Class<Type>) AbiTypes.getType(encTypes.get(i));

            boolean atleastOneConstructorExistsForGivenParametersType = false;
            // Using the Reflection API to get the types of the parameters
            Constructor[] constructors = typeClazz.getConstructors();
            for (Constructor constructor : constructors) {
                // Check which constructor matches
                try {
                    Class[] parameterTypes = constructor.getParameterTypes();
                    byte[] temp =
                            Numeric.hexStringToByteArray(
                                    TypeEncoder.encode(
                                            typeClazz
                                                    .getDeclaredConstructor(parameterTypes)
                                                    .newInstance(encValues.get(i))));
                    baos.write(temp, 0, temp.length);
                    atleastOneConstructorExistsForGivenParametersType = true;
                    break;
                } catch (IllegalArgumentException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ignored) {
                }
            }

            if (!atleastOneConstructorExistsForGivenParametersType) {
                throw new RuntimeException(
                        String.format(
                                "Received an invalid argument for which no constructor"
                                        + " exists for the ABI Class %s",
                                typeClazz.getSimpleName()));
            }
        }

        return baos.toByteArray();
    }

    private BigInteger convertToBigInt(Object value) throws NumberFormatException, NullPointerException {
        if (value.toString().startsWith("0x")) {
            return Numeric.toBigInt(value.toString());
        } else {
            return new BigInteger(value.toString());
        }
    }

    public byte[] hashMessage(String primaryType, HashMap<String, Object> data)
            throws RuntimeException {
        return sha3(encodeData(primaryType, data));
    }

    @SuppressWarnings("unchecked")
    public byte[] hashDomain() throws RuntimeException {
        ObjectMapper oMapper = new ObjectMapper();
        HashMap<String, Object> data =
                oMapper.convertValue(jsonMessageObject.getDomain(), HashMap.class);

        if (data.get("chainId") != null) {
            data.put("chainId", ((HashMap<String, Object>) data.get("chainId")).get("value"));
        } else {
            data.remove("chainId");
        }

        if (data.get("verifyingContract") != null)
        {
            data.put(
                    "verifyingContract",
                    ((HashMap<String, Object>) data.get("verifyingContract")).get("value"));
        }
        return sha3(encodeData("EIP712Domain", data));
    }

    public void validateStructuredData(StructuredData.EIP712Message jsonMessageObject)
            throws RuntimeException {
        for (String structName : jsonMessageObject.getTypes().keySet()) {
            List<StructuredData.Entry> fields = jsonMessageObject.getTypes().get(structName);
            for (StructuredData.Entry entry : fields) {
                if (!identifierPattern.matcher(entry.getName()).find()) {
                    // raise Error
                    throw new RuntimeException(
                            String.format(
                                    "Invalid Identifier %s in %s", entry.getName(), structName));
                }
                if (!typePattern.matcher(entry.getType()).find()) {
                    // raise Error
                    throw new RuntimeException(
                            String.format("Invalid Type %s in %s", entry.getType(), structName));
                }
            }
        }
    }

    public StructuredData.EIP712Message parseJSONMessage(String jsonMessageInString)
            throws IOException, RuntimeException {
        ObjectMapper mapper = new ObjectMapper();

        // convert JSON string to EIP712Message object
        StructuredData.EIP712Message tempJSONMessageObject =
                mapper.readValue(jsonMessageInString, StructuredData.EIP712Message.class);
        validateStructuredData(tempJSONMessageObject);

        return tempJSONMessageObject;
    }

    @SuppressWarnings("unchecked")
    public byte[] getStructuredData() throws RuntimeException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final String messagePrefix = "\u0019\u0001";
        byte[] prefix = messagePrefix.getBytes();
        baos.write(prefix, 0, prefix.length);

        byte[] domainHash = hashDomain();
        baos.write(domainHash, 0, domainHash.length);

        byte[] dataHash =
                hashMessage(
                        jsonMessageObject.getPrimaryType(),
                        (HashMap<String, Object>) jsonMessageObject.getMessage());
        baos.write(dataHash, 0, dataHash.length);

        return baos.toByteArray();
    }
}
