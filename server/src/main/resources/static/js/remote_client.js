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

remote.handlePauseButton = function () {
    remote.sendCommand("OP_PAUSE");
};

remote.handleFavoriteButton = function () {
    remote.sendCommand("OP_FAVORITE");
};

remote.main = function () {
    var next_button = document.getElementById("forward-button");
    var previous_button = document.getElementById("back-button");
    var pause_button = document.getElementById("pause-button");
    var favorite_button = document.getElementById("favorite-button");
    next_button.addEventListener("click", remote.handleNextButton);
    previous_button.addEventListener("click", remote.handlePreviousButton);
    pause_button.addEventListener("click", remote.handlePauseButton);
    favorite_button.addEventListener("click", remote.handleFavoriteButton);
};

remote.main();