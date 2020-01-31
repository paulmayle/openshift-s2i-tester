
let timerId;
let ackCounttimerId;

window.onload = startRefreshTimer;

function updateStartAt() {
    buildBaseForm("send", "application/x-www-form-urlencoded");
    // Port
    document.getElementById("controlForm").appendChild(document.getElementById("sendPort"));
    // Host
    document.getElementById("controlForm").appendChild(document.getElementById("sendHost"));
    // StartAt
    document.getElementById("controlForm").appendChild(document.getElementById("startAt"));

    addSubmitButton("updateView");
   }

function connect() {
    buildBaseForm("send", "application/x-www-form-urlencoded");
    // Port
    document.getElementById("controlForm").appendChild(document.getElementById("sendPort"));
    // Host
    document.getElementById("controlForm").appendChild(document.getElementById("sendHost"));
    addSubmitButton("connect");
}

function disconnect() {
    buildBaseForm("send", "application/x-www-form-urlencoded");
    addSubmitButton("disconnect");
}

function buildBaseForm(action , enctype){
    clearInterval(timerId);  // stop the update timer
    let controlForm = document.createElement("FORM");
    controlForm.setAttribute("id", "controlForm");
    controlForm.setAttribute("method", "post");
    controlForm.setAttribute("style", "display: none");
    controlForm.setAttribute("action", action);
    controlForm.setAttribute("enctype", enctype);
    document.body.appendChild(controlForm);
}

function addSubmitButton(value){

    let btn = document.createElement("button");
    btn.type = "submit";
    btn.value = value;
    btn.name = "submit";
    document.getElementById("controlForm").appendChild(btn);
    document.getElementById("loader").removeAttribute("hidden");

    btn.click();
}

function multiSend() {

    buildBaseForm("send", "application/x-www-form-urlencoded");
    // Port
    document.getElementById("controlForm").appendChild(document.getElementById("sendPort"));
    // Host
    document.getElementById("controlForm").appendChild(document.getElementById("sendHost"));
    addSubmitButton("multisend");

}

function send() {
    buildBaseForm("send", "application/x-www-form-urlencoded");
    // Port
    document.getElementById("controlForm").appendChild(document.getElementById("sendPort"));
    // Host
    document.getElementById("controlForm").appendChild(document.getElementById("sendHost"));
    // HL7 Message to send
    document.getElementById("controlForm").appendChild(document.getElementById("hl7Message"));
    addSubmitButton("send");
}


function uploadFile() {

    let fi = document.getElementById('file');
    if (fi.files.length > 0) {
        let fsize = fi.files.item(0).size;
        if(fsize>200000000){
            alert("file size too large - the limit is 200MB");
            send();
            return;
        }
    }

    // else file is less than 200M so we can upload it
    buildBaseForm("/upload", "multipart/form-data");

    // Port
    document.getElementById("controlForm").appendChild(document.getElementById("sendPort"));
    // Host
    document.getElementById("controlForm").appendChild(document.getElementById("sendHost"));
    // file to upload
    document.getElementById("controlForm").appendChild(document.getElementById("file"));

    addSubmitButton("fileload");
}

/**
 * Refresh Timer to update the connection state.
 */

function startRefreshTimer() {
    if(status !=null && status.toLowerCase() === "sending"){
   //     if(status !=null ){
        ackCounttimerId = setInterval(updateAckCount, 1000);
    }else{
        timerId = setInterval(updateMessage, 1000);
    }
}

function updateMessage() {
    //create url to request fragment
    let url = "/sendUpdate";
    //load fragment and replace content
    $('#test_result').load(url);
}


function updateAckCount() {
    //create url to request fragment
    let url = "/multisendcount";
    $.get(url, function(data, htmlStatus){

        let sendStatus = JSON.parse(data);
        if(sendStatus.ackCount == -1){
          // send is complete
          //  alert("send complete");
            document.getElementById("hl7Response").value =  sendStatus.message ;
            status=null;
            clearInterval(ackCounttimerId);
            startRefreshTimer();
        }else {
            document.getElementById("hl7Response").value = "Received " + sendStatus.ackCount  + " Acks";
        }
    });
}



