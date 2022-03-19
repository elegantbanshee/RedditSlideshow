var remote = {};
remote.code = "";

remote.moveLeft = function () {
    index -= 2; // simple.js
    if (index < 0)
        index = 0;
    clearIntervalAndPlay();
};

remote.moveRight = function () {
    clearIntervalAndPlay();
};

remote.getRandomString = function () {
    var randomString = ""
    for (var i = 0; i < 6; i++) {
        randomString += parseInt(Math.random() * 9).toString()
    }
    return randomString;
};

remote.pause = function () {
    paused = !paused; // simple.js
};

remote.favorite = function () {
    favorite.handleFavorite();
};

remote.doLogic = function () {
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var json = JSON.parse(this.responseText);
            switch (json.command) {
                case "OP_PREVIOUS":
                    remote.moveLeft();
                    break;
                case "OP_NEXT":
                    remote.moveRight()
                    break;
                case "OP_PAUSE":
                    remote.pause();
                    break;
                case "OP_FAVORITE":
                    remote.favorite();
                default:
                    //if (json.command !== "undefined" && json.command !== null)
                    //    console.log("Unhandled message: " + json.command)
                    break;
            }
        }
    }

    request.open("GET", "/api/remote/server?code=CODE"
        .replace("CODE", remote.code)
    );
    request.setRequestHeader("Content-Type", "text/plain");
    request.send();
};

remote.main = function () {
    var remoteButton = document.getElementById("remote");
    remoteButton.addEventListener("click", function () {
        if (remote.code === "") {
            remote.code = remote.getRandomString();
            setInterval(remote.doLogic, 1000);
        }

        var reg = /(https?:\/\/[A-Za-z\.:\d]*\/?)r?\/?.*/;
        alert(reg.exec(window.location.href)[1] + "remote/" + remote.code);
    });
};

remote.main();