pragma solidity ^0.4.17;
contract TicketPro
{
    uint totalTickets;
    mapping(address => bytes32[]) inventory;
    uint16 ticketIndex = 0; //to track mapping in tickets
    address stormbird;   // the address that calls selfdestruct() and takes fees
    address organiser;
    address paymaster;
    uint transferFee;
    uint numOfTransfers = 0;
    string public name;
    string public symbol;
    uint8 public constant decimals = 0; //no decimals as tickets cannot be split

    event Transfer(address indexed _to, uint16[] _indices);
    event TransferFrom(address indexed _from, address indexed _to, uint16[] _indices);
    event Trade(address indexed seller, uint16[] ticketIndices, uint8 v, bytes32 r, bytes32 s);
    event PassTo(uint16[] ticketIndices, uint8 v, bytes32 r, bytes32 s, address indexed recipient);

    modifier organiserOnly()
    {
        if(msg.sender != organiser) revert();
        else _;
    }
    
    modifier payMasterOnly()
    {
        if(msg.sender != paymaster) revert();
        else _;
    }
    
    function() public { revert(); } //should not send any ether directly

    // example: 
    //["0x0a015af6d74042544e43484e01010001", "0x0a015af6d74042544e43484e01010002", "0x0a015af6d74042544e43484e01010003", "0x0a015af6d74042544e43484e01010004", "0x0a015af6d74042544e43484e01010005", "0x0a015af6d74042544e43484e01020001", "0x0a015af6d74042544e43484e01020002", "0x0a015af6d74042544e43484e01020003", "0x0a015af6d74042544e43484e01020004", "0x0a015af6d74042544e43484e01020005", "0x0a015af6d740414c4255534102010001", "0x0a015af6d740414c4255534102010002", "0x0a015af6d740414c4255534102010003", "0x0a015af6d740414c4255534102010004", "0x0a015af6d740414c4255534102010005", "0x0a015af6d740414c4255534102020001", "0x0a015af6d740414c4255534102020002", "0x0a015af6d740414c4255534102020003", "0x0a015af6d740414c4255534102020004", "0x0a015af6d740414c4255534102020005", "0x0a025af97a4041534d47425203010001", "0x0a025af97a4041534d47425203010002", "0x0a025af97a4041534d47425203010003", "0x0a025af97a4041534d47425203010004", "0x0a025af97a4041534d47425203010005", "0x0a025af97a4041534d47425203020001", "0x0a025af97a4041534d47425203020002", "0x0a025af97a4041534d47425203020003", "0x0a025af97a4041534d47425203020004", "0x0a025af97a4041534d47425203020005", "0x0a025af97a4044455542544e04010001", "0x0a025af97a4044455542544e04010002", "0x0a025af97a4044455542544e04010003", "0x0a025af97a4044455542544e04010004", "0x0a025af97a4044455542544e04010005", "0x0a025af97a4044455542544e04020001", "0x0a025af97a4044455542544e04020002", "0x0a025af97a4044455542544e04020003", "0x0a025af97a4044455542544e04020004", "0x0a025af97a4044455542544e04020005", "0x08035afc1d4047425252555305010001", "0x08035afc1d4047425252555305010002", "0x08035afc1d4047425252555305010003", "0x08035afc1d4047425252555305010004", "0x08035afc1d4047425252555305010005", "0x08035afc1d4047425252555305020001", "0x08035afc1d4047425252555305020002", "0x08035afc1d4047425252555305020003", "0x08035afc1d4047425252555305020004", "0x08035afc1d4047425252555305020005", "0x08035afc1d4047524c4e5a4c06010001", "0x08035afc1d4047524c4e5a4c06010002", "0x08035afc1d4047524c4e5a4c06010003", "0x08035afc1d4047524c4e5a4c06010004", "0x08035afc1d4047524c4e5a4c06010005", "0x08035afc1d4047524c4e5a4c06020001", "0x08035afc1d4047524c4e5a4c06020002", "0x08035afc1d4047524c4e5a4c06020003", "0x08035afc1d4047524c4e5a4c06020004", "0x08035afc1d4047524c4e5a4c06020005", "0x01045afec04049534c4e5a4c07010001", "0x01045afec04049534c4e5a4c07010002", "0x01045afec04049534c4e5a4c07010003", "0x01045afec04049534c4e5a4c07010004", "0x01045afec04049534c4e5a4c07010005", "0x01045afec04049534c4e5a4c07020001", "0x01045afec04049534c4e5a4c07020002", "0x01045afec04049534c4e5a4c07020003", "0x01045afec04049534c4e5a4c07020004", "0x01045afec04049534c4e5a4c07020005", "0x01045afec04041555346524108010001", "0x01045afec04041555346524108010002", "0x01045afec04041555346524108010003", "0x01045afec04041555346524108010004", "0x01045afec04041555346524108010005", "0x01045afec04041555346524108020001", "0x01045afec04041555346524108020002", "0x01045afec04041555346524108020003", "0x01045afec04041555346524108020004", "0x01045afec04041555346524108020005"], "English football brawl", "EFB", "0x007bEe82BDd9e866b2bd114780a47f2261C684E3", "0x007bEe82BDd9e866b2bd114780a47f2261C684E3" 
    function TicketPro (
        bytes32[] tickets,
        string evName,
        string eventSymbol,
        address organiserAddr,
        address paymasterAddr) public
    {
        totalTickets = tickets.length;
        //assign some tickets to event organiser
        stormbird = msg.sender;
        organiser = organiserAddr;
        inventory[organiser] = tickets;
        symbol = eventSymbol;
        name = evName;
        paymaster = paymasterAddr;
    }

    function getDecimals() public pure returns(uint)
    {
        return decimals;
    }

    // example: 0, [3, 4], 27, "0x9CAF1C785074F5948310CD1AA44CE2EFDA0AB19C308307610D7BA2C74604AE98", "0x23D8D97AB44A2389043ECB3C1FB29C40EC702282DB6EE1D2B2204F8954E4B451"
    // price is encoded in the server and the msg.value is added to the message digest,
    // if the message digest is thus invalid then either the price or something else in the message is invalid
    function trade(uint256 expiry,
                   uint16[] ticketIndices,
                   uint8 v,
                   bytes32 r,
                   bytes32 s) public payable
    {
        //checks expiry timestamp,
        //if fake timestamp is added then message verification will fail
        require(expiry > block.timestamp || expiry == 0);

        bytes32 message = encodeMessage(msg.value, expiry, ticketIndices);
        address seller = ecrecover(message, v, r, s);

        for(uint i = 0; i < ticketIndices.length; i++)
        { // transfer each individual tickets in the ask order
            uint16 index = ticketIndices[i];
            assert(inventory[seller][index] != bytes32(0)); // 0 means ticket sold.
            inventory[msg.sender].push(inventory[seller][index]);
            // 0 means ticket sold.
            delete inventory[seller][index];
        }
        seller.transfer(msg.value);
        
        emit Trade(seller, ticketIndices, v, r, s);
    }
    
    function loadNewTickets(bytes32[] tickets) public organiserOnly 
    {
        for(uint i = 0; i < tickets.length; i++) 
        {
            inventory[organiser].push(tickets[i]);    
        }
    }

    //anyone can claim this for free, just have to place their address
    function passTo(uint256 expiry,
                    uint16[] ticketIndices,
                    uint8 v,
                    bytes32 r,
                    bytes32 s,
                    address recipient) public payMasterOnly
    {
        require(expiry > block.timestamp || expiry == 0);
        bytes32 message = encodeMessage(0, expiry, ticketIndices);
        address giver = ecrecover(message, v, r, s);
        for(uint i = 0; i < ticketIndices.length; i++)
        {
            uint16 index = ticketIndices[i];
            //needs to use revert as all changes should be reversed
            //if the user doesnt't hold all the tickets 
            assert(inventory[giver][index] != bytes32(0));
            bytes32 ticket = inventory[giver][index];
            inventory[recipient].push(ticket);
            delete inventory[giver][index];
        }
        
        emit PassTo(ticketIndices, v, r, s, recipient);
    }

    //must also sign in the contractAddress
    function encodeMessage(uint value, uint expiry, uint16[] ticketIndices)
        internal view returns (bytes32)
    {
        bytes memory message = new bytes(84 + ticketIndices.length * 2);
        address contractAddress = getContractAddress();
        for (uint i = 0; i < 32; i++)
        {   // convert bytes32 to bytes[32]
            // this adds the price to the message
            message[i] = byte(bytes32(value << (8 * i)));
        }

        for (i = 0; i < 32; i++)
        {
            message[i + 32] = byte(bytes32(expiry << (8 * i)));
        }

        for(i = 0; i < 20; i++)
        {
            message[64 + i] = byte(bytes20(bytes20(contractAddress) << (8 * i)));
        }

        for (i = 0; i < ticketIndices.length; i++)
        {
            // convert int[] to bytes
            message[84 + i * 2 ] = byte(ticketIndices[i] >> 8);
            message[84 + i * 2 + 1] = byte(ticketIndices[i]);
        }

        return keccak256(message);
    }

    function name() public view returns(string)
    {
        return name;
    }

    function symbol() public view returns(string)
    {
        return symbol;
    }

    function getAmountTransferred() public view returns (uint)
    {
        return numOfTransfers;
    }

    function balanceOf(address _owner) public view returns (bytes32[])
    {
        return inventory[_owner];
    }

    function myBalance() public view returns(bytes32[]){
        return inventory[msg.sender];
    }

    function transfer(address _to, uint16[] ticketIndices) public 
    {
        for(uint i = 0; i < ticketIndices.length; i++)
        {
            uint index = uint(ticketIndices[i]);
            assert(inventory[msg.sender][index] != bytes32(0));
            //pushes each element with ordering
            inventory[_to].push(inventory[msg.sender][index]);
            delete inventory[msg.sender][index];
        }
        emit Transfer(_to, ticketIndices);
    }

    function transferFrom(address _from, address _to, uint16[] ticketIndices)
        organiserOnly public
    {
        for(uint i = 0; i < ticketIndices.length; i++)
        {
            uint index = uint(ticketIndices[i]);
            assert(inventory[_from][index] != bytes32(0));
            //pushes each element with ordering
            inventory[_to].push(inventory[msg.sender][index]);
            delete inventory[_from][index];
        }
        
        emit TransferFrom(_from, _to, ticketIndices);
    }

    function endContract() public
    {
        if(msg.sender == stormbird)
        {
            selfdestruct(stormbird);
        }
        else revert();
    }

    function isStormBirdContract() public pure returns (bool) 
    {
        return true; 
    }

    function getContractAddress() public view returns(address)
    {
        return this;
    }

}
