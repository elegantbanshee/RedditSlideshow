var remote = {};

remote.sendCommand = function (commmand) {
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            console.log("Command sent");
        }
    }
    request.open("POST", "/api/remote/client");
    request.setRequestHeader("Content-Type", "text/plain");

    var reg = /https?:\/\/[A-Za-z\.:\d]*\/remote\/(.*)/;
    request.send(reg.exec(window.location.href)[1] + ";" + commmand);
};

remote.handleNextButton = function () {
    remote.sendCommand("OP_NEXT");
};

remote.handlePreviousButton = function () {
    remote.sendCommand("OP_PREVIOUS")
};

remote.main = function () {
    var next_button = document.getElementById("forward-button");
    var previous_button = document.getElementById("back-button");
    next_button.addEventListener("click", remote.handleNextButton)
    previous_button.addEventListener("click", remote.handlePreviousButton)
};

remote.main();