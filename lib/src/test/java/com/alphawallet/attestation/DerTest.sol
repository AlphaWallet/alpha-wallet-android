/* Attestation decode and validation */
/* AlphaWallet 2020 */

pragma solidity ^0.5.0;
import "https://github.com/witnet/elliptic-curve-solidity/blob/master/contracts/EllipticCurve.sol";

contract DerDecode {
    address payable owner;

    bytes1 constant BOOLEAN_TAG         = bytes1(0x01);
    bytes1 constant INTEGER_TAG         = bytes1(0x02);
    bytes1 constant BIT_STRING_TAG      = bytes1(0x03);
    bytes1 constant OCTET_STRING_TAG    = bytes1(0x04);
    bytes1 constant NULL_TAG            = bytes1(0x05);
    bytes1 constant OBJECT_IDENTIFIER_TAG = bytes1(0x06);
    bytes1 constant EXTERNAL_TAG        = bytes1(0x08);
    bytes1 constant ENUMERATED_TAG      = bytes1(0x0a); // decimal 10
    bytes1 constant SEQUENCE_TAG        = bytes1(0x10); // decimal 16
    bytes1 constant SET_TAG             = bytes1(0x11); // decimal 17
    bytes1 constant SET_OF_TAG          = bytes1(0x11);

    bytes1 constant NUMERIC_STRING_TAG  = bytes1(0x12); // decimal 18
    bytes1 constant PRINTABLE_STRING_TAG = bytes1(0x13); // decimal 19
    bytes1 constant T61_STRING_TAG      = bytes1(0x14); // decimal 20
    bytes1 constant VIDEOTEX_STRING_TAG = bytes1(0x15); // decimal 21
    bytes1 constant IA5_STRING_TAG      = bytes1(0x16); // decimal 22
    bytes1 constant UTC_TIME_TAG        = bytes1(0x17); // decimal 23
    bytes1 constant GENERALIZED_TIME_TAG = bytes1(0x18); // decimal 24
    bytes1 constant GRAPHIC_STRING_TAG  = bytes1(0x19); // decimal 25
    bytes1 constant VISIBLE_STRING_TAG  = bytes1(0x1a); // decimal 26
    bytes1 constant GENERAL_STRING_TAG  = bytes1(0x1b); // decimal 27
    bytes1 constant UNIVERSAL_STRING_TAG = bytes1(0x1c); // decimal 28
    bytes1 constant BMP_STRING_TAG      = bytes1(0x1e); // decimal 30
    bytes1 constant UTF8_STRING_TAG     = bytes1(0x0c); // decimal 12

    bytes1 constant CONSTRUCTED_TAG     = bytes1(0x20); // decimal 28

    uint256 constant IA5_CODE = uint256(bytes32("IA5")); //tags for disambiguating content
    uint256 constant DEROBJ_CODE = uint256(bytes32("OBJID"));

    uint256 constant AA = 0;
    uint256 constant BB = 7;
    uint256 constant PP = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F;
    uint256 constant CURVE_ORDER = 0xfffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141;

    event Value(uint256 indexed val);
    event RtnStr(bytes val);
    event RtnS(string val);

    constructor() public
    {
        owner = msg.sender;
    }

    struct Length {
        uint decodeIndex;
        uint length;
    }

    struct Exponent {
        bytes base;
        bytes riddle;
        bytes tPoint;
        uint256 challenge;
    }

    struct OctetString {
        uint decodeIndex;
        bytes byteCode;
    }

    function decodeAttestation(bytes memory attestation) public view returns(bool)
    {
        Length memory len;
        Exponent memory pok;
        OctetString memory octet;

        require(attestation[0] == (CONSTRUCTED_TAG | SEQUENCE_TAG));

        len = decodeLength(attestation, 1);
        octet.decodeIndex = len.decodeIndex;

        //decode parts
        octet = decodeOctetString(attestation, octet.decodeIndex);
        pok.base = octet.byteCode;
        octet = decodeOctetString(attestation, octet.decodeIndex);
        pok.riddle = octet.byteCode;
        octet = decodeOctetString(attestation, octet.decodeIndex);
        pok.challenge = bytesToUint(octet.byteCode);
        octet = decodeOctetString(attestation, octet.decodeIndex);
        pok.tPoint = octet.byteCode;

        //ideally we should expand the concat to take multiple args
        bytes memory cArray = concat(pok.base, pok.riddle);
        cArray = concat(cArray, pok.tPoint);

        (uint256 x, uint256 y) = extractXYFromPoint(pok.base);

        // Multiply base with challenge
        (uint256 lhsX, uint256 lhsY) = EllipticCurve.ecMul(pok.challenge, x, y, AA, PP);

        (x, y) = extractXYFromPoint(pok.riddle);

        uint256 c = mapToInteger(cArray);
        (uint256 rhsX, uint256 rhsY) = EllipticCurve.ecMul(c, x, y, AA, PP);

        (x, y) = extractXYFromPoint(pok.tPoint);

        (rhsX, rhsY) = EllipticCurve.ecAdd(rhsX, rhsY, x, y, AA, PP);

        if (lhsX == rhsX && lhsY == rhsY)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    function extractXYFromPoint(bytes memory data) internal pure returns (uint256 x, uint256 y)
    {
        require (data[0] == OCTET_STRING_TAG);

        bytes memory s = new bytes(32);
        bytes memory r = new bytes(32);
        for (uint i = 0; i < 32; i++)
        {
            s[i] = data[i+1];
            r[i] = data[i+33];
        }

        x = bytesToUint(s);
        y = bytesToUint(r);
    }

    //TODO: optimise using assembly
    function mapToBytes(string memory id, uint t) private pure returns(bytes memory b)
    {
        uint i;
        bytes memory s = new bytes(32);
        bytes memory idBytes = bytes(id);
        b = new bytes(idBytes.length + 4);
        assembly { mstore(add(s, 32), t) }
        for (i = 0; i < 4; i++)
        {
            b[i] = s[(32 - 4 + i)];
        }

        for (i = 0; i < (idBytes.length); i++)
        {
            b[i+4] = idBytes[i];
        }
    }

    //TODO: Check maths; validate that it is correct to add the field size if the hash is negative
    function mapToInteger(bytes memory input) private pure returns(uint256 idNumU)
    {
        bytes32 idHash = keccak256(input);
        int256 idNum = int256(bytes32ToUint(idHash));
        if (idNum < 0) idNum = idNum + int256(PP);
        idNumU = uint256(idNum) % PP;
        idNumU = idNumU % CURVE_ORDER;
    }

    function decodeOctetString(bytes memory byteCode, uint decodeIndex) private pure returns(OctetString memory data)
    {
        data.decodeIndex = decodeIndex;
        Length memory len;

        require (byteCode[data.decodeIndex++] == OCTET_STRING_TAG);

        len = decodeLength(byteCode, data.decodeIndex);
        data.decodeIndex = len.decodeIndex;

        //parse the octet string
        data.byteCode = new bytes(len.length);

        //TODO: re-code in assembly
        for (uint i = 0; i < len.length; i++)
        {
            data.byteCode[i] = byteCode[data.decodeIndex++];
        }
    }

    function toBytes(uint256 x, uint length) private pure returns (bytes memory b)
    {
        bytes memory s = new bytes(32);
        b = new bytes(length);
        assembly { mstore(add(s, 32), x) }
        for (uint i = 0; i < length; i++)
        {
            b[i] = s[(32 - length + i)];
        }
    }

    function bytesToUint(bytes memory b) private pure returns (uint256 number)
    {
        for(uint i = 0; i < b.length; i++)
        {
            number = number + uint(uint8(b[i]))*(2**(8*(b.length-(i+1))));
        }
    }

    function bytes32ToUint(bytes32 b) private pure returns (uint256 number)
    {
        for(uint i = 0; i < 32; i++)
        {
            number = number + uint(uint8(b[i]))*(2**(8*(b.length-(i+1))));
        }
    }

    function decodeLength(bytes memory byteCode, uint decodeIndex) private pure returns(Length memory)
    {
        uint codeLength = 1;
        Length memory retVal;
        retVal.length = 0;
        retVal.decodeIndex = decodeIndex;

        if ((byteCode[retVal.decodeIndex] & 0x80) == 0x80)
        {
            codeLength = uint8((byteCode[retVal.decodeIndex++] & 0x7f));
        }

        for (uint i = 0; i < codeLength; i++)
        {
            retVal.length |= uint(uint8(byteCode[retVal.decodeIndex++] & 0xFF)) << ((codeLength - i - 1) * 8);
        }

        return retVal;
    }

    function decodeDER(bytes memory byteCode) public view returns(uint256[] memory) //limit for decoded input is 32 bytes for first draft
    {
        uint256[] memory objCodes = new uint256[](40); //arbitrary limit for testing - handle up to 40 translation objects
        Status memory data;
        uint objCodeIndex = 0;
        uint decodeIndex = 0;
        uint length = byteCode.length;

        //need decodeDERLength
        //first get tag of next object
        while (decodeIndex < (length - 2) && byteCode[decodeIndex] != 0)
        {
            //get tag
            bytes1 tag = byteCode[decodeIndex++];
            require((tag & 0x20) == 0); //assert primitive
            require((tag & 0xC0) == 0); //assert universal type

            if ((tag & 0x1f) == IA5_STRING_TAG)
            {
                objCodes[objCodeIndex++] = IA5_CODE;
                data = decodeIA5String(byteCode, objCodes, objCodeIndex, decodeIndex);
                objCodeIndex = data.objCodeIndex;
                decodeIndex = data.decodeIndex;
            }
            else if ((tag & 0x1f) == OBJECT_IDENTIFIER_TAG)
            {
                objCodes[objCodeIndex++] = DEROBJ_CODE;
                data = decodeObjectIdentifier(byteCode, objCodes, objCodeIndex, decodeIndex);
                objCodeIndex = data.objCodeIndex;
                decodeIndex = data.decodeIndex;
            }
        }

        uint256[] memory objCodesComplete = new uint256[](objCodeIndex);
        for (uint i = 0; i < objCodeIndex; i++) objCodesComplete[i] = objCodes[i];

        return objCodesComplete;
    }

    function decodeIA5String(bytes memory byteCode, uint256[] memory objCodes, uint objCodeIndex, uint decodeIndex) private pure returns(Status memory)
    {
        uint length = uint8(byteCode[decodeIndex++]);
        bytes32 store = 0;
        for (uint j = 0; j < length; j++) store |= bytes32(byteCode[decodeIndex++] & 0xFF) >> (j * 8);
        objCodes[objCodeIndex++] = uint256(store);
        Status memory retVal;
        retVal.decodeIndex = decodeIndex;
        retVal.objCodeIndex = objCodeIndex;

        return retVal;
    }

    struct Status {
        uint decodeIndex;
        uint objCodeIndex;
    }

    function decodeObjectIdentifier(bytes memory byteCode, uint256[] memory objCodes, uint objCodeIndex, uint decodeIndex) private pure returns(Status memory)
    {
        uint length = uint8(byteCode[decodeIndex++]);

        Status memory retVal;

        //1. decode leading pair
        uint subIDEndIndex = decodeIndex;
        uint256 subId;

        while ((byteCode[subIDEndIndex] & 0x80) == 0x80)
        {
            require(subIDEndIndex < byteCode.length);
            subIDEndIndex++;
        }

        uint subidentifier = 0;
        for (uint i = decodeIndex; i <= subIDEndIndex; i++)
        {
            subId = uint256(uint8(byteCode[i] & 0x7f)) << ((subIDEndIndex - i) * 7);
            subidentifier |= subId;
        }

        if (subidentifier < 40)
        {
            objCodes[objCodeIndex++] = 0;
            objCodes[objCodeIndex++] = subidentifier;
        }
        else if (subidentifier < 80)
        {
            objCodes[objCodeIndex++] = 1;
            objCodes[objCodeIndex++] = subidentifier - 40;
        }
        else
        {
            objCodes[objCodeIndex++] = 2;
            objCodes[objCodeIndex++] = subidentifier - 80;
        }

        subIDEndIndex++;

        while (subIDEndIndex < (decodeIndex + length) && byteCode[subIDEndIndex] != 0)
        {
            subidentifier = 0;
            uint256 subIDStartIndex = subIDEndIndex;

            while ((byteCode[subIDEndIndex] & 0x80) == 0x80)
            {
                require(subIDEndIndex < byteCode.length);
                subIDEndIndex++;
            }
            subidentifier = 0;
            for (uint256 j = subIDStartIndex; j <= subIDEndIndex; j++)
            {
                subId = uint256(uint8(byteCode[j] & 0x7f)) << ((subIDEndIndex - j) * 7);
                subidentifier |= subId;
            }
            objCodes[objCodeIndex++] = subidentifier;
            subIDEndIndex++;
        }

        decodeIndex += length;

        retVal.decodeIndex = decodeIndex;
        retVal.objCodeIndex = objCodeIndex;

        return retVal;
    }

    function endContract() public payable
    {
        if(msg.sender == owner)
        {
            selfdestruct(owner);
        }
        else revert();
    }

    function concat(
        bytes memory _preBytes,
        bytes memory _postBytes
    )
    internal
    pure
    returns (bytes memory)
    {
        bytes memory tempBytes;

        assembly {
        // Get a location of some free memory and store it in tempBytes as
        // Solidity does for memory variables.
            tempBytes := mload(0x40)

        // Store the length of the first bytes array at the beginning of
        // the memory for tempBytes.
            let length := mload(_preBytes)
            mstore(tempBytes, length)

        // Maintain a memory counter for the current write location in the
        // temp bytes array by adding the 32 bytes for the array length to
        // the starting location.
            let mc := add(tempBytes, 0x20)
        // Stop copying when the memory counter reaches the length of the
        // first bytes array.
            let end := add(mc, length)

            for {
            // Initialize a copy counter to the start of the _preBytes data,
            // 32 bytes into its memory.
                let cc := add(_preBytes, 0x20)
            } lt(mc, end) {
            // Increase both counters by 32 bytes each iteration.
                mc := add(mc, 0x20)
                cc := add(cc, 0x20)
            } {
            // Write the _preBytes data into the tempBytes memory 32 bytes
            // at a time.
                mstore(mc, mload(cc))
            }

        // Add the length of _postBytes to the current length of tempBytes
        // and store it as the new length in the first 32 bytes of the
        // tempBytes memory.
            length := mload(_postBytes)
            mstore(tempBytes, add(length, mload(tempBytes)))

        // Move the memory counter back from a multiple of 0x20 to the
        // actual end of the _preBytes data.
            mc := end
        // Stop copying when the memory counter reaches the new combined
        // length of the arrays.
            end := add(mc, length)

            for {
                let cc := add(_postBytes, 0x20)
            } lt(mc, end) {
                mc := add(mc, 0x20)
                cc := add(cc, 0x20)
            } {
                mstore(mc, mload(cc))
            }

        // Update the free-memory pointer by padding our last write location
        // to 32 bytes: add 31 bytes to the end of tempBytes to move to the
        // next 32 byte block, then round down to the nearest multiple of
        // 32. If the sum of the length of the two arrays is zero then add
        // one before rounding down to leave a blank 32 bytes (the length block with 0).
            mstore(0x40, and(
            add(add(end, iszero(add(length, mload(_preBytes)))), 31),
            not(31) // Round down to the nearest 32 bytes.
            ))
        }

        return tempBytes;
    }
}