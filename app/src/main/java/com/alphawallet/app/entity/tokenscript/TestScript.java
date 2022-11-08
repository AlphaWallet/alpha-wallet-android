package com.alphawallet.app.entity.tokenscript;

/**
 * Created by JB on 6/11/2022.
 */
public abstract class TestScript
{
    public static String testScriptXXLF = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> <ts:token xmlns:ts=\"http://tokenscript.org/2020/06/tokenscript\"" +
            " xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xsi:schemaLocation=\"http://tokenscript.org/2020/06/tokenscript" +
            " http://tokenscript.org/2020/06/tokenscript.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:asnx=\"urn:ietf:params:xml:ns:asnx\"" +
            " xmlns:ethereum=\"urn:ethereum:constantinople\" custodian=\"false\"> <ts:label> <ts:plurals xml:lang=\"en\"> <ts:string quantity=\"one\">STL Office Token</ts:string>" +
            " <ts:string quantity=\"other\">STL Office Tokens</ts:string> </ts:plurals> <ts:plurals xml:lang=\"es\"> <ts:string quantity=\"one\">Boleto de admisión</ts:string>" +
            " <ts:string quantity=\"other\">Boleto de admisiónes</ts:string> </ts:plurals> <ts:plurals xml:lang=\"zh\"> <ts:string quantity=\"one\">入場券</ts:string>" +
            " <ts:string quantity=\"other\">入場券</ts:string> </ts:plurals> </ts:label> <ts:contract interface=\"erc721\" name=\"MessageToken\">" +
            " <ts:address network=\"2\">0xC3eeCa3Feb9Dbc06c6e749702AcB8d56A07BFb05</ts:address> </ts:contract> <ts:origins> <ts:ethereum contract=\"MessageToken\"> </ts:ethereum>" +
            " </ts:origins> <ts:cards> <ts:card type=\"action\"> <ts:label> <ts:string xml:lang=\"en\">Unlock</ts:string> <ts:string xml:lang=\"zh\">开锁</ts:string>" +
            " <ts:string xml:lang=\"es\">Abrir</ts:string> </ts:label> <ts:view xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">" +
            " <style type=\"text/css\"> html { } body { padding: 0px; margin: 0px; } div { margin: 0px; padding: 0px; } </style> <script type=\"text/javascript\">" +
            " class Token { constructor(tokenInstance) { this.props = tokenInstance } render() { return` &lt;h3&gt;Sign the challenge to unlock ...&lt;/h3&gt; &lt;div" +
            " id=\"msg\"&gt;Preparing to unlock the entrance door.&lt;/div&gt; &lt;div id=\"inputBox\"&gt; &lt;h3&gt;Door open time&lt;/h3&gt; &lt;input" +
            " id=\"openTime\" type=\"number\" value='120' /&gt; &lt;/div&gt; &lt;div id=\"contractAddress\"&gt;${this.props.contractAddress}&lt;/div&gt; &lt;div id=\"status\"/&gt;`; } }" +
            " web3.tokens.dataChanged = (oldTokens, updatedTokens, cardId) => { const currentTokenInstance = updatedTokens.currentInstance; document.getElementById(cardId).innerHTML =" +
            " new Token(currentTokenInstance).render(); }; function handleErrors(response) { if (!response.ok) { throw Error(response.statusText); } return response.text(); }" +
            " var iotAddr = \"0x2A53DEF90EBA4248F1E14948EBF1F01BD7C44656\".toLowerCase(); var serverAddr = \"http://scriptproxy.smarttokenlabs.com\";" +
            " document.addEventListener(\"DOMContentLoaded\", function() { window.onload = function startup() { james.lug.org.cn fetch(`${serverAddr}:8080/api/${iotAddr}/getChallenge`)" +
            " .then(handleErrors) .then(function (response) { document.getElementById('msg').innerHTML = 'Challenge: ' + response window.challenge = response }) }" +
            " window.onConfirm = function onConfirm(signature) { if (window.challenge === undefined || window.challenge.length == 0) return const challenge = window.challenge" +
            " document.getElementById('status').innerHTML = 'Wait for signature...' // 2. sign challenge to generate response web3.personal.sign({ data: challenge }, function (error, value)" +
            " { if (error != null) { document.getElementById('status').innerHTML = error } else { document.getElementById('status').innerHTML = 'Verifying credentials ...'" +
            " let contractAddress = document.getElementById(\"contractAddress\").textContent; let unlockTime = document.getElementById(\"openTime\").value; " +
            " fetch(`${serverAddr}:8080/api/${iotAddr}/checkSignature?openTime=${unlockTime}&amp;sig=${value}`) .then(function (response) { if (!response.ok)" +
            " { document.getElementById('status').innerHTML = response.statusText; throw Error(response.statusText); } else { return response.text() } }) .then(function (response)" +
            " { if (response == \"pass\") { document.getElementById('status').innerHTML = 'Entrance granted!' window.close() } else { document.getElementById('status').innerHTML = 'Failed with: '" +
            " + response } }).catch(function() { console.log(\"error blah\"); }); } }); window.challenge = ''; document.getElementById('msg').innerHTML = ''; } }) </script> </ts:view> </ts:card>" +
            " <ts:card type=\"action\"> <ts:label> <ts:string xml:lang=\"en\">Lock</ts:string> <ts:string xml:lang=\"zh\">关锁</ts:string>" +
            " <ts:string xml:lang=\"es\">Cerrar</ts:string> </ts:label> <ts:view xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\"> <style type=\"text/css\"> html { } body" +
            " { padding: 0px; margin: 0px; } div { margin: 0px; padding: 0px; } .data-icon { height:16px; vertical-align: middle } </style> <script type=\"text/javascript\"> class Token" +
            " { constructor(tokenInstance) { this.props = tokenInstance } render() { return` &lt;h3&gt;Sign the challenge to lock ...&lt;/h3&gt;" +
            " &lt;div id=\"msg\"&gt;Preparing to lock the door.&lt;/div&gt; &lt;div id=\"contractAddress\"&gt;${this.props.contractAddress}&lt;/div&gt; &lt;div id=\"status\"/&gt;`; } }" +
            " web3.tokens.dataChanged = (oldTokens, updatedTokens, cardId) => { const currentTokenInstance = updatedTokens.currentInstance; document.getElementById(cardId).innerHTML" +
            " = new Token(currentTokenInstance).render(); }; function handleErrors(response) { if (!response.ok) { throw Error(response.statusText); } return response.text(); }" +
            " var iotAddr = \"0x2A53DEF90EBA4248F1E14948EBF1F01BD7C44656\".toLowerCase(); var serverAddr = \"http://scriptproxy.smarttokenlabs.com\";" +
            " document.addEventListener(\"DOMContentLoaded\", function() { window.onload = function startup() { fetch(`${serverAddr}:8080/api/${iotAddr}/getChallenge`) .then(handleErrors)" +
            " .then(function (response) { document.getElementById('msg').innerHTML = 'Challenge: ' + response window.challenge = response }) } window.onConfirm = function onConfirm(signature)" +
            " { if (window.challenge === undefined || window.challenge.length == 0) return const challenge = window.challenge document.getElementById('status').innerHTML = 'Wait for signature...'" +
            " web3.personal.sign({ data: challenge }, function (error, value) { if (error != null) { document.getElementById('status').innerHTML = error } else {" +
            " document.getElementById('status').innerHTML = 'Verifying credentials ...' let contractAddress = document.getElementById(\"contractAddress\").textContent;" +
            " fetch(`${serverAddr}:8080/api/${iotAddr}/checkSignatureLock?openTime=0&amp;sig=${value}`) .then(function (response) { if (!response.ok) { document.getElementById('status').innerHTML" +
            " = response.statusText; throw Error(response.statusText); } else { return response.text() } }) .then(function (response) { if (response == \"pass\") " +
            "{ document.getElementById('status').innerHTML = 'Door locked' window.close() } else { document.getElementById('status').innerHTML = 'Failed with: ' + response } }).catch(function()" +
            " { console.log(\"error blah\"); }); } }); window.challenge = ''; document.getElementById('msg').innerHTML = ''; } }) </script> </ts:view> </ts:card> <ts:card type=\"action\">" +
            " <ts:label> <ts:string xml:lang=\"en\">Mint Ape 1</ts:string> </ts:label> <ts:view xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\"> <style type=\"text/css\">" +
            " html { } body { padding: 0px; margin: 0px; } div { margin: 0px; padding: 0px; } </style> <script type=\"text/javascript\"> class Token { constructor(tokenInstance)" +
            " { this.props = tokenInstance } render() { return` &lt;h3&gt;Mint Ape with Ticket ...&lt;/h3&gt;`; } } web3.tokens.dataChanged = (oldTokens, updatedTokens, cardId) =>" +
            " { const currentTokenInstance = updatedTokens.currentInstance; document.getElementById(cardId).innerHTML = new Token(currentTokenInstance).render(); }; </script> </ts:view>" +
            " <ts:transaction> <ethereum:transaction contract=\"MessageToken\" function=\"mintUsingSequentialTokenId\"> <ts:data> </ts:data> </ethereum:transaction> </ts:transaction>" +
            " </ts:card> <ts:card type=\"action\"> <!-- this action is of the model confirm-back. window.onConfirm is called if user hit \"confirm\"; window.close()" +
            " causes the back button to be pressed. --> <ts:label> <ts:string xml:lang=\"en\">Mint Ape 2</ts:string> </ts:label> <ts:view xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">" +
            " <style type=\"text/css\"> html { } body { padding: 0px; margin: 0px; } div { margin: 0px; padding: 0px; } </style> <script type=\"text/javascript\">" +
            " class Token { constructor(tokenInstance) { this.props = tokenInstance } render() { return` &lt;h3&gt;Mint Ape with Ticket ...&lt;/h3&gt;`; } } web3.tokens.dataChanged =" +
            " (oldTokens, updatedTokens, cardId) => { const currentTokenInstance = updatedTokens.currentInstance; document.getElementById(cardId).innerHTML =" +
            " new Token(currentTokenInstance).render(); }; </script> </ts:view> <ts:transaction> <ethereum:transaction contract=\"MessageToken\" function=\"topMintUsingSequentialTokenId\">" +
            " <ts:data> </ts:data> </ethereum:transaction> </ts:transaction> </ts:card> <ts:card type=\"action\"> <ts:label> <ts:string xml:lang=\"en\">Mint STL Token</ts:string>" +
            " </ts:label> <ts:view xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\"> <style type=\"text/css\"> html { } body { padding: 0px; margin: 0px; } div { margin: 0px; padding: 0px; }" +
            " </style> <script type=\"text/javascript\"> class Token { constructor(tokenInstance) { this.props = tokenInstance } render()" +
            " { return` &lt;h3&gt;Mint STL Logo token ...&lt;/h3&gt;`; } } web3.tokens.dataChanged = (oldTokens, updatedTokens, cardId) => { const currentTokenInstance" +
            " = updatedTokens.currentInstance; document.getElementById(cardId).innerHTML = new Token(currentTokenInstance).render(); }; </script> </ts:view> <ts:transaction>" +
            " <ethereum:transaction contract=\"MessageToken\" function=\"stlMintUsingSequentialTokenId\"> <ts:data> </ts:data> </ethereum:transaction> </ts:transaction> </ts:card>" +
            " </ts:cards> </ts:token>";
}
